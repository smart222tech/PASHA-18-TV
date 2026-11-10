package com.smart222tech.pasha18tv

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
private val sites = listOf(
    VideoSite("Pornhub", "PH", "https://www.pornhub.com/"),
    VideoSite("xVideos", "XV", "https://www.xvideos.com/"),
    VideoSite("xHamster", "XH", "https://xhamster.com/"),
    VideoSite("XNXX", "XN", "https://www.xnxx.com/")
)
private enum class Page { HOME, SETTINGS, SEARCH }
private enum class AppLanguage(val code: String, val label: String) {
    RU("ru", "Русский"), DE("de", "Deutsch"), EN("en", "English"), UK("uk", "Українська")
}
private enum class PlayerMode { INTERNAL, VLC, MX }
private enum class GridMode(val count: Int, val label: String) {
    THREE(3, "3"), NINE(9, "9"), EIGHTEEN(18, "18"), LIST(1, "List")
}
private data class Preferences(
    val language: AppLanguage = AppLanguage.RU,
    val grid: GridMode = GridMode.NINE,
    val player: PlayerMode = PlayerMode.INTERNAL,
    val accent: Int = 0xFFFF6B35.toInt()
)

private val translations = mapOf(
    "ru" to listOf("Выберите источник", "Обновить", "Поиск", "Настройки", "Источники", "Каталог видео пока не подключён", "Откройте источник, чтобы просматривать его каталог.", "Язык", "Вид карточек", "Плеер", "Цвет темы", "Встроенный браузер", "Внешний VLC", "Внешний MX Player", "Сетка", "Список", "Назад", "Мне исполнилось 18 лет", "Только для совершеннолетних пользователей", "Введите запрос"),
    "de" to listOf("Quelle auswählen", "Aktualisieren", "Suche", "Einstellungen", "Quellen", "Der Videokatalog ist noch nicht verbunden", "Öffne eine Quelle, um ihren Katalog zu durchsuchen.", "Sprache", "Kartenansicht", "Player", "Designfarbe", "Integrierter Browser", "Externer VLC", "Externer MX Player", "Raster", "Liste", "Zurück", "Ich bin mindestens 18 Jahre alt", "Nur für volljährige Nutzer", "Suchbegriff eingeben"),
    "en" to listOf("Choose a source", "Refresh", "Search", "Settings", "Sources", "Video catalogue is not connected yet", "Open a source to browse its catalogue.", "Language", "Card layout", "Player", "Theme colour", "Built-in browser", "External VLC", "External MX Player", "Grid", "List", "Back", "I am 18 or older", "For adults only", "Enter search"),
    "uk" to listOf("Виберіть джерело", "Оновити", "Пошук", "Налаштування", "Джерела", "Каталог відео ще не підключено", "Відкрийте джерело, щоб переглянути його каталог.", "Мова", "Вигляд карток", "Плеєр", "Колір теми", "Вбудований браузер", "Зовнішній VLC", "Зовнішній MX Player", "Сітка", "Список", "Назад", "Мені виповнилося 18 років", "Лише для повнолітніх користувачів", "Введіть запит")
)
private fun tr(p: Preferences, index: Int) = translations[p.language.code]!![index]
private val accents = listOf(0xFFFF6B35.toInt(), 0xFF36A3FF.toInt(), 0xFFB66CFF.toInt(), 0xFF28B889.toInt())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("pasha18", MODE_PRIVATE)
        val initial = Preferences(
            language = AppLanguage.values().firstOrNull { it.code == prefs.getString("language", "ru") } ?: AppLanguage.RU,
            grid = GridMode.values().firstOrNull { it.name == prefs.getString("grid", "NINE") } ?: GridMode.NINE,
            player = PlayerMode.values().firstOrNull { it.name == prefs.getString("player", "INTERNAL") } ?: PlayerMode.INTERNAL,
            accent = prefs.getInt("accent", accents.first())
        )
        setContent {
            Pasha18App(initial) { p ->
                prefs.edit().putString("language", p.language.code).putString("grid", p.grid.name)
                    .putString("player", p.player.name).putInt("accent", p.accent).apply()
            }
        }
    }
}

