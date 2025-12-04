package com.example.demovideo1

import kotlin.random.Random

data class ProgressConfig(
    var action: String = LookBackConstants.RESET,
    var configValue: Int? = Random.nextInt()
)

fun ProgressConfig.pause(): ProgressConfig {
    return this.copy(
        action = LookBackConstants.PAUSE
    )
}

fun ProgressConfig.resume(): ProgressConfig {
    return this.copy(
        action = LookBackConstants.RESUME
    )
}

fun ProgressConfig.reset(): ProgressConfig {
    return this.copy(
        action = LookBackConstants.RESET,
        configValue = Random.nextInt()
    )
}
