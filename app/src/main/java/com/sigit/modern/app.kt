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

@OptIn(ExperimentalMaterial3Api::class)
class App : ComponentActivity() {
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
                                
                                addJavascriptInterface(WebAppInterface(context), "AndroidInterface")
                                webViewClient = WebViewClient()
                                
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

    private fun findFirstHtmlFile(context: Context): String? {
        try {
            val files = context.assets.list("") ?: return null
            if (files.contains("index.html")) return "index.html"
            return files.firstOrNull { it.endsWith(".html", ignoreCase = true) }
        } catch (e: Exception) { return null }
    }
}

class WebAppInterface(private val context: Context) {
    @JavascriptInterface
    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    
    @JavascriptInterface
    fun startVpn() {
        Toast.makeText(context, "Memulai VPN...", Toast.LENGTH_SHORT).show()
    }
}
