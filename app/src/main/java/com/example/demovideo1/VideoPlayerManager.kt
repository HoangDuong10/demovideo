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
    private val poolSize: Int = 5 // Pool có 5 ExoPlayer
) {
    // Pool các ExoPlayer sẵn sàng để dùng
    private val playerPool = LinkedList<ExoPlayer>()
    
    // Map video index -> ExoPlayer đang dùng
    private val activePlayersMap = mutableMapOf<Int, ExoPlayer>()
    
    // Map ExoPlayer -> video index để track
    private val playerToIndexMap = mutableMapOf<ExoPlayer, Int>()
    
    init {
        // Tạo sẵn pool ExoPlayer
        repeat(poolSize) {
            val player = createNewPlayer()
            playerPool.add(player)
            android.util.Log.d("VideoPlayerManager", "Created player ${it + 1}/$poolSize in pool")
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
            }
    }
    
    fun getOrCreatePlayer(index: Int, videoUrl: String): ExoPlayer {
        // Nếu đã có player cho index này, trả về luôn
        activePlayersMap[index]?.let {
            android.util.Log.d("VideoPlayerManager", "Reusing existing player for index $index")
            return it
        }
        
        // Lấy player từ pool hoặc tái sử dụng player cũ nhất
        val player = if (playerPool.isNotEmpty()) {
            playerPool.removeFirst().also {
                android.util.Log.d("VideoPlayerManager", "Got player from pool for index $index")
            }
        } else {
            // Pool hết, tái sử dụng player cũ nhất (LRU)
            recycleOldestPlayer().also {
                android.util.Log.d("VideoPlayerManager", "Recycled oldest player for index $index")
            }
        }
        
        // Cấu hình player với video mới
        player.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = false
        }
        
        // Lưu vào map
        activePlayersMap[index] = player
        playerToIndexMap[player] = index
        
        android.util.Log.d("VideoPlayerManager", "Assigned player to index $index, active: ${activePlayersMap.size}")
        return player
    }
    
    private fun recycleOldestPlayer(): ExoPlayer {
        // Tìm player cũ nhất (index nhỏ nhất hoặc xa currentIndex nhất)
        val oldestEntry = activePlayersMap.entries.firstOrNull()
        return if (oldestEntry != null) {
            val player = oldestEntry.value
            activePlayersMap.remove(oldestEntry.key)
            playerToIndexMap.remove(player)
            android.util.Log.d("VideoPlayerManager", "Recycled player from index ${oldestEntry.key}")
            player
        } else {
            // Không có player nào, tạo mới
            createNewPlayer()
        }
    }
    
    fun releasePlayer(index: Int) {
        activePlayersMap[index]?.let { player ->
            player.stop()
            player.clearMediaItems()
            player.playWhenReady = false
            
            // Trả player về pool nếu pool chưa đầy
            if (playerPool.size < poolSize) {
                playerPool.add(player)
                android.util.Log.d("VideoPlayerManager", "Returned player to pool from index $index")
            } else {
                player.release()
                android.util.Log.d("VideoPlayerManager", "Released player from index $index")
            }
            
            activePlayersMap.remove(index)
            playerToIndexMap.remove(player)
        }
    }
    
    fun releaseAllPlayers() {
        // Release tất cả active players
        activePlayersMap.values.forEach { it.release() }
        activePlayersMap.clear()
        playerToIndexMap.clear()
        
        // Release pool
        playerPool.forEach { it.release() }
        playerPool.clear()
        
        android.util.Log.d("VideoPlayerManager", "Released all players and pool")
    }
    
    fun pauseAllExcept(currentIndex: Int) {
        activePlayersMap.forEach { (index, player) ->
            if (index != currentIndex) {
                player.pause()
                player.playWhenReady = false
            }
        }
    }
    
    fun cleanupDistantPlayers(currentIndex: Int, keepRange: Int = 2) {
        // Giải phóng các player quá xa currentIndex để tiết kiệm bộ nhớ
        val toRemove = activePlayersMap.keys.filter { index ->
            kotlin.math.abs(index - currentIndex) > keepRange
        }
        
        toRemove.forEach { index ->
            releasePlayer(index)
        }
        
        if (toRemove.isNotEmpty()) {
            android.util.Log.d("VideoPlayerManager", "Cleaned up ${toRemove.size} distant players")
        }
    }
    
    fun getPoolStats(): String {
        return "Pool: ${playerPool.size}/$poolSize, Active: ${activePlayersMap.size}"
    }
}
