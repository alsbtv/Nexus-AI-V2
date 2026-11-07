package com.nexus.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class TrackInfo(val title: String, val streamUrl: String)

class MusicRepository {
    private val client = OkHttpClient()
    suspend fun getAudiusTrending(): List<TrackInfo> = withContext(Dispatchers.IO) {
        val url = "https://discoveryprovider.audius.co/v1/tracks/trending?app_name=NexusAI"
        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            val dataArray = json.optJSONArray("data") ?: return@withContext emptyList()
            val tracks = mutableListOf<TrackInfo>()
            for (i in 0 until dataArray.length()) {
                val track = dataArray.getJSONObject(i)
                tracks.add(TrackInfo(track.optString("title"), "https://discoveryprovider.audius.co/v1/tracks/${track.optString("id")}/stream?app_name=NexusAI"))
            }
            tracks
        } catch (e: Exception) { emptyList() }
    }
}
