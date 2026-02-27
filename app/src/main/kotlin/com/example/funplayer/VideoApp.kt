package com.example.funplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoApp(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isDeveloperMode by remember { mutableStateOf(getIsDeveloperMode(context)) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var currentTab by remember { mutableStateOf(MainTab.Home) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    var searchText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showLogWindow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isDeveloperMode) DevLog.log("App", "开发者模式已开启，可点击 Log 悬浮窗查看日志")
    }
    LaunchedEffect(Unit) {
        when (getVideoLibrarySource(context)) {
            "local" -> {
                val uri = getStoredVideoLibraryUri(context)
                if (uri != null) {
                    val list = withContext(Dispatchers.IO) {
                        collectVideosFromTree(context, android.net.Uri.parse(uri))
                    }
                    videos = list
                }
            }
            "nas" -> {
                val host = getNasHost(context)
                val share = getNasShare(context)
                if (host.isNotBlank() && share.isNotBlank()) {
                    val list = withContext(Dispatchers.IO) {
                        collectVideosFromSmb(
                            context,
                            host,
                            share,
                            getNasUser(context),
                            getNasPassword(context),
                            getNasPort(context),
                            getNasSubpath(context)
                        )
                    }
                    videos = list
                }
            }
            else -> { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    MainScaffold(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        videos = videos,
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        selectedTags = selectedTags,
                        onTagToggled = { tag ->
                            selectedTags = if (tag == TAG_KEY_ALL) emptySet()
                            else if (tag in selectedTags) selectedTags - tag
                            else selectedTags + tag
                        },
                        onVideoClick = { videoId ->
                            currentScreen = Screen.VideoDetail(videoId)
                        },
                        isDarkTheme = isDarkTheme,
                        onThemeChange = onThemeChange,
                        onVideosUpdate = { videos = it },
                        onDeveloperModeChange = { isDeveloperMode = it }
                    )
                }

                is Screen.VideoDetail -> {
                    val video = videos.firstOrNull { it.id == screen.videoId }
                    if (video != null) {
                        VideoDetailScreen(
                            video = video,
                            onBack = { currentScreen = Screen.Main },
                            onTagsChanged = { newTags ->
                                videos = videos.map {
                                    if (it.id == video.id) it.copy(tags = newTags) else it
                                }
                                saveTagsForVideo(context, video, newTags)
                            }
                        )
                    } else {
                        currentScreen = Screen.Main
                    }
                }
            }
        }

        if (isDeveloperMode) {
            if (showLogWindow) {
                DevLogOverlay(
                    onClose = { showLogWindow = false },
                    onClear = { DevLog.clear() },
                    modifier = Modifier.zIndex(999f)
                )
            } else {
                FloatingActionButton(
                    onClick = { showLogWindow = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .zIndex(998f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(stringResource(R.string.dev_log_fab), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
internal fun DevLogOverlay(
    onClose: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val entries = DevLog.entries
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.dev_log_title),
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = onClear) {
                        Text(stringResource(R.string.dev_log_clear))
                    }
                    androidx.compose.material3.TextButton(onClick = onClose) {
                        Text(stringResource(R.string.dev_log_close))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(entries.size, key = { it }) { i ->
                        val e = entries.getOrNull(i) ?: return@items
                        Text(
                            text = "${android.text.format.DateFormat.format("HH:mm:ss.SSS", e.time)} [${e.tag}] ${e.msg}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
