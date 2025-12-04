package com.example.demovideo1

data class ProgressConfig(
    val action: String = LookBackConstants.RESET,
    val configValue: Int = 0
) {
    fun pause(): ProgressConfig = copy(action = LookBackConstants.PAUSE)
    fun resume(): ProgressConfig = copy(action = LookBackConstants.RESUME)
    fun reset(): ProgressConfig = copy(action = LookBackConstants.RESET, configValue = configValue + 1)
}
