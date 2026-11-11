package com.smart222tech.pasha18tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val Background = Color(0xFF080B12)
private val Panel = Color(0xFF151C29)
private val Muted = Color(0xFF98A4B7)
private val Accent = Color(0xFFFF6B45)

/** A catalogue entry must come from a source that permits displaying its metadata and playback URL. */
private data class VideoEntry(
    val id: String,
    val title: String,
    val synopsis: String,
    val posterUrl: String,
    val playbackUrl: String,
    val category: String,
    val duration: String
)

/** Intentionally empty until a permitted catalogue/feed endpoint is configured. */
private object VideoCatalogRepository {
    fun load(): List<VideoEntry> = emptyList()
}

private sealed interface Screen {
    data object Catalogue : Screen
    data class Details(val entry: VideoEntry) : Screen
    data class Player(val entry: VideoEntry) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Background,
                    surface = Panel,
                    onPrimary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                PashxxxApp()
            }
        }
    }
}

@Composable
private fun PashxxxApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Catalogue) }
    val entries = remember { VideoCatalogRepository.load() }
    BackHandler(enabled = screen !is Screen.Catalogue) {
        screen = when (screen) {
            is Screen.Player -> Screen.Details((screen as Screen.Player).entry)
            is Screen.Details -> Screen.Catalogue
            Screen.Catalogue -> Screen.Catalogue
        }
    }
    Surface(Modifier.fillMaxSize(), color = Background) {
        when (val current = screen) {
            Screen.Catalogue -> CatalogueScreen(entries, onOpen = { screen = Screen.Details(it) })
            is Screen.Details -> DetailsScreen(current.entry,
                onBack = { screen = Screen.Catalogue },
                onPlay = { screen = Screen.Player(current.entry) })
            is Screen.Player -> NativePlayerScreen(current.entry, onBack = { screen = Screen.Details(current.entry) })
        }
    }
}

@Composable
private fun CatalogueScreen(entries: List<VideoEntry>, onOpen: (VideoEntry) -> Unit) {
    Row(Modifier.fillMaxSize().padding(28.dp)) {
        Column(
            Modifier.width(205.dp).fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1B2433), Color(0xFF111722))))
                .border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(24.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PASHXXX", fontSize = 27.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("PRIVATE ENTERTAINMENT", fontSize = 9.sp, color = Accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(22.dp))
            Text("DISCOVER", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold)
            SideItem("⌂", "Главная", selected = true)
            SideItem("▦", "Каталог", selected = false)
            SideItem("♡", "Избранное", selected = false)
            Spacer(Modifier.weight(1f))
            Text("18+  •  PASHXXX", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.width(30.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ТВОЯ ВИДЕОТЕКА", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Открой для себя", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Text("PASHXXX  /  TV", color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF38263A), Color(0xFF202D42), Color(0xFF141C2A))))
                    .border(1.dp, Color.White.copy(alpha = .07f), RoundedCornerShape(22.dp))
                    .padding(26.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ТВОЙ ПЕРСОНАЛЬНЫЙ КАТАЛОГ", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Видео. Истории. Впечатления.", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Постеры, описание каждой сцены и запуск в нативном плеере", color = Color(0xFFCCD2DC), fontSize = 13.sp)
                }
                Text("P", Modifier.align(Alignment.CenterEnd).padding(end = 18.dp),
                    color = Color.White.copy(alpha = .05f), fontSize = 116.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Каталог видео", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("${entries.size} ВИДЕО", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            if (entries.isEmpty()) {
                EmptyCatalogue()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 185.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(entries, key = { it.id }) { entry -> VideoPosterCard(entry, onClick = { onOpen(entry) }) }
                }
            }
        }
    }
}

@Composable
private fun SideItem(symbol: String, label: String, selected: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF30394A) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, color = if (selected) Accent else Muted, fontSize = 17.sp)
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (selected) Color.White else Muted, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun EmptyCatalogue() {
    Box(
        Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp))
            .background(Panel).border(1.dp, Color.White.copy(alpha = .055f), RoundedCornerShape(22.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(62.dp).clip(RoundedCornerShape(19.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF493044), Color(0xFF26374B)))),
                contentAlignment = Alignment.Center
            ) { Text("▶", color = Accent, fontSize = 24.sp) }
            Text("Каталог пока пуст", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Нужен разрешённый источник, который предоставляет постеры, описания и прямые ссылки на воспроизведение. Как только он подключён, здесь появятся карточки видео.",
                color = Muted, fontSize = 13.sp, lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun VideoPosterCard(entry: VideoEntry, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Panel)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(.72f).background(Color(0xFF252F40)), contentAlignment = Alignment.Center) {
            Text("▶", color = Accent, fontSize = 34.sp)
        }
        Column(Modifier.padding(12.dp)) {
            Text(entry.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(entry.category + "  •  " + entry.duration, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DetailsScreen(entry: VideoEntry, onBack: () -> Unit, onPlay: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(30.dp)) {
        TextButton(onClick = onBack) { Text("‹  Назад", color = Accent) }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Box(
                Modifier.weight(.8f).fillMaxHeight().clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF222D40)), contentAlignment = Alignment.Center
            ) { Text("POSTER", color = Muted, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(entry.category.uppercase(), color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(entry.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(entry.duration, color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                Text("ОПИСАНИЕ СЦЕНЫ", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(entry.synopsis, color = Color(0xFFD2D8E2), fontSize = 16.sp, lineHeight = 24.sp)
                Spacer(Modifier.height(28.dp))
                Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                    Text("▶  Смотреть", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NativePlayerScreen(entry: VideoEntry, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(entry.playbackUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(entry.playbackUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        TextButton(onClick = onBack) { Text("‹  Назад", color = Color.White) }
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
    }
}
