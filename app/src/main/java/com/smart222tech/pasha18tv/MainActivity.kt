package com.smart222tech.pasha18tv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.Toast
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Bg = Color(0xFF080B12)
private val Panel = Color(0xFF151C29)
private val Muted = Color(0xFF98A4B7)
private val Accent = Color(0xFFFF6B45)
private val FocusLight = Color(0xFFF4F7FC)
private fun tr(language: String, ru: String, en: String) = if (language == "en") en else ru
private const val API = "https://adultembed.com"

private data class VideoEntry(
    val id: String, val title: String, val synopsis: String, val posterUrl: String,
    val playerUrl: String, val category: String, val duration: String
)
private data class VideoCategory(val name: String, val slug: String, val count: Int)
private data class VideoPage(val items: List<VideoEntry>, val hasNext: Boolean, val hasPrevious: Boolean, val total: Int)

private object AdultEmbedRepository {
    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(API + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 20000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "PASHXXX-AndroidTV/0.6")
        }
        try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            JSONObject(body)
        } finally { connection.disconnect() }
    }

    suspend fun categories(): List<VideoCategory> {
        val json = get("/api/taxonomy/categories/?per_page=100&min_videos=1&ordering=popular")
        val rows = json.optJSONArray("results") ?: return emptyList()
        return (0 until rows.length()).mapNotNull { i ->
            rows.optJSONObject(i)?.let { VideoCategory(it.optString("name"), it.optString("slug"), it.optInt("video_count")) }
        }
    }

    suspend fun videos(query: String = "", category: String = "", page: Int = 1): VideoPage {
        val params = mutableListOf("orientation=all", "page="+page, "page_size=40", "sort=popular")
        if (query.isNotBlank()) params += "search=" + java.net.URLEncoder.encode(query, "UTF-8")
        if (category.isNotBlank()) params += "categories=" + java.net.URLEncoder.encode(category, "UTF-8")
        val json = get("/api/database/browse/?"+params.joinToString("&"))
        val rows = json.optJSONArray("results") ?: return VideoPage(emptyList(), false, page > 1, 0)
        val items = (0 until rows.length()).mapNotNull { i ->
            val item = rows.optJSONObject(i) ?: return@mapNotNull null
            val id = item.optString("id")
            if (id.isBlank() || id == "0") return@mapNotNull null
            val cats = item.optJSONArray("categories")
            val cat = cats?.optJSONObject(0)?.optString("name").orEmpty().ifBlank { "Видео" }
            VideoEntry(id, item.optString("title").ifBlank { "Видео #"+id }, item.optString("description"), item.optString("thumbnail"), "$API/player/embed/$id/", cat, item.optString("duration"))
        }
        return VideoPage(items, !json.isNull("next") && json.optString("next").isNotBlank(), !json.isNull("previous") && json.optString("previous").isNotBlank(), json.optInt("count", items.size))
    }
}

private enum class Tab { HOME, SEARCH, FAVORITES, SETTINGS }
private sealed interface Page {
    data object Browse : Page
    data class Details(val video: VideoEntry) : Page
    data class Player(val video: VideoEntry) : Page
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
                PashxxxApp()
            }
        }
    }
}

