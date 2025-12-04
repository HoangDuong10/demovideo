package com.example.demovideo1

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlin.compareTo
import kotlin.random.Random

class MainViewModel(
    private val context: Context
) : ViewModel() {

    private val _videoUrls = MutableStateFlow<List<String>>(emptyList())
    val videoUrls = _videoUrls.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying = _isVideoPlaying.asStateFlow()

    private val _isShowAction = MutableStateFlow(false)
    val isShowAction = _isShowAction.asStateFlow()

    private val _progressConfig = MutableStateFlow(
        ProgressConfig(
            action = LookBackConstants.RESET,
            configValue = Random.nextInt()
        )
    )
    val progressConfig = _progressConfig.asStateFlow()

    private val _videoConfig = MutableStateFlow(VideoConfig())
    val videoConfig = _videoConfig.asStateFlow()

    private val _durationProgressBar = MutableStateFlow(0L)
    val durationProgressBar = _durationProgressBar.asStateFlow()

    private val _index = MutableStateFlow(0)
    val index = _index.asStateFlow()

    private val _stateVideo = MutableStateFlow(LookBackConstants.INIT)
    val stateVideo = _stateVideo.asStateFlow()

    private var isTapScreen = false

    fun updateCurrentIndex(index: Int) {
        _currentIndex.value = index
        _index.value = index
        android.util.Log.d("MainViewModel", "Updated index to: $index")
    }
    
    // Chuyển sang video tiếp theo hoặc video trước (giữ lại cho tương thích)
    fun updateVideoView(isNext: Boolean) {
        val currentIdx = _currentIndex.value
        if (_videoUrls.value.isEmpty()) {
            return
        }
        
        if (isNext) {
            if (currentIdx < _videoUrls.value.size - 1) {
                _currentIndex.value = currentIdx + 1
            }
        } else {
            if (currentIdx > 0) {
                _currentIndex.value = currentIdx - 1
            }
        }
    }

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    fun setupPreloadView() {
        val currentIdx = _currentIndex.value
        val urls = _videoUrls.value
        
        // Preload 2 video tiếp theo
        val startIndex = currentIdx + 1
        if (startIndex < urls.size) {
            preloadVideos(
                startIndex,
                size = 2,
                videoUrls = urls
            )
        }
    }

    private fun timeDisplayText(): Int? {
        return when (index.value) {
            10 -> {
                1800
            }

            11 -> {
                1700
            }

            else -> {
                null
            }
        }
    }

    @ExperimentalCoroutinesApi
    @OptIn(UnstableApi::class)
    fun preloadVideos(
        index: Int,
        size: Int = 3,
        videoUrls: List<String>,
    ) {
        flowOf(
            videoUrls
        )
            .filter {
                videoUrls.size > index
            }
            .map {
                videoUrls.subList(
                    index,
                    minOf(index + size, videoUrls.size)
                )
            }.flatMapLatest {
                val dataSourceFactory = VideoCache.getDataSourceFactory(context)
                context.preloadVideos(
                    it,
                    dataSourceFactory
                )
            }
            .launchIn(viewModelScope)
    }

    fun resumeVideo(
        isResume: Boolean
    ) {
        _isVideoPlaying.value = isResume
    }

    fun updateProgressConfig(
        isResume: Boolean
    ) {
        if (!isTapScreen) {
            resumeVideo(isResume)
            if (isResume) {
                _progressConfig.value = progressConfig.value.resume()
            } else {
                _progressConfig.value = progressConfig.value.pause()
            }
        } else {
            _progressConfig.value = progressConfig.value.pause()
            resumeVideo(false)
        }
    }

    fun updateDuration(
        duration: Long
    ) {
        if (duration != 0L) {
            // Để 10 giây
            _durationProgressBar.value = 10000
//            _durationProgressBar.value = duration + index.value.toTimeDelay()
        }
    }

    fun updateStatePlayer(
        state: Int
    ) {
        _stateVideo.value = state
    }

    fun updateShowAction(
        isShow: Boolean
    ) {
        _isShowAction.value = isShow
    }

    fun updateTapAction(
        isTap: Boolean
    ) {
        isTapScreen = isTap
    }

    fun initData(listUrl: List<String>) {
        _videoUrls.value = listUrl
        _currentIndex.value = 0
        _progressConfig.value = progressConfig.value.copy(
            action = LookBackConstants.RESET,
        )
        _videoConfig.value = videoConfig.value.copy(
            videoUrl = if (listUrl.isNotEmpty()) listUrl[0] else "",
            time = timeDisplayText()
        )
        _isVideoPlaying.value = true
        android.util.Log.d("MainViewModel", "Initialized with ${listUrl.size} videos")
    }

}

fun Context.preloadVideos(
    videoUrls: List<String>,
    dataSourceFactory: DataSource.Factory
): Flow<Pair<ExoPlayer, String>> {
    return videoUrls
        .asFlow()
        .flatMapConcat { videoUrl ->
            callbackFlow {
                val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                val preloadPlayer = ExoPlayer.Builder(this@preloadVideos)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setLoadControl(DefaultLoadControl.Builder().build())
                    .build()
                val mediaItem = MediaItem.fromUri(videoUrl)
                preloadPlayer.setMediaItem(mediaItem)

                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == ExoPlayer.STATE_READY) {
                            trySend(Pair(preloadPlayer, videoUrl))
                            preloadPlayer.release()
                            close()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        preloadPlayer.release()
                        close(error)
                    }
                }

                preloadPlayer.addListener(listener)
                preloadPlayer.prepare()

                awaitClose {
                    preloadPlayer.removeListener(listener)
                    preloadPlayer.release()
                }
            }
        }
}
