package com.uzuns.uzunsiptv

object EpisodeManager {
    private var episodes: List<SeriesEpisode> = emptyList()
    private var currentIndex: Int = 0

    fun setEpisodes(list: List<SeriesEpisode>, currentId: String) {
        episodes = list
        currentIndex = episodes.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
    }

    fun getCurrent(): SeriesEpisode? {
        return episodes.getOrNull(currentIndex)
    }

    fun next(): SeriesEpisode? {
        if (episodes.isEmpty()) return null
        currentIndex = (currentIndex + 1) % episodes.size
        return episodes[currentIndex]
    }

    fun previous(): SeriesEpisode? {
        if (episodes.isEmpty()) return null
        currentIndex = if (currentIndex - 1 < 0) episodes.lastIndex else currentIndex - 1
        return episodes[currentIndex]
    }
}
