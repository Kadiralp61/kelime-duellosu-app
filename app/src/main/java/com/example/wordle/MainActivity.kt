package com.example.wordle

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            window.statusBarColor = android.graphics.Color.parseColor("#121212")

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

            MainAppFlow()
        }
    }
}

// Cihazın internet bağlantısını kontrol eden fonksiyon
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun MainAppFlow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var hasInternet by remember { mutableStateOf(true) }

    // Açılış süreci ve internet kontrolü
    LaunchedEffect(key1 = true) {
        hasInternet = isNetworkAvailable(context)
        delay(2000L) // 2 saniye splash ekranı
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        if (hasInternet) {
            WordleWebViewScreen(url = "https://kiral-oyun.kelime-duellosu.workers.dev/")
        } else {
            NoInternetScreen(onRetry = {
                // Tekrar dene butonuna basıldığında durumu güncelle ve yeniden kontrol et
                hasInternet = isNetworkAvailable(context)
            })
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4A0E17)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "👑", fontSize = 80.sp)
            Text(text = "Kelime Düellosu", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "📡", fontSize = 60.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "İnternet Bağlantısı Yok",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kelime Düellosu'nu oynayabilmek için aktif bir internet bağlantısına ihtiyacın var.",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E17))
            ) {
                Text(text = "Tekrar Dene", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WordleWebViewScreen(url: String) {
    var webViewRef: WebView? = null

    BackHandler(enabled = true) {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            webViewRef?.context?.let { ctx ->
                (ctx as? ComponentActivity)?.finish()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                this.setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                this.settings.javaScriptEnabled = true
                this.settings.domStorageEnabled = true

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val currentUrl = request?.url?.toString()
                        if (currentUrl != null && currentUrl.startsWith("mailto:")) {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse(currentUrl)
                                }
                                view?.context?.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        view?.evaluateJavascript(
                            """
                            (function() {
                                var elements = document.querySelectorAll('[class*="lovable"], [id*="lovable"], .lovable-badge');
                                elements.forEach(function(el) {
                                    el.style.setProperty('display', 'none', 'important');
                                });
                                
                                var footers = document.querySelectorAll('footer');
                                footers.forEach(function(f) {
                                    f.style.setProperty('display', 'none', 'important');
                                });

                                var style = document.createElement('style');
                                style.type = 'text/css';
                                style.innerHTML = `
                                    body, html, #root, main, #app, .min-h-screen { 
                                        background-color: #121212 !important; 
                                    }
                                    h1, h2, h3, h4, h5, h6, .text-foreground, [class*="title"] { 
                                        color: #f3f4f6 !important; 
                                    }
                                    p, span, .text-muted-foreground { 
                                        color: #9ca3af !important; 
                                    }
                                `;
                                document.head.appendChild(style);
                            })();
                            """.trimIndent(), null
                        )
                    }
                }

                overScrollMode = WebView.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.databaseEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                loadUrl(url)
                webViewRef = this
            }
        },
        update = {
            webViewRef = it
        }
    )
}