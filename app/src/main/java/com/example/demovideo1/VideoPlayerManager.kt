package com.example.demovideo1

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.util.LinkedList

@UnstableApi
class VideoPlayerManager(
    private val context: Context,
    private val poolSize: Int = 5
) {
    private val playerPool = LinkedList<ExoPlayer>()
    private val activePlayersMap = mutableMapOf<Int, ExoPlayer>()
    private val playerToIndexMap = mutableMapOf<ExoPlayer, Int>()

    // Track trạng thái prepare để tránh prepare lại
    private val preparedIndices = mutableSetOf<Int>()

    init {
        repeat(poolSize) {
            val player = createNewPlayer()
            playerPool.add(player)
            android.util.Log.d("VideoPlayerManager", "Created player ${it + 1}/$poolSize")
        }
    }

    private fun createNewPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    VideoCache.getDataSourceFactory(context)
                )
            )
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                // Tối ưu buffer để load nhanh hơn
                setVideoScalingMode(androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            }
    }

    /**
     * Preload videos xung quanh currentIndex
     */
    fun preloadAround(currentIndex: Int, videoUrls: List<String>) {
        // Preload video trước (-1) và 2 video sau (+1, +2)
        val indicesToPreload = listOf(
            currentIndex - 1,  // Video trước
            currentIndex + 1,  // Video tiếp theo (quan trọng nhất!)
            currentIndex + 2   // Video tiếp theo nữa
        ).filter { it in videoUrls.indices && !preparedIndices.contains(it) }

        indicesToPreload.forEach { index ->
            preparePlayerForIndex(index, videoUrls[index])
        }
    }

    /**
     * Prepare player trước mà không hiển thị
     */
    private fun preparePlayerForIndex(index: Int, videoUrl: String) {
        if (preparedIndices.contains(index)) return

        val player = if (playerPool.isNotEmpty()) {
            playerPool.removeFirst()
        } else {
            recycleOldestPlayer(currentIndex = index)
        }

        player.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare() // Prepare ngay để buffer sẵn
            playWhenReady = false
        }

        activePlayersMap[index] = player
        playerToIndexMap[player] = index
        preparedIndices.add(index)

        android.util.Log.d("VideoPlayerManager", "Preloaded index $index")
    }

    fun getOrCreatePlayer(index: Int, videoUrl: String): ExoPlayer {
        // Nếu đã có và đã prepare, trả về ngay
        activePlayersMap[index]?.let {
            android.util.Log.d("VideoPlayerManager", "Using cached player for $index")
            return it
        }

        // Nếu chưa có, tạo mới
        val player = if (playerPool.isNotEmpty()) {
            playerPool.removeFirst()
        } else {
            recycleOldestPlayer(currentIndex = index)
        }

        player.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = false
        }

        activePlayersMap[index] = player
        playerToIndexMap[player] = index
        preparedIndices.add(index)

        return player
    }

    private fun recycleOldestPlayer(currentIndex: Int): ExoPlayer {
        // Tìm player xa currentIndex nhất để recycle
        val farthestEntry = activePlayersMap.entries
            .filter { it.key != currentIndex } // Không recycle player đang phát
            .maxByOrNull { kotlin.math.abs(it.key - currentIndex) }

        return if (farthestEntry != null) {
            val player = farthestEntry.value
            activePlayersMap.remove(farthestEntry.key)
            playerToIndexMap.remove(player)
            preparedIndices.remove(farthestEntry.key)
            android.util.Log.d("VideoPlayerManager", "Recycled player from index ${farthestEntry.key}")
            player
        } else {
            createNewPlayer()
        }
    }

    fun releasePlayer(index: Int) {
        activePlayersMap[index]?.let { player ->
            player.stop()
            player.clearMediaItems()
            player.playWhenReady = false

            if (playerPool.size < poolSize) {
                playerPool.add(player)
            } else {
                player.release()
            }

            activePlayersMap.remove(index)
            playerToIndexMap.remove(player)
            preparedIndices.remove(index)
        }
    }

    fun releaseAllPlayers() {
        activePlayersMap.values.forEach { it.release() }
        activePlayersMap.clear()
        playerToIndexMap.clear()
        preparedIndices.clear()

        playerPool.forEach { it.release() }
        playerPool.clear()

        android.util.Log.d("VideoPlayerManager", "Released all")
    }

    fun pauseAllExcept(currentIndex: Int) {
        activePlayersMap.forEach { (index, player) ->
            if (index != currentIndex) {
                player.pause()
                player.playWhenReady = false
            }
        }
    }

    fun cleanupDistantPlayers(currentIndex: Int, keepRange: Int = 3) {
        val toRemove = activePlayersMap.keys.filter { index ->
            kotlin.math.abs(index - currentIndex) > keepRange
        }

        toRemove.forEach { index ->
            releasePlayer(index)
        }

        if (toRemove.isNotEmpty()) {
            android.util.Log.d("VideoPlayerManager", "Cleaned ${toRemove.size} distant players")
        }
    }

    fun getPoolStats(): String {
        return "Pool: ${playerPool.size}/$poolSize | Active: ${activePlayersMap.size} | Prepared: ${preparedIndices.size}"
    }
}