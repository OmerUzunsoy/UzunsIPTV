package com.uzuns.uzunsiptv

object ChannelManager {
    var channelList: List<LiveStream> = emptyList()
    var currentPosition: Int = 0
    var categoryList: List<LiveCategory> = emptyList()

    fun getCurrentChannel(): LiveStream? {
        if (channelList.isNotEmpty() && currentPosition in channelList.indices) {
            return channelList[currentPosition]
        }
        return null
    }

    fun nextChannel(): LiveStream? {
        if (channelList.isEmpty()) return null
        currentPosition++
        if (currentPosition >= channelList.size) {
            currentPosition = 0 // Listenin sonuysa başa dön
        }
        return channelList[currentPosition]
    }

    fun previousChannel(): LiveStream? {
        if (channelList.isEmpty()) return null
        currentPosition--
        if (currentPosition < 0) {
            currentPosition = channelList.size - 1 // Listenin başıysa sona dön
        }
        return channelList[currentPosition]
    }

    fun getChannelById(streamId: Int): LiveStream? {
        if (channelList.isEmpty()) return null
        val idx = channelList.indexOfFirst { it.streamId == streamId }
        return if (idx >= 0) {
            currentPosition = idx
            channelList[idx]
        } else null
    }
}
