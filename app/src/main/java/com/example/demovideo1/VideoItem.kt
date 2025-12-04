package com.example.demovideo1

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@UnstableApi
@Composable
fun VideoItem(
    videoUrl: String,
    index: Int,
    isCurrentPage: Boolean,
    playerManager: VideoPlayerManager
) {
    val context = LocalContext.current
    val exoPlayer = playerManager.getOrCreatePlayer(index, videoUrl)
    
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            android.util.Log.d("VideoItem", "Playing video at index $index")
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            android.util.Log.d("VideoItem", "Pausing video at index $index")
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Không release ngay, để cache lại
        }
    }
    
    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(Color.BLACK)
                setKeepScreenOn(true)
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        modifier = Modifier.fillMaxSize()
    )
}
