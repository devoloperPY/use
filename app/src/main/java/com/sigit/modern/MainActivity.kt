package com.sigit.modern

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private var webViewRef: WebView? = null
    private var pendingConfigJson: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Nexus VPN") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                ) { padding ->
                    AndroidView(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.databaseEnabled = true
                                
                                addJavascriptInterface(WebAppInterface(this@MainActivity), "AndroidInterface")
                                webViewClient = WebViewClient()
                                webViewRef = this
                                
                                val targetFile = findFirstHtmlFile(context)
                                if (targetFile != null) {
                                    loadUrl("file:///android_asset/$targetFile")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                pendingConfigJson?.let { startVpnService(it) }
            } else {
                Toast.makeText(this, "Izin VPN ditolak!", Toast.LENGTH_SHORT).show()
                webViewRef?.evaluateJavascript("window.setVpnState('disconnected', '')", null)
            }
            pendingConfigJson = null
        }
    }

    private fun startVpnService(configJson: String) {
        try {
            val serviceIntent = Intent(this, Class.forName(packageName + ".vpn_engine.VpnServiceCore"))
            serviceIntent.putExtra("CONFIG_JSON", configJson)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memulai VPN: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun findFirstHtmlFile(context: Context): String? {
        try {
            val files = context.assets.list("") ?: return null
            if (files.contains("index.html")) return "index.html"
            return files.firstOrNull { it.endsWith(".html", ignoreCase = true) }
        } catch (e: Exception) { return null }
    }

    inner class WebAppInterface(private val context: Context) {
        private val prefs = context.getSharedPreferences("NexusVPNPrefs", Context.MODE_PRIVATE)

        @JavascriptInterface
        fun toast(msg: String) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun saveConfig(jsonStr: String) {
            try {
                val configObj = JSONObject(jsonStr)
                val id = configObj.getString("id")
                val currentArray = getConfigs()
                var found = false
                for (i in 0 until currentArray.length()) {
                    if (currentArray.getJSONObject(i).getString("id") == id) {
                        currentArray.put(i, configObj)
                        found = true
                        break
                    }
                }
                if (!found) currentArray.put(configObj)
                prefs.edit().putString("configs", currentArray.toString()).apply()
            } catch (e: Exception) {}
        }

        @JavascriptInterface
        fun deleteConfig(id: String) {
            try {
                val currentArray = getConfigs()
                val newArray = JSONArray()
                for (i in 0 until currentArray.length()) {
                    if (currentArray.getJSONObject(i).getString("id") != id) {
                        newArray.put(currentArray.getJSONObject(i))
                    }
                }
                prefs.edit().putString("configs", newArray.toString()).apply()
            } catch (e: Exception) {}
        }

        @JavascriptInterface
        fun getConfigs(): JSONArray {
            try {
                val str = prefs.getString("configs", "[]") ?: "[]"
                return JSONArray(str)
            } catch (e: Exception) { return JSONArray() }
        }

        @JavascriptInterface
        fun startVpn(configId: String) {
            try {
                val configs = getConfigs()
                var targetConfig = ""
                for (i in 0 until configs.length()) {
                    if (configs.getJSONObject(i).getString("id") == configId) {
                        targetConfig = configs.getJSONObject(i).toString()
                        break
                    }
                }
                if (targetConfig.isEmpty()) return

                val vpnIntent = android.net.VpnService.prepare(context)
                if (vpnIntent != null) {
                    pendingConfigJson = targetConfig
                    startActivityForResult(vpnIntent, 100)
                } else {
                    startVpnService(targetConfig)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        @JavascriptInterface
        fun stopVpn() {
            try {
                val serviceIntent = Intent(context, Class.forName(packageName + ".vpn_engine.VpnServiceCore"))
                context.stopService(serviceIntent)
            } catch (e: Exception) {}
        }
    }
}
