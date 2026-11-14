package com.smart222tech.pasha18tv

import android.annotation.SuppressLint
import android.os.Bundle
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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Bg = Color(0xFF080B12)
private val Panel = Color(0xFF151C29)
private val Muted = Color(0xFF98A4B7)
private val Accent = Color(0xFFFF6B45)
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

private enum class Tab { HOME, CATALOGUE, FAVORITES, SETTINGS }
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
        AlertDialog(onDismissRequest = {}, title = { Text("PASHXXX — Выбор языка / Language / Sprache") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Выбери язык приложения") ; listOf("Русский" to "ru", "English" to "en", "Deutsch" to "de").forEach { (label, code) -> Button(onClick = { language = code; prefs.edit().putString("language", code).apply() }, modifier = Modifier.fillMaxWidth()) { Text(label) } } } }, confirmButton = {})
    } else if (!accepted18) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("PASHXXX — 18+") },
            text = { Text("Приложение содержит материалы для совершеннолетних. Подтверди, что тебе исполнилось 18 лет.") },
            confirmButton = { TextButton(onClick = {
                accepted18 = true; prefs.edit().putBoolean("age_ok", true).apply()
                scope.launch {
                    loading = true
                    try { categories = AdultEmbedRepository.categories(); val result = AdultEmbedRepository.videos(); videos = result.items; hasNextPage = result.hasNext; hasPreviousPage = result.hasPrevious; totalVideos = result.total; catalogPage = 1 }
                    catch (t: Throwable) { error = "Ошибка каталога: ${t.message}" }
                    finally { loading = false }
                }
            }) { Text("Мне есть 18 лет") } },
            dismissButton = { TextButton(onClick = { (context as? ComponentActivity)?.finish() }) { Text("Выйти") } }
        )
    }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        when (val current = page) {
            Page.Browse -> Row(Modifier.fillMaxSize().padding(22.dp)) {
                Column(
                    Modifier.width(190.dp).fillMaxHeight().clip(RoundedCornerShape(20.dp))
                        .background(Panel).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("PASHXXX", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("PRIVATE VIDEO LIBRARY", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    NavItem("⌂", "Главная", tab == Tab.HOME) { tab = Tab.HOME; selectedCategory = ""; loadCatalog("", "") }
                    NavItem("▦", "Каталог", tab == Tab.CATALOGUE) { tab = Tab.CATALOGUE; selectedCategory = ""; loadCatalog("", "") }
                    NavItem("♡", "Избранное", tab == Tab.FAVORITES) { tab = Tab.FAVORITES }
                    Spacer(Modifier.weight(1f))
                    NavItem("⚙", "Настройки", tab == Tab.SETTINGS) { tab = Tab.SETTINGS }
                    Text("18+  •  PASHXXX", color = Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.width(24.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Text("PASHXXX  /  ${tab.name}", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    when (tab) {
                        Tab.SETTINGS -> {
                            Text("Настройки", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp))
                            Text("Язык интерфейса", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Русский" to "ru", "English" to "en", "Deutsch" to "de").forEach { (label, code) ->
                                    FilterChip(selected = language == code, onClick = { language = code; prefs.edit().putString("language", code).apply() }, label = { Text(label) })
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Воспроизведение", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Основной режим: встроенный WebView. VLC и MX Player не получают прямую ссылку на видео от источника, поэтому пока не могут быть гарантированным способом воспроизведения.", color = Muted)
                            Spacer(Modifier.height(14.dp))
                            Text("Источник каталога: AdultEmbed API", color = Color.White, fontSize = 16.sp)
                            Text("Возрастное ограничение: 18+", color = Muted)
                            Text("Загружено на странице: " + videos.size + " · всего найдено: " + totalVideos, color = Muted)
                            Button(onClick = { loadCatalog(search, selectedCategory, 1) }, enabled = !loading) { Text("Обновить каталог") }
                            OutlinedButton(onClick = { accepted18 = false; prefs.edit().putBoolean("age_ok", false).apply() }) { Text("Сбросить подтверждение возраста") }
                        }
                        else -> {
                            Text(if (tab == Tab.FAVORITES) "Избранное" else if (tab == Tab.HOME) "Главная" else "Каталог видео",
                                color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 14.dp, vertical = 12.dp)) {
                                    if (search.isEmpty()) Text("Поиск по каталогу…", color = Muted)
                                    BasicTextField(value = search, onValueChange = { search = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                                        singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                                Spacer(Modifier.width(10.dp))
                                Button(onClick = { tab = Tab.CATALOGUE; loadCatalog() }) { Text("Найти") }
                            }
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { FilterChip(selected = selectedCategory.isEmpty(), onClick = { selectedCategory = ""; tab = Tab.CATALOGUE; loadCatalog() }, label = { Text("Все") }) }
                                items(categories) { cat ->
                                    FilterChip(selected = selectedCategory == cat.slug, onClick = {
                                        selectedCategory = cat.slug; tab = Tab.CATALOGUE; loadCatalog()
                                    }, label = { Text("${cat.name} · ${cat.count}") })
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            error?.let { Text(it, color = Color(0xFFFF9A82)) }
                            val shown = if (tab == Tab.FAVORITES) videos.filter { it.id in favorites } else videos
                            Text(shown.size.toString() + " видео · страница " + catalogPage + " · всего " + totalVideos, color = Muted, fontSize = 12.sp)
if (tab != Tab.FAVORITES) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { if (hasPreviousPage) loadCatalog(search, selectedCategory, catalogPage - 1) }, enabled = hasPreviousPage && !loading) { Text("‹ Предыдущая") }
        Text("Страница " + catalogPage, color = Color.White, fontWeight = FontWeight.Bold)
        Button(onClick = { if (hasNextPage) loadCatalog(search, selectedCategory, catalogPage + 1) }, enabled = hasNextPage && !loading) { Text("Следующая ›") }
    }
}
                            Spacer(Modifier.height(8.dp))
                            if (!loading && shown.isEmpty() && error == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(if (tab == Tab.FAVORITES) "Избранное пока пусто" else "По этому запросу ничего не найдено",
                                        color = Muted, fontSize = 18.sp)
                                }
                            } else {
                                LazyVerticalGrid(columns = GridCells.Adaptive(180.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(bottom = 20.dp)) {
                                    items(shown, key = { it.id }) { video ->
                                        VideoCard(video) { page = Page.Details(video) }
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
                        TextButton(onClick = { page = Page.Browse }) { Text("‹ Назад") }
                        Text(video.category.uppercase(), color = Accent, fontSize = 12.sp)
                        Text(video.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(video.duration, color = Muted)
                        Spacer(Modifier.height(14.dp))
                        Text(video.synopsis, color = Color(0xFFD2D8E2), fontSize = 15.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { page = Page.Player(video) }) { Text("▶ Смотреть") }
                            OutlinedButton(onClick = {
                                favorites = if (video.id in favorites) favorites - video.id else favorites + video.id
                                prefs.edit().putStringSet("ids", favorites).apply()
                            }) { Text(if (video.id in favorites) "♥ В избранном" else "♡ В избранное") }
                        }
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
        .background(if (focused || selected) Color(0xFF30394A) else Color.Transparent)
        .border(if (focused) 2.dp else 1.dp, if (focused) Accent else Color.Transparent, RoundedCornerShape(10.dp))
        .clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = Accent); Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun VideoCard(video: VideoEntry, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.onFocusChanged { focused = it.isFocused }.border(if (focused) 3.dp else 1.dp, if (focused) Accent else Color(0xFF263246), RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(if (focused) Color(0xFF273449) else Panel).clickable(onClick = onClick).padding(if (focused) 2.dp else 0.dp)) {
        AsyncImage(model = video.posterUrl, contentDescription = video.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(.72f), contentScale = ContentScale.Crop)
        Column(Modifier.padding(10.dp)) {
            Text(video.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2)
            Text("${video.category}  •  ${video.duration}", color = Muted, fontSize = 10.sp, maxLines = 1)
        }
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
                loadUrl(video.playerUrl)
            }
        }, modifier = Modifier.fillMaxSize())
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Text("‹ Назад", color = Color.White)
        }
    }
}
