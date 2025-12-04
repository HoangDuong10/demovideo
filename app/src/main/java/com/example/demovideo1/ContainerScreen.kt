package com.example.demovideo1

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(UnstableApi::class)
@Composable
fun LooBackContainerScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val videoUrls by viewModel.videoUrls.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val playerManager = remember { VideoPlayerManager(context) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { videoUrls.size }
    )
    
    // Theo dõi thay đổi trang
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                android.util.Log.d("ContainerScreen", "Current page: $page")
                viewModel.updateCurrentIndex(page)
                playerManager.pauseAllExcept(page)
            }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    playerManager.pauseAllExcept(-1) // Pause tất cả
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerManager.releaseAllPlayers()
        }
    }
    
    Box(Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            VideoItem(
                videoUrl = videoUrls[page],
                index = page,
                isCurrentPage = page == pagerState.currentPage,
                playerManager = playerManager
            )
        }

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