@Composable
private fun Pasha18App(initial: Preferences, save: (Preferences) -> Unit) {
    var preferences by remember { mutableStateOf(initial) }
    var ageConfirmed by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(Page.HOME) }
    var selectedSite by remember { mutableStateOf<VideoSite?>(null) }
    var searchText by remember { mutableStateOf("") }
    val accent = Color(preferences.accent)
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF090B10)) {
            when {
                !ageConfirmed -> LanguageGate(preferences, onLanguage = { preferences = preferences.copy(language = it); save(preferences) },
                    onConfirm = { ageConfirmed = true })
                selectedSite != null -> SiteBrowser(selectedSite!!, preferences, accent, onBack = { selectedSite = null })
                page == Page.SETTINGS -> SettingsScreen(preferences, accent, onChange = {
                    preferences = it; save(it)
                }, onBack = { page = Page.HOME })
                page == Page.SEARCH -> SearchScreen(preferences, accent, value = searchText,
                    onValue = { searchText = it }, onSelect = { selectedSite = it }, onBack = { page = Page.HOME })
                else -> HomeScreen(preferences, accent, onRefresh = { selectedSite = sites.first() },
                    onSearch = { page = Page.SEARCH }, onSettings = { page = Page.SETTINGS },
                    onSelect = { selectedSite = it })
            }
        }
    }
}

