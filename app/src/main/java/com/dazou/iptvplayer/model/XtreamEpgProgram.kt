package com.dazou.iptvplayer.model

data class XtreamEpgProgram(
    val title: String,
    val startTimestamp: Long,
    val stopTimestamp: Long,
    val nowPlaying: Boolean
) {
    fun progressPercent(): Int {
        val now = System.currentTimeMillis() / 1000
        if (now <= startTimestamp) return 0
        if (now >= stopTimestamp) return 100
        val total = (stopTimestamp - startTimestamp).toFloat()
        val elapsed = (now - startTimestamp).toFloat()
        if (total <= 0) return 0
        return ((elapsed / total) * 100).toInt().coerceIn(0, 100)
    }
}