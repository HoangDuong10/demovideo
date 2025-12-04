package com.example.demovideo1

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerView(
    modifier: Modifier = Modifier,
    context: Context,
    exoPlayer: ExoPlayer,
    videoConfig: VideoConfig,
    isPlayVideo: Boolean,
    onDuration: (Long) -> Unit = {},   // Thời lương video
    onAction: () -> Unit = {},   // Action khi video chạy tới giây đã set trước
    onVideoFullyLoaded: () -> Unit = {},  // Video đã preload hết
    onStateVideo: (Int) -> Unit = {},  // State: Bắt đầu phát, loading, phát xong
    onUpdateView: (Boolean) -> Unit = {}
) {
    var isFullyLoaded by remember() {
        mutableStateOf(false)
    }
    var isGetCurrentPosition by remember() {
        mutableStateOf(true)
    }
    var isGetDuration by remember() {
        mutableStateOf(true)
    }
    var isEndVideo by remember() {
        mutableStateOf(false)
    }
    val playerListener = rememberUpdatedState { bufferedPercentage: Int ->
        if (bufferedPercentage == 100 && !isFullyLoaded) {
            isFullyLoaded = true
            onVideoFullyLoaded()
            Log.d("duonghx","aaaaaa")
        }
    }
    LaunchedEffect(videoConfig) {
        exoPlayer.apply {
            if (videoConfig.videoUrl.isNotEmpty()) {
                android.util.Log.d("VideoPlayerView", "Loading video: ${videoConfig.videoUrl}")
                isFullyLoaded = false
                isGetCurrentPosition = true
                isGetDuration = true
                isEndVideo = false
                stop()
                clearMediaItems()
                setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(videoConfig.videoUrl)))
                prepare()
                playWhenReady = true
                android.util.Log.d("VideoPlayerView", "Video prepared and ready to play")
            }
        }
        while (videoConfig.time != null && isGetCurrentPosition) {
            val currentPosition = exoPlayer.currentPosition
            if (currentPosition > videoConfig.time) {
//                Log.d("VideoPlayerView", "onAction: " + videoConfig.videoUrl)
                isGetCurrentPosition = false
                onAction.invoke()
            }
            delay(500)
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        if (!isEndVideo) {
                            onStateVideo.invoke(LookBackConstants.READY)
//                            Log.d("VideoPlayerView10", "READY")
                        }
                        if (isGetDuration) {
                            onDuration.invoke(exoPlayer.duration)
                            isGetDuration = false
                        }
                    }

                    Player.STATE_ENDED -> {
                        exoPlayer.seekTo(exoPlayer.duration)
                        if (!isEndVideo) {
//                            Log.d("VideoPlayerView11", "STATE_BUFFERING")
                            isEndVideo = true
//                            Log.d("VideoPlayerView33", "STATE_ENDED: " + videoConfig.videoUrl)
                            onStateVideo.invoke(LookBackConstants.END)
                        }
                    }

                    Player.STATE_BUFFERING -> {
                        if (!isEndVideo) {
//                            Log.d("VideoPlayerView12", "STATE_BUFFERING")
                            onStateVideo.invoke(LookBackConstants.LOADING)
                        }
                    }

                    Player.STATE_IDLE -> {

                    }
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                    val bufferedPercentage = player.bufferedPercentage
                    playerListener.value(bufferedPercentage)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
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
        update = {
            onUpdateView.invoke(isPlayVideo)
            if (isPlayVideo) {
                exoPlayer.playWhenReady = true
                it.onResume()
            } else {
                it.player = exoPlayer
                it.onPause()
                it.player?.pause()
                it.player?.playWhenReady = false
            }
        },
        modifier = modifier
    )
}
