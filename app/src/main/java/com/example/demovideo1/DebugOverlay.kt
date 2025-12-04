package com.example.demovideo1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DebugOverlay(
    currentIndex: Int,
    totalVideos: Int,
    poolStats: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(8.dp)
    ) {
        Text(
            text = "Video: ${currentIndex + 1}/$totalVideos\n$poolStats",
            color = Color.White,
            fontSize = 12.sp
        )
    }
}
