package com.nexus.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AnimeClip(val title: String, val previewUrl: String?)

class AnimeMediaRepository {
    private val client = OkHttpClient()
    suspend fun searchAnime(query: String): List<AnimeClip> = withContext(Dispatchers.IO) {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.jikan.moe/v4/anime?q=$encodedQuery&limit=5"
        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()
            val clips = mutableListOf<AnimeClip>()
            for (i in 0 until dataArray.length()) {
                val anime = dataArray.getJSONObject(i)
                clips.add(AnimeClip(anime.optString("title"), anime.optJSONObject("trailer")?.optString("url")))
            }
            clips
        } catch (e: Exception) { emptyList() }
    }
}
