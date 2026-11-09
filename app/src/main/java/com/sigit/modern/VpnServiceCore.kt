package com.sigit.modern.vpn_engine

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class VpnServiceCore : VpnService() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var vpnInterface: ParcelFileDescriptor? = null
    private var sshThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val configJson = intent?.getStringExtra("CONFIG_JSON") ?: ""
        
        startForeground(1, buildNotification("Menghubungkan..."))
        
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NexusVPN::WakeLock")
        wakeLock?.acquire()

        sshThread = Thread {
            try {
                sendLogToUI("[SYS] Memulai engine SSH...", "info")
                
                val config = org.json.JSONObject(configJson)
                val host = config.optString("host", "")
                val port = config.optInt("port", 22)
                val user = config.optString("user", "")
                val pass = config.optString("pass", "")
                val payload = config.optString("payload", "")

                val connector = SshConnector(host, port, user, pass, payload)
                connector.setLogCallback { msg, type ->
                    sendLogToUI(msg, type)
                }
                
                val success = connector.connect()
                if (success) {
                    sendLogToUI("[OK] SSH Terhubung! Membuka TUN Interface...", "ok")
                    updateNotification("Terhubung: $host")
                    sendVpnStateToUI("connected", "Connected")
                    
                    vpnInterface = connector.establishTun(VpnServiceCore@ this)
                    
                    if (vpnInterface != null) {
                        sendLogToUI("[SYS] Tunnel VPN Aktif. Semua trafik diamankan.", "ok")
                        connector.startTunnel(vpnInterface!!)
                    } else {
                        sendLogToUI("[ERR] Gagal membuat TUN Interface.", "err")
                        stopSelf()
                    }
                } else {
                    sendLogToUI("[ERR] Gagal konek ke server SSH.", "err")
                    sendVpnStateToUI("disconnected", "")
                    stopSelf()
                }
            } catch (e: Exception) {
                sendLogToUI("[ERR] Exception: ${e.message}", "err")
                sendVpnStateToUI("disconnected", "")
                stopSelf()
            }
        }
        sshThread?.start()

        return START_STICKY
    }

    private fun buildNotification(status: String): Notification {
        val channelId = "vpn_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nexus VPN Service", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Nexus VPN")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(status: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, buildNotification(status))
    }

    private fun sendLogToUI(msg: String, type: String) {
        val intent = Intent("VPN_LOG_EVENT")
        intent.putExtra("msg", msg)
        intent.putExtra("type", type)
        sendBroadcast(intent)
    }

    private fun sendVpnStateToUI(state: String, text: String) {
        val intent = Intent("VPN_STATE_EVENT")
        intent.putExtra("state", state)
        intent.putExtra("text", text)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        sshThread?.interrupt()
        try { vpnInterface?.close() } catch (e: Exception) {}
        super.onDestroy()
    }
}
