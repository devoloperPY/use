package com.sigit.modern.vpn_engine

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.jcraft.jsch.ChannelDirectTCPIP
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class SshConnector(
    private val host: String,
    private val port: Int,
    private val user: String,
    private val pass: String,
    private val payload: String
) {
    private var session: Session? = null
    private var logCallback: ((String, String) -> Unit)? = null
    private val TUN_MTU = 1500
    private val SOCKS_PORT = 1080

    fun setLogCallback(callback: (String, String) -> Unit) {
        this.logCallback = callback
    }

    private fun log(msg: String, type: String = "info") {
        logCallback?.invoke(msg, type)
    }

    fun connect(): Boolean {
        return try {
            log("[SSH] Mencoba konek ke $host:$port...", "info")
            
            val jsch = JSch()
            session = jsch.getSession(user, host, port)
            session?.setPassword(pass)
            
            val config = java.util.Properties()
            config["StrictHostKeyChecking"] = "no"
            session?.setConfig(config)
            session?.connect(15000)

            if (payload.isNotEmpty()) {
                log("[SSH] Mengirim Payload HTTP...", "debug")
                // Payload HTTP injection logic goes here
            }

            log("[SSH] Autentikasi berhasil.", "ok")
            true
        } catch (e: Exception) {
            log("[SSH] Gagal: ${e.message}", "err")
            false
        }
    }

    fun establishTun(service: VpnService): ParcelFileDescriptor? {
        val builder = service.Builder()
        builder.setSession("Nexus VPN")
        builder.addAddress("10.8.0.2", 32)
        builder.addRoute("0.0.0.0", 0)
        builder.setMtu(TUN_MTU)
        
        builder.addDnsServer("1.1.1.1")
        builder.addDnsServer("8.8.8.8")

        return builder.establish()
    }

    fun startTunnel(tunFd: ParcelFileDescriptor) {
        try {
            val tunInput = FileInputStream(tunFd.fileDescriptor)
            val tunOutput = FileOutputStream(tunFd.fileDescriptor)
            val buffer = ByteArray(TUN_MTU)

            while (!Thread.currentThread().isInterrupted) {
                val length = tunInput.read(buffer)
                if (length > 0) {
                    // Di sini logic membaca IP Packet dan meneruskannya ke SSH
                    log("[TUN] Menerima paket data: $length bytes", "debug")
                }
            }
        } catch (e: Exception) {
            log("[TUN] Error: ${e.message}", "err")
        }
    }

    fun disconnect() {
        try {
            session?.disconnect()
        } catch (e: Exception) {}
    }
}
