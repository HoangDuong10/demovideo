package com.example.demovideo1

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.DEBUG) // chỉ dùng khi debug
            androidContext(this@MyApplication)
            modules(appModule) // hoặc list các module
        }
    }
}