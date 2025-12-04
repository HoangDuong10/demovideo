package com.example.demovideo1

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

@UnstableApi
class VideoPlayerManager(private val context: Context) {
    private val players = mutableMapOf<Int, ExoPlayer>()
    
    fun getOrCreatePlayer(index: Int, videoUrl: String): ExoPlayer {
        return players.getOrPut(index) {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(
                        VideoCache.getDataSourceFactory(context)
                    )
                )
                .build().apply {
                    setMediaItem(MediaItem.fromUri(videoUrl))
                    prepare()
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = false
                    android.util.Log.d("VideoPlayerManager", "Created player for index $index")
                }
        }
    }
    
    fun releasePlayer(index: Int) {
        players[index]?.release()
        players.remove(index)
        android.util.Log.d("VideoPlayerManager", "Released player for index $index")
    }
    
    fun releaseAllPlayers() {
        players.values.forEach { it.release() }
        players.clear()
        android.util.Log.d("VideoPlayerManager", "Released all players")
    }
    
    fun pauseAllExcept(currentIndex: Int) {
        players.forEach { (index, player) ->
            if (index != currentIndex) {
                player.pause()
                player.playWhenReady = false
            }
        }
    }
}
