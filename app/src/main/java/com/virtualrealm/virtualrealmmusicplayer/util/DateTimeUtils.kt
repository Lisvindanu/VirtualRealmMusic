// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/DateTimeUtils.kt

package com.virtualrealm.virtualrealmmusicplayer.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%d:%02d", minutes, seconds)
    }

    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
}