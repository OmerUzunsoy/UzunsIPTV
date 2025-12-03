package com.uzuns.uzunsiptv

import com.google.gson.annotations.SerializedName

data class SeriesCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: String?
)

data class SeriesStream(
    @SerializedName("num") val num: Int,
    @SerializedName("name") val name: String,
    @SerializedName("series_id") val seriesId: Int,
    @SerializedName("cover") val cover: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("cast") val cast: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("last_modified") val lastModified: String?
)

data class SeriesInfoResponse(
    @SerializedName("info") val info: SeriesInfo,
    @SerializedName("episodes") val episodes: Map<String, List<SeriesEpisode>>
)

data class SeriesInfo(
    @SerializedName("name") val name: String,
    @SerializedName("cover") val cover: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("cast") val cast: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("rating") val rating: String?
)

data class SeriesEpisode(
    @SerializedName("id") val id: String,
    @SerializedName("episode_num") val episodeNum: Int,
    @SerializedName("title") val title: String,
    @SerializedName("container_extension") val containerExtension: String?,
    @SerializedName("info") val info: EpisodeInfo?
)

data class EpisodeInfo(
    @SerializedName("plot") val plot: String?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("movie_image") val movieImage: String?
)
