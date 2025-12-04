package com.example.demovideo1

import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

@OptIn(UnstableApi::class)
@Composable
fun LooBackContainerScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    VideoCache.getDataSourceFactory(context)
                )
            )
            .build().apply {
                android.util.Log.d("ContainerScreen", "ExoPlayer created")
            }
    }
    val videoConfig by viewModel.videoConfig.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val isPlayingVideo by viewModel.isVideoPlaying.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(exoPlayer) {
        onDispose {
            android.util.Log.d("ContainerScreen", "Releasing ExoPlayer")
            exoPlayer.release()
        }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.resumeVideo(true)
                }

                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.resumeVideo(false)
                }

                Lifecycle.Event.ON_STOP -> {}
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDragY = 0f
                val swipeThreshold = 100f // Ngưỡng vuốt tối thiểu (pixels)
                
                detectDragGestures(
                    onDragStart = {
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        // Khi kết thúc vuốt, kiểm tra hướng và khoảng cách
                        when {
                            totalDragY < -swipeThreshold -> {
                                // Vuốt lên -> video tiếp theo
                                android.util.Log.d("ContainerScreen", "Vuốt lên - Video tiếp theo")
                                viewModel.updateVideoView(true)
                            }
                            totalDragY > swipeThreshold -> {
                                // Vuốt xuống -> video trước
                                android.util.Log.d("ContainerScreen", "Vuốt xuống - Video trước")
                                viewModel.updateVideoView(false)
                            }
                        }
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        totalDragY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalDragY += dragAmount.y
                }
            }
    ) {
        VideoPlayerView(
            context = context,
            modifier = Modifier.fillMaxSize(),
            exoPlayer = exoPlayer,
            videoConfig = videoConfig,
            isPlayVideo = isPlayingVideo,
            onStateVideo = {
                when (it) {
                    LookBackConstants.READY -> {
                        viewModel.updateProgressConfig(true)
                        viewModel.updateStatePlayer(LookBackConstants.READY)
                    }

                    LookBackConstants.LOADING -> {
                        viewModel.updateProgressConfig(false)
                        viewModel.updateStatePlayer(LookBackConstants.LOADING)
                    }

                    LookBackConstants.END -> {
                        viewModel.updateStatePlayer(LookBackConstants.END)
                    }
                }
            },
            onDuration = {
                viewModel.updateDuration(it)
            },
            onUpdateView = {
                viewModel.updateProgressConfig(it)
            },
            onVideoFullyLoaded = {
                viewModel.setupPreloadView()
            },
            onAction = {
//                viewModel.updateShowAction(true)
            }
        )

//        when (index) {
//            0 -> {
//                Screen1(
//                    dataNavigation = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            1 -> {
//                Screen2(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            2 -> {
//                Screen3(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            3 -> {
//                Screen4(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            4 -> {
//                Screen5(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            5 -> {
//                Screen6(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            6 -> {
//                Screen7(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            7 -> {
//                Screen9(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            8 -> {
//                Screen8(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            9 -> {
//                Screen10(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            10 -> {
//                Screen11(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//
//            11 -> {
//                Screen12(
//                    viewModel = viewModel,
//                    data = data
//                ) {
//                    onBackPressed.invoke()
//                }
//            }
//        }
//        SlicedProgressBar(
//            modifier = Modifier
//                .height(40.dp)
//                .padding(18.dp, 0.dp)
//                .fillMaxWidth(),
//            data.data?.gifLinks?.size ?: 0,
//            (index + 1),
//            progressConfig,
//            durationProgressBar.toInt(), {
//                viewModel.updateVideoView(true)
//            }
//        )
    }
}
