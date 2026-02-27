package com.example.funplayer

import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.OptIn
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoDetailScreen(
    video: VideoItem,
    onBack: () -> Unit,
    onTagsChanged: (List<String>) -> Unit
) {
    var isFullscreen by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler {
        if (isFullscreen) isFullscreen = false else onBack()
    }

    val activity = LocalContext.current as? Activity
    var fullscreenIsLandscape by remember { mutableStateOf(true) }

    androidx.compose.runtime.SideEffect {
        activity ?: return@SideEffect
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        if (isFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(isFullscreen, fullscreenIsLandscape) {
        activity ?: return@LaunchedEffect
        if (isFullscreen) {
            val orientation = if (fullscreenIsLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
            activity.requestedOrientation = orientation
        }
    }

    val context = LocalContext.current
    var localTags by remember(video) { mutableStateOf(video.tags) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteMode by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var loadedDuration by remember(video.uri) { mutableStateOf<String?>(null) }

    LaunchedEffect(video.uri) {
        if (video.uri != null && video.duration.isEmpty() && video.uri.startsWith("smb:", ignoreCase = true)) {
            val dur = withContext(Dispatchers.IO) { getVideoDurationFormattedFromUri(context, video.uri) }
            loadedDuration = dur
        }
    }
    val detailDisplayDuration = video.duration.ifEmpty { loadedDuration ?: "…" }

    val scriptKey = video.funscriptUri to video.funscriptUrisByAxis
    var funscriptData by remember(scriptKey) { mutableStateOf<FunscriptData?>(null) }
    var funscriptHeatmaps by remember(scriptKey) { mutableStateOf<Map<String, FloatArray>?>(null) }
    var playbackPositionMs by remember { mutableStateOf(0L) }
    LaunchedEffect(video.funscriptUri, video.funscriptUrisByAxis) {
        val multiUris = video.funscriptUrisByAxis
        val data = if (multiUris != null && multiUris.isNotEmpty()) {
            withContext(Dispatchers.IO) { loadFunscriptMultiFromUris(context, multiUris) }
        } else {
            val uri = video.funscriptUri
            if (uri == null) {
                funscriptData = null
                funscriptHeatmaps = null
                return@LaunchedEffect
            }
            withContext(Dispatchers.IO) { loadFunscriptFromUri(context, uri) }
        } ?: run {
            funscriptData = null
            funscriptHeatmaps = null
            return@LaunchedEffect
        }
        val totalMs = if (data.durationSec > 0) (data.durationSec * 1000).toLong()
            else data.axes.flatMap { it.actions }.maxOfOrNull { it.at } ?: 1L
        val totalMsCoerced = totalMs.coerceAtLeast(1L)
        val segmentCount = 120
        val heatmaps = data.axes.associate { axis ->
            axis.id to computeAxisHeatmap(axis.actions, totalMsCoerced, segmentCount)
        }
        funscriptData = data
        funscriptHeatmaps = heatmaps
    }

    LaunchedEffect(localTags) {
        onTagsChanged(localTags)
    }

    val axisCommandForSend = buildAxisCommandFromScript(context, funscriptData, playbackPositionMs)
    LaunchedEffect(axisCommandForSend) {
        if (axisCommandForSend.isEmpty()) return@LaunchedEffect
        if (!getConnectionEnabled(context)) return@LaunchedEffect
        sendAxisCommand(context, axisCommandForSend)
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(stringResource(R.string.video_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(if (isFullscreen) 0.dp else 16.dp)
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                )
        ) {
            Box(
                modifier = if (isFullscreen) Modifier.fillMaxSize().background(Color.Black)
                else Modifier.fillMaxWidth().aspectRatio(16 / 9f)
            ) {
                VideoPlayerEmbed(
                    videoUri = video.uri,
                    modifier = Modifier.fillMaxSize(),
                    onPlaybackPosition = { playbackPositionMs = it }
                )
                if (isFullscreen) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .zIndex(2f)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.exit_fullscreen),
                            tint = Color.White
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .zIndex(2f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { fullscreenIsLandscape = !fullscreenIsLandscape }
                        ) {
                            Icon(
                                Icons.Filled.ScreenRotation,
                                contentDescription = stringResource(R.string.rotate_fullscreen),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { isFullscreen = false }) {
                            Icon(
                                Icons.Filled.FullscreenExit,
                                contentDescription = stringResource(R.string.exit_fullscreen),
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { isFullscreen = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Fullscreen,
                            contentDescription = stringResource(R.string.fullscreen_play),
                            tint = Color.White
                        )
                    }
                }
            }

            if (!isFullscreen) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = video.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.video_duration_format, detailDisplayDuration),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.label_tags),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    localTags.forEach { tag ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(tag, fontSize = 12.sp)
                            if (showDeleteMode) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "×",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .clickable {
                                            localTags = localTags.filterNot { it == tag }
                                        }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    AssistChip(
                        onClick = { showAddDialog = true },
                        label = { Text(stringResource(R.string.add_tag_btn)) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    AssistChip(
                        onClick = { showDeleteMode = !showDeleteMode },
                        label = { Text(if (showDeleteMode) stringResource(R.string.done) else stringResource(R.string.add_tag_done_btn)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.script_heatmap),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val heatmaps = funscriptHeatmaps
                val scriptData = funscriptData
                if (scriptData != null && heatmaps != null && heatmaps.isNotEmpty()) {
                    val coolColor = Color(0xFF2196F3)
                    val hotColor = Color(0xFFF44336)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        scriptData.axes.forEach { axis ->
                            val intensities = heatmaps[axis.id] ?: return@forEach
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = axis.id,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.width(28.dp)
                                )
                                Canvas(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp)
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val segW = w / intensities.size.coerceAtLeast(1)
                                    intensities.forEachIndexed { i, t ->
                                        val color = lerp(coolColor, hotColor, t)
                                        drawRect(
                                            color = color,
                                            topLeft = Offset(i * segW, 0f),
                                            size = Size(segW + 1f, h)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                if (video.funscriptUri == null && video.funscriptUrisByAxis.isNullOrEmpty())
                                    R.string.no_script else R.string.script_loading
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (getIsDeveloperMode(context) && axisCommandForSend.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.axis_command),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = axisCommandForSend,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.add_tag_dialog_title)) },
                text = {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text(stringResource(R.string.tag_name_label)) }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trimmed = newTagText.trim()
                            if (trimmed.isNotEmpty() && !localTags.contains(trimmed)) {
                                localTags = localTags + trimmed
                            }
                            newTagText = ""
                            showAddDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