@Composable
private fun PashxxxApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pashxxx_favorites", 0) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("ids", emptySet())?.toSet() ?: emptySet()) }
    var page by remember { mutableStateOf<Page>(Page.Browse) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var videos by remember { mutableStateOf<List<VideoEntry>>(emptyList()) }
    var catalogPage by remember { mutableIntStateOf(1) }
    var hasNextPage by remember { mutableStateOf(false) }
    var hasPreviousPage by remember { mutableStateOf(false) }
    var totalVideos by remember { mutableIntStateOf(0) }
    var categories by remember { mutableStateOf<List<VideoCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var accepted18 by remember { mutableStateOf(prefs.getBoolean("age_ok", false)) }
    var language by remember { mutableStateOf(prefs.getString("language", "") ?: "") }
    var menuVisible by remember { mutableStateOf(true) }
    var showPlayerChooser by remember { mutableStateOf(false) }
    val firstVideoFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    var externalPlayer by remember { mutableStateOf(prefs.getString("external_player", "builtin") ?: "builtin") }
    val scope = rememberCoroutineScope()

    fun loadCatalog(q: String = search, cat: String = selectedCategory, requestedPage: Int = 1) {
        scope.launch {
            loading = true; error = null
            try {
                val result = AdultEmbedRepository.videos(q, cat, requestedPage)
                videos = result.items
                catalogPage = requestedPage
                hasNextPage = result.hasNext
                hasPreviousPage = result.hasPrevious
                totalVideos = result.total
            } catch (t: Throwable) { error = "Не удалось загрузить каталог: " + (t.message ?: "ошибка сети") }
            finally { loading = false }
        }
    }
    LaunchedEffect(tab, menuVisible, videos.size) {
        if (!menuVisible && tab == Tab.HOME && videos.isNotEmpty()) {
            delay(180)
            runCatching { firstVideoFocusRequester.requestFocus() }
        } else if (!menuVisible && tab == Tab.SEARCH) {
            delay(180)
            runCatching { searchFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(Unit) {
        if (accepted18) {
            try { categories = AdultEmbedRepository.categories(); loadCatalog() }
            catch (t: Throwable) { error = "Не удалось подключиться к каталогу: ${t.message}" }
        }
    }
    BackHandler(enabled = page !is Page.Browse) {
        page = when (val p = page) { is Page.Player -> Page.Details(p.video); is Page.Details -> Page.Browse; Page.Browse -> Page.Browse }
    }

    if (language.isBlank()) {
        AlertDialog(onDismissRequest = {}, title = { Text("PASHXXX — Language / Язык") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Выберите язык / Choose your language") ; listOf("Русский" to "ru", "English" to "en").forEach { (label, code) -> Button(onClick = { language = code; prefs.edit().putString("language", code).apply() }, modifier = Modifier.fillMaxWidth()) { Text(label) } } } }, confirmButton = {})
    } else if (!accepted18) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("PASHXXX — 18+") },
            text = { Text(tr(language, "Приложение содержит материалы для совершеннолетних. Подтверди, что тебе исполнилось 18 лет.", "This app contains adult content. Please confirm that you are 18 or older.")) },
            confirmButton = { TextButton(onClick = {
                accepted18 = true; prefs.edit().putBoolean("age_ok", true).apply()
                scope.launch {
                    loading = true
                    try { categories = AdultEmbedRepository.categories(); val result = AdultEmbedRepository.videos(); videos = result.items; hasNextPage = result.hasNext; hasPreviousPage = result.hasPrevious; totalVideos = result.total; catalogPage = 1 }
                    catch (t: Throwable) { error = "Ошибка каталога: ${t.message}" }
                    finally { loading = false }
                }
            }) { Text(tr(language, "Мне есть 18 лет", "I am 18 or older")) } },
            dismissButton = { TextButton(onClick = { (context as? ComponentActivity)?.finish() }) { Text(tr(language, "Выйти", "Exit")) } }
        )
    }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        when (val current = page) {
            Page.Browse -> Row(Modifier.fillMaxSize().padding(22.dp)) {
                if (menuVisible) Column(
                    Modifier.width(190.dp).fillMaxHeight().clip(RoundedCornerShape(20.dp))
                        .background(Panel).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("PASHXXX", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("PRIVATE VIDEO LIBRARY", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    NavItem("⌂", tr(language, "Главная", "Home"), tab == Tab.HOME) { tab = Tab.HOME; menuVisible = false; selectedCategory = ""; loadCatalog("", "") }
                    NavItem("⌕", tr(language, "Поиск", "Search"), tab == Tab.SEARCH) { tab = Tab.SEARCH; menuVisible = false }
                    NavItem("♡", tr(language, "Избранное", "Favorites"), tab == Tab.FAVORITES) { tab = Tab.FAVORITES; menuVisible = false }
                    Spacer(Modifier.weight(1f))
                    NavItem("⚙", tr(language, "Настройки", "Settings"), tab == Tab.SETTINGS) { tab = Tab.SETTINGS; menuVisible = false }
                    Text("18+  •  PASHXXX", color = Muted, fontSize = 10.sp)
                }
                if (menuVisible) Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { menuVisible = !menuVisible }) { Text(if (menuVisible) "‹" else "☰  " + tr(language, "МЕНЮ", "MENU"), color = FocusLight, fontWeight = FontWeight.Bold) }
                        Text("PASHXXX", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    when (tab) {
                        Tab.SETTINGS -> {
                            Text(tr(language, "Настройки", "Settings"), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp))
                            Text(tr(language, "Язык интерфейса", "Interface language"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Русский" to "ru", "English" to "en").forEach { (label, code) ->
                                    FilterChip(selected = language == code, onClick = { language = code; prefs.edit().putString("language", code).apply() }, label = { Text(label) })
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(tr(language, "Плеер", "Player"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(tr(language, "Выберите плеер. Для этого каталога источник предоставляет iframe-плеер, поэтому VLC/MX Player могут не принять ссылку. Встроенный плеер открывает iframe внутри приложения.", "Choose a player. This catalogue provides an iframe player, not a direct media URL, so VLC/MX Player may reject it. Built-in mode opens the iframe inside the app."), color = Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = externalPlayer == "builtin", onClick = { externalPlayer = "builtin"; prefs.edit().putString("external_player", externalPlayer).apply() }, label = { Text(tr(language, "Встроенный", "Built-in")) })
                                FilterChip(selected = externalPlayer == "vlc", onClick = { externalPlayer = "vlc"; prefs.edit().putString("external_player", externalPlayer).apply() }, label = { Text("VLC") })
                                FilterChip(selected = externalPlayer == "mx", onClick = { externalPlayer = "mx"; prefs.edit().putString("external_player", externalPlayer).apply() }, label = { Text("MX Player") })
                            }
                            Text(tr(language, "Внешние плееры откроются только если установлены и поддерживают ссылку источника.", "External players will launch only if installed and able to handle this source URL."), color = Muted)
                            Spacer(Modifier.height(14.dp))
                            Text(tr(language, "Источник каталога: AdultEmbed API", "Catalogue source: AdultEmbed API"), color = Color.White, fontSize = 16.sp)
                            Text(tr(language, "Возрастное ограничение: 18+", "Age restriction: 18+"), color = Muted)
                            Text(tr(language, "На странице: ", "On page: ") + videos.size + tr(language, " · всего: ", " · total: ") + totalVideos, color = Muted)
                            Button(onClick = { loadCatalog(search, selectedCategory, 1) }, enabled = !loading) { Text(tr(language, "Обновить каталог", "Refresh catalogue")) }
                            OutlinedButton(onClick = { accepted18 = false; prefs.edit().putBoolean("age_ok", false).apply() }) { Text(tr(language, "Сбросить подтверждение возраста", "Reset age confirmation")) }
                        }
                        else -> {
                            Text(if (tab == Tab.FAVORITES) tr(language, "Избранное", "Favorites") else tr(language, "Главная", "Home"),
                                color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            if (tab == Tab.SEARCH) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 14.dp, vertical = 12.dp)) {
                                        if (search.isEmpty()) Text(tr(language, "Поиск по каталогу…", "Search videos…"), color = Muted)
                                        BasicTextField(value = search, onValueChange = { search = it },
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                                            singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Button(onClick = { loadCatalog(search, selectedCategory, 1); tab = Tab.HOME }, modifier = Modifier.height(48.dp)) { Text(tr(language, "Найти", "Search")) }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { FilterChip(selected = selectedCategory.isEmpty(), onClick = { selectedCategory = ""; tab = Tab.HOME; loadCatalog() }, label = { Text(tr(language, "Все", "All")) }) }
                                items(categories) { cat ->
                                    FilterChip(selected = selectedCategory == cat.slug, onClick = {
                                        selectedCategory = cat.slug; tab = Tab.HOME; loadCatalog()
                                    }, label = { Text("${cat.name} · ${cat.count}") })
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            error?.let { Text(it, color = Color(0xFFFF9A82)) }
                            val shown = if (tab == Tab.FAVORITES) videos.filter { it.id in favorites } else videos
                            Text(shown.size.toString() + tr(language, " видео · страница ", " videos · page ") + catalogPage + tr(language, " · всего ", " · total ") + totalVideos, color = Muted, fontSize = 12.sp)
if (tab != Tab.FAVORITES) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { if (hasPreviousPage) loadCatalog(search, selectedCategory, catalogPage - 1) }, enabled = hasPreviousPage && !loading) { Text(tr(language, "‹ Предыдущая", "‹ Previous")) }
        Text(tr(language, "Страница ", "Page ") + catalogPage, color = Color.White, fontWeight = FontWeight.Bold)
        Button(onClick = { if (hasNextPage) loadCatalog(search, selectedCategory, catalogPage + 1) }, enabled = hasNextPage && !loading) { Text(tr(language, "Следующая ›", "Next ›")) }
    }
}
                            Spacer(Modifier.height(8.dp))
                            if (!loading && shown.isEmpty() && error == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(if (tab == Tab.FAVORITES) tr(language, "Избранное пока пусто", "No favourites yet") else tr(language, "По этому запросу ничего не найдено", "No videos found"),
                                        color = Muted, fontSize = 18.sp)
                                }
                            } else {
                                LazyVerticalGrid(columns = GridCells.Adaptive(180.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(bottom = 20.dp)) {
                                    items(shown, key = { it.id }) { video ->
                                        val isFirst = video.id == shown.firstOrNull()?.id
                                        VideoCard(video, modifier = if (isFirst && tab != Tab.FAVORITES) Modifier.focusRequester(firstVideoFocusRequester) else Modifier) { page = Page.Details(video); showPlayerChooser = false }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is Page.Details -> {
                val video = current.video
                Row(Modifier.fillMaxSize().padding(30.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column(Modifier.weight(1f)) {
                        AsyncImage(model = video.posterUrl, contentDescription = video.title,
                            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop)
                    }
                    Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        TextButton(onClick = { page = Page.Browse }) { Text(tr(language, "‹ Назад", "‹ Back"), color = FocusLight) }
                        Text(video.category.uppercase(), color = Accent, fontSize = 12.sp)
                        Text(video.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(video.duration, color = Muted)
                        Spacer(Modifier.height(14.dp))
                        Text(video.synopsis, color = Color(0xFFD2D8E2), fontSize = 15.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { showPlayerChooser = true }) { Text(tr(language, "▶ Смотреть", "▶ Watch"), color = Color.Black) }
                            OutlinedButton(onClick = {
                                favorites = if (video.id in favorites) favorites - video.id else favorites + video.id
                                prefs.edit().putStringSet("ids", favorites).apply()
                            }) { Text(if (video.id in favorites) tr(language, "♥ В избранном", "♥ Saved") else tr(language, "♡ В избранное", "♡ Add to favourites"), color = FocusLight) }
                        }
                    }
                    if (showPlayerChooser) {
                        AlertDialog(
                            onDismissRequest = { showPlayerChooser = false },
                            title = { Text(tr(language, "Выбор плеера", "Choose player")) },
                            text = { Text(tr(language, "Предпочтение из настроек: " + when (externalPlayer) { "vlc" -> "VLC"; "mx" -> "MX Player"; else -> "Встроенный" } + ". AdultEmbed передаёт страницу iframe, а не прямой видеофайл. VLC/MX Player могут не поддерживать такую ссылку.", "Preferred in Settings: " + when (externalPlayer) { "vlc" -> "VLC"; "mx" -> "MX Player"; else -> "Built-in" } + ". AdultEmbed provides an iframe page, not a direct video file. VLC/MX Player may not support this link.")) },
                            confirmButton = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { showPlayerChooser = false; page = Page.Player(video) }, modifier = Modifier.fillMaxWidth()) { Text(tr(language, "Встроенный плеер", "Built-in player")) }
                                    OutlinedButton(onClick = { showPlayerChooser = false; openExternal(context, video, "org.videolan.vlc", tr(language, "VLC не установлен или не поддерживает ссылку. Открываю встроенный плеер.", "VLC is missing or cannot handle this link. Opening built-in player.")) { page = Page.Player(video) } }, modifier = Modifier.fillMaxWidth()) { Text("VLC") }
                                    OutlinedButton(onClick = { showPlayerChooser = false; openExternal(context, video, "com.mxtech.videoplayer.ad", tr(language, "MX Player не установлен или не поддерживает ссылку. Открываю встроенный плеер.", "MX Player is missing or cannot handle this link. Opening built-in player.")) { page = Page.Player(video) } }, modifier = Modifier.fillMaxWidth()) { Text("MX Player") }
                                }
                            },
                            dismissButton = { TextButton(onClick = { showPlayerChooser = false }) { Text(tr(language, "Отмена", "Cancel")) } }
                        )
                    }
                }
            }
            is Page.Player -> EmbeddedPlayer(current.video) { page = Page.Details(current.video) }
        }
    }
}

@Composable
private fun NavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
        .clip(RoundedCornerShape(10.dp))
        .background(if (focused) FocusLight else if (selected) Color(0xFF30394A) else Color.Transparent)
        .border(if (focused) 3.dp else 1.dp, if (focused) FocusLight else Color.Transparent, RoundedCornerShape(10.dp))
        .clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = if (focused) Color.Black else Accent); Spacer(Modifier.width(10.dp))
        Text(label, color = if (focused) Color.Black else Color.White, fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun VideoCard(video: VideoEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.onFocusChanged { focused = it.isFocused }.focusable().border(if (focused) 4.dp else 1.dp, if (focused) FocusLight else Color(0xFF263246), RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(if (focused) FocusLight else Color.Black).clickable(onClick = onClick).padding(if (focused) 2.dp else 0.dp)) {
        AsyncImage(model = video.posterUrl, contentDescription = video.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(.72f), contentScale = ContentScale.Crop)
        Column(Modifier.padding(10.dp)) {
            Text(video.title, color = if (focused) Color.Black else Color.White, fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2)
            Text("${video.category}  •  ${video.duration}", color = if (focused) Color.DarkGray else Muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

private fun openExternal(context: android.content.Context, video: VideoEntry, packageName: String, fallbackMessage: String, fallback: () -> Unit) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.playerUrl)).setPackage(packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, fallbackMessage, Toast.LENGTH_LONG).show()
        fallback()
    } catch (_: Exception) {
        Toast.makeText(context, fallbackMessage, Toast.LENGTH_LONG).show()
        fallback()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedPlayer(video: VideoEntry, onBack: () -> Unit) {
    BackHandler { onBack() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                isFocusable = true
                isFocusableInTouchMode = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"></head><body style="margin:0;background:#000;overflow:hidden"><iframe src="${video.playerUrl}?autoplay=1&muted=1" style="position:fixed;inset:0;width:100%;height:100%;border:0" allow="autoplay; encrypted-media; fullscreen; picture-in-picture" allowfullscreen></iframe></body></html>"""
                loadDataWithBaseURL("https://adultembed.com/", html, "text/html", "UTF-8", null)
            }
        }, modifier = Modifier.fillMaxSize())
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Text(tr("ru", "‹ Назад", "‹ Back"), color = Color.White)
        }
    }
}