@Composable
private fun LanguageGate(p: Preferences, onLanguage: (AppLanguage) -> Unit, onConfirm: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(42.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("PASHA 18+", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        Text("Choose language / Sprache wählen / Выберите язык / Оберіть мову",
            color = Color.LightGray, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLanguage.values().forEach { lang ->
                OutlinedButton(onClick = { onLanguage(lang) },
                    border = BorderStroke(if (p.language == lang) 3.dp else 1.dp, if (p.language == lang) Color(p.accent) else Color.Gray)) {
                    Text(lang.label, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(tr(p, 18), color = Color(0xFFB9C0CC), fontSize = 17.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(p.accent))) {
            Text(tr(p, 17))
        }
    }
}

@Composable
private fun HomeScreen(p: Preferences, accent: Color, onRefresh: () -> Unit, onSearch: () -> Unit,
                       onSettings: () -> Unit, onSelect: (VideoSite) -> Unit) {
    Row(Modifier.fillMaxSize().padding(24.dp)) {
        Column(Modifier.width(170.dp).fillMaxSize().background(Color(0xFF111722), RoundedCornerShape(18.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PASHA 18+", color = accent, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text(tr(p, 1)) }
            OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth()) { Text(tr(p, 2), color = Color.White) }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text(tr(p, 3), color = Color.White) }
            Spacer(Modifier.weight(1f))
            Text(tr(p, 4), color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f).fillMaxSize()) {
            Text(tr(p, 0), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text(tr(p, 5), color = Color(0xFFB9C0CC), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(tr(p, 6), color = Color.Gray, fontSize = 15.sp)
            Spacer(Modifier.height(18.dp))
            LazyVerticalGrid(
                columns = if (p.grid == GridMode.LIST) GridCells.Fixed(1) else GridCells.Fixed(p.grid.count.coerceAtMost(6)),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) { items(sites) { site -> SiteCard(site, accent, p.grid == GridMode.LIST, onClick = { onSelect(site) }) } }
        }
    }
}

@Composable
private fun SiteCard(site: VideoSite, accent: Color, list: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().aspectRatio(if (list) 5.8f else 1.65f)
        .onFocusChanged { focused = it.isFocused }, shape = RoundedCornerShape(16.dp),
        border = if (focused) BorderStroke(3.dp, accent) else null,
        colors = ButtonDefaults.buttonColors(containerColor = if (focused) Color(0xFF30394A) else Color(0xFF191F2B)),
        contentPadding = PaddingValues(12.dp)) {
        Text(site.name, color = Color.White, fontSize = if (list) 21.sp else 25.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SearchScreen(p: Preferences, accent: Color, value: String, onValue: (String) -> Unit,
                         onSelect: (VideoSite) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(36.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text(tr(p, 16)) }
            Spacer(Modifier.width(18.dp))
            Text(tr(p, 2), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value, onValue, modifier = Modifier.fillMaxWidth(),
            label = { Text(tr(p, 19)) }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        Text("Выберите источник для поиска по его собственному каталогу. Единый поиск появится после подключения каталогов источников.",
            color = Color.LightGray, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            sites.forEach { site -> Button(onClick = { onSelect(site) }, colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text(site.name) } }
        }
    }
}

@Composable
private fun SettingsScreen(p: Preferences, accent: Color, onChange: (Preferences) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text(tr(p, 16)) }
            Spacer(Modifier.width(18.dp))
            Text(tr(p, 3), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        SettingLabel(tr(p, 7))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.values().forEach { lang -> Choice(lang.label, p.language == lang, accent) { onChange(p.copy(language = lang)) } }
        }
        Spacer(Modifier.height(18.dp))
        SettingLabel(tr(p, 8))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GridMode.values().forEach { mode -> Choice(if (mode == GridMode.LIST) tr(p, 15) else mode.label, p.grid == mode, accent) { onChange(p.copy(grid = mode)) } }
        }
        Spacer(Modifier.height(18.dp))
        SettingLabel(tr(p, 9))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice(tr(p, 10), p.player == PlayerMode.INTERNAL, accent) { onChange(p.copy(player = PlayerMode.INTERNAL)) }
            Choice("VLC", p.player == PlayerMode.VLC, accent) { onChange(p.copy(player = PlayerMode.VLC)) }
            Choice("MX Player", p.player == PlayerMode.MX, accent) { onChange(p.copy(player = PlayerMode.MX)) }
        }
        Spacer(Modifier.height(18.dp))
        SettingLabel(tr(p, 10))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            accents.forEach { color ->
                Button(onClick = { onChange(p.copy(accent = color)) }, modifier = Modifier.width(52.dp).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(color)), contentPadding = PaddingValues(0.dp)) {
                    if (p.accent == color) Text("✓", color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Внешний плеер будет вызываться только для прямых ссылок на медиафайлы, если сайт разрешает их передачу. Ссылки внутри защищённых веб-плееров могут оставаться только во встроенном браузере.",
            color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun SettingLabel(text: String) { Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }

@Composable
private fun Choice(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) accent else Color.Gray)) {
        Text(label, color = Color.White)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SiteBrowser(site: VideoSite, p: Preferences, accent: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    var canGoForward by remember(site.url) { mutableStateOf(false) }
    var isLoading by remember(site.url) { mutableStateOf(true) }
    val webView = remember(site.url) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            isFocusable = true
            isFocusableInTouchMode = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    if (uri.scheme != "https" && uri.scheme != "http") return true
                    val path = uri.path.orEmpty().lowercase()
                    val isMedia = path.endsWith(".mp4") || path.endsWith(".m3u8") || path.endsWith(".webm")
                    if (isMedia && p.player != PlayerMode.INTERNAL) {
                        val packageName = if (p.player == PlayerMode.VLC) "org.videolan.vlc" else "com.mxtech.videoplayer.ad"
                        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/*").setPackage(packageName)
                        try { context.startActivity(intent) } catch (_: Exception) {
                            val fallback = Intent(Intent.ACTION_VIEW, uri).setDataAndType(uri, "video/*")
                            try { context.startActivity(fallback) } catch (_: Exception) { }
                        }
                        return true
                    }
                    return false
                }
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) { isLoading = true }
                override fun onPageFinished(view: WebView?, url: String?) { isLoading = false; canGoForward = view?.canGoForward() == true }
            }
            webChromeClient = object : WebChromeClient() {}
            loadUrl(site.url)
        }
    }
    BackHandler { if (webView.canGoBack()) webView.goBack() else onBack() }
    DisposableEffect(webView) { onDispose { webView.stopLoading(); webView.destroy() } }
    Column(Modifier.fillMaxSize().background(Color(0xFF090B10))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF151923)).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { if (webView.canGoBack()) webView.goBack() else onBack() }) { Text("‹") }
            Spacer(Modifier.width(6.dp))
            Button(onClick = { if (webView.canGoForward()) webView.goForward() }, enabled = canGoForward) { Text("›") }
            Spacer(Modifier.width(6.dp))
            Button(onClick = { webView.reload() }, colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text(tr(p, 1)) }
            Spacer(Modifier.width(6.dp))
            Button(onClick = onBack) { Text("⌂") }
            if (isLoading) Text("  …", color = accent)
        }
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
    }
}
