package com.smart222tech.pasha18tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private data class VideoSite(val name: String, val initials: String, val url: String)

private val videoSites = listOf(
    VideoSite("Pornhub", "PH", "https://www.pornhub.com/"),
    VideoSite("xVideos", "XV", "https://www.xvideos.com/"),
    VideoSite("xHamster", "XH", "https://xhamster.com/"),
    VideoSite("XNXX", "XN", "https://www.xnxx.com/")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Pasha18App() }
    }
}

@Composable
private fun Pasha18App() {
    var ageConfirmed by remember { mutableStateOf(false) }
    var selectedSite by remember { mutableStateOf<VideoSite?>(null) }

    BackHandler(enabled = selectedSite != null) { selectedSite = null }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(0xFF090B10),
            surface = Color(0xFF151923),
            primary = Color(0xFFFF6B35),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF090B10)) {
            when {
                !ageConfirmed -> AgeGate(onConfirm = { ageConfirmed = true })
                selectedSite != null -> SiteBrowser(
                    site = selectedSite!!,
                    onBack = { selectedSite = null }
                )
                else -> HomeScreen(onSiteSelected = { selectedSite = it })
            }
        }
    }
}

@Composable
private fun AgeGate(onConfirm: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PASHA 18+", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(14.dp))
        Text(
            "Этот каталог предназначен только для совершеннолетних пользователей.",
            color = Color(0xFFB9C0CC), fontSize = 18.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConfirm) { Text("Мне исполнилось 18 лет") }
    }
}

@Composable
private fun HomeScreen(onSiteSelected: (VideoSite) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 42.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PASHA", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("18+", color = Color(0xFFFF6B35), fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("VIDEO SOURCES", color = Color(0xFF8993A4), fontSize = 13.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text("Выберите сайт", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(videoSites) { site -> SiteCard(site = site, onClick = { onSiteSelected(site) }) }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Сайты открываются в защищённом встроенном окне. Реклама и ограничения самих сайтов сохраняются.",
            color = Color(0xFF7D8798), fontSize = 12.sp
        )
    }
}

@Composable
private fun SiteCard(site: VideoSite, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1.55f)
            .onFocusChanged { focused = it.isFocused },
        shape = RoundedCornerShape(18.dp),
        border = if (focused) BorderStroke(3.dp, Color(0xFFFF8A50)) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (focused) Color(0xFF30394A) else Color(0xFF191F2B),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(site.initials, fontSize = 31.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF8A50))
            Spacer(Modifier.height(8.dp))
            Text(site.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SiteBrowser(site: VideoSite, onBack: () -> Unit) {
    val context = LocalContext.current
    var pageTitle by remember(site.url) { mutableStateOf(site.name) }
    val webView = remember(site.url) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val scheme = request?.url?.scheme ?: return true
                    return scheme != "https" && scheme != "http"
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) pageTitle = title.take(48)
                }
            }
            loadUrl(site.url)
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.stopLoading(); webView.destroy() }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF090B10))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF151923)).padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("‹  НАЗАД") }
            Spacer(Modifier.width(18.dp))
            Text(pageTitle, color = Color.White, fontSize = 16.sp, maxLines = 1)
        }
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
    }
}
