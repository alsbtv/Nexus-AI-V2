package com.nexus.ai.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GitHubUpdateManager {
    private val client = OkHttpClient()

    suspend fun checkLatestRelease(): JSONObject? = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/alsbtv/Nexus-AI-V2/releases/latest"
        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) JSONObject(response.body?.string() ?: "{}") else null
        } catch (e: Exception) { null }
    }

    fun downloadAndInstallApk(context: Context, downloadUrl: String, versionName: String) {
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Nexus AI Update")
            .setDescription("Downloading version $versionName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "Nexus_AI_${versionName}.apk")
            .setMimeType("application/vnd.android.package-archive")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}
