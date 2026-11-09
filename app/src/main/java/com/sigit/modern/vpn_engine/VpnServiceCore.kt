import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class VpnServiceCore : VpnService() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val configJson = intent?.getStringExtra("CONFIG_JSON") ?: ""
            startForeground(1, buildNotification("Menghubungkan..."))
            
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NexusVPN::WakeLock")
            wakeLock?.acquire()

            val builder = Builder()
            builder.setSession("Nexus VPN")
            builder.addAddress("10.8.0.2", 32)
            builder.addRoute("0.0.0.0", 0)
            builder.setMtu(1500)
            builder.addDnsServer("1.1.1.1")
            
            vpnInterface = builder.establish()
            
            if (vpnInterface == null) {
                stopSelf()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
        
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

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        try { vpnInterface?.close() } catch (e: Exception) {}
        super.onDestroy()
    }
}
