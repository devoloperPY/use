@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sigit.modern

import android.annotation.SuppressLint
import android.content.Context
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

class MainActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Quantum App") },
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
                                
                                addJavascriptInterface(WebAppInterface(context), "GhostInterface")
                                
                                webViewClient = WebViewClient()
                                
                                val targetFile = findFirstHtmlFile(context)
                                if (targetFile != null) {
                                    loadUrl("file:///android_asset/$targetFile")
                                } else {
                                    loadDataWithBaseURL(null, "<h1 style='color:red;text-align:center;'>Error: Nggak ada file .html di folder assets!</h1>", "text/html", "UTF-8", null)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun findFirstHtmlFile(context: Context): String? {
        try {
            val files = context.assets.list("") ?: return null
            if (files.contains("index.html")) return "index.html"
            return files.firstOrNull { it.endsWith(".html", ignoreCase = true) }
        } catch (e: Exception) {
            return null
        }
    }
}

class WebAppInterface(private val context: Context) {
    @JavascriptInterface
    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
