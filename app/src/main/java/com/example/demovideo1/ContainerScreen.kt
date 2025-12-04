package com.example.demovideo1

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
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

    // Preload video đầu tiên khi khởi tạo
    LaunchedEffect(videoUrls) {
        if (videoUrls.isNotEmpty()) {
            playerManager.preloadAround(0, videoUrls)
        }
    }

    // Theo dõi thay đổi trang và preload
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                android.util.Log.d("ContainerScreen", "Page: $page - ${playerManager.getPoolStats()}")

                viewModel.updateCurrentIndex(page)

                // Pause tất cả trừ page hiện tại
                playerManager.pauseAllExcept(page)

                // Preload videos xung quanh (quan trọng!)
                playerManager.preloadAround(page, videoUrls)

                // Cleanup sau khi preload (keepRange = 3 để giữ đủ buffer)
                playerManager.cleanupDistantPlayers(page, keepRange = 3)
            }
    }

    // Xử lý settling state để preload sớm hơn
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                // Khi user bắt đầu vuốt, preload luôn
                if (settledPage != pagerState.currentPage) {
                    playerManager.preloadAround(settledPage, videoUrls)
                }
            }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    playerManager.pauseAllExcept(-1)
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Resume video hiện tại nếu cần
                    if (videoUrls.isNotEmpty()) {
                        playerManager.preloadAround(pagerState.currentPage, videoUrls)
                    }
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
            modifier = Modifier.fillMaxSize(),
            // Thêm beyondBoundsPageCount để Compose render sẵn pages xung quanh
            // Render sẵn 1 page trước/sau
        ) { page ->
            VideoItem(
                videoUrl = videoUrls[page],
                index = page,
                isCurrentPage = page == pagerState.currentPage,
                playerManager = playerManager
            )
        }

        // Debug overlay
        DebugOverlay(
            currentIndex = pagerState.currentPage,
            totalVideos = videoUrls.size,
            poolStats = playerManager.getPoolStats(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}