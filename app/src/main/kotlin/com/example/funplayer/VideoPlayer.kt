package com.example.funplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
internal fun VideoPlayerEmbed(
    videoUri: String?,
    modifier: Modifier = Modifier,
    onVideoSizeKnown: ((width: Int, height: Int) -> Unit)? = null,
    onPlaybackPosition: ((positionMs: Long) -> Unit)? = null
) {
    val context = LocalContext.current

    if (videoUri == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_play_uri),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        return
    }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            if (videoUri.startsWith("smb:", ignoreCase = true)) {
                val mediaItem = MediaItem.fromUri(android.net.Uri.parse(videoUri))
                val mediaSource = ProgressiveMediaSource.Factory(SmbDataSource.Factory())
                    .createMediaSource(mediaItem)
                setMediaSource(mediaSource)
            } else {
                setMediaItem(MediaItem.fromUri(android.net.Uri.parse(videoUri)))
            }
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(videoUri) {
        onDispose {
            exoPlayer.release()
        }
    }

    if (onVideoSizeKnown != null) {
        DisposableEffect(exoPlayer, onVideoSizeKnown) {
            val listener = object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        onVideoSizeKnown(videoSize.width, videoSize.height)
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose { exoPlayer.removeListener(listener) }
        }
    }

    onPlaybackPosition?.let { callback ->
        LaunchedEffect(exoPlayer) {
            while (true) {
                delay(100)
                callback(exoPlayer.currentPosition)
            }
        }
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                controllerShowTimeoutMs = 3000
            }
        }
    )
}
