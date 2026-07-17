package com.dazou.iptvplayer.player

interface PlayerCallback {

    fun playStream(
        url: String,
        name: String,
        type: String
    )

    fun onNextChannel()

    fun onPreviousChannel()

}