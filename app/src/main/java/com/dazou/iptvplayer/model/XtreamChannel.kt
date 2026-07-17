package com.dazou.iptvplayer.model

data class XtreamChannel(
    val streamId: Int,
    val name: String,
    val type: String = "live",
    val icon: String = "",
    val categoryId: String = "",
    val streamUrl: String = "",
    val username: String = "",
    val password: String = "",
    val extension: String = "ts"
)