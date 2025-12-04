package com.example.demovideo1

import android.content.Context
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object VideoCache {
    private var simpleCache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "video_cache")
            simpleCache = SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // Cache tối đa 100MB
            )
        }
        return simpleCache!!
    }

    fun getDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
        val defaultDataSourceFactory =
            DefaultDataSource.Factory(context, defaultHttpDataSourceFactory)
        return CacheDataSource.Factory().apply {
            setCache(cache)
            setUpstreamDataSourceFactory(defaultDataSourceFactory).setEventListener(object :
                CacheDataSource.EventListener {
                override fun onCacheIgnored(reason: Int) {
                    Log.d("Tesssss", "Cache ignored, reason: $reason")
                }

                override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
                    Log.d(
                        "Tesssss",
                        "Cached bytes read: $cachedBytesRead / Cache size: $cacheSizeBytes"
                    )
                }
            })
        }
    }
}
