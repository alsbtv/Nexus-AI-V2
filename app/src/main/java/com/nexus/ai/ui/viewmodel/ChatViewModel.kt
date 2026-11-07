package com.nexus.ai.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.ai.data.*
import com.nexus.ai.editor.FFmpegHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val messageDao = db.messageDao()
    private val chatDao = db.chatDao()
    private val pinDao = db.pinnedMessageDao()
    private val ffmpeg = FFmpegHelper()
    
    var currentChatId: String? = null
        private set
        
    var currentSkill by mutableStateOf("General")
    var currentThinkingLevel by mutableStateOf("Medium")
    var currentModel by mutableStateOf("Auto")
    
    var isGeneratingVideo by mutableStateOf(false)
    var pendingVideoUrl by mutableStateOf("")
    var pendingMusicUrl by mutableStateOf("")
    var webViewRef: android.webkit.WebView? = null

    fun startNewChat() {
        val newId = System.currentTimeMillis().toString()
        currentChatId = newId
        viewModelScope.launch {
            chatDao.insertChat(ChatEntity(id = newId, title = "محادثة جديدة", timestamp = System.currentTimeMillis()))
        }
    }

    fun loadChat(chatId: String) {
        currentChatId = chatId
    }

    fun getMessages(chatId: String) = messageDao.getMessagesForChat(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPins(chatId: String) = pinDao.getPinsForChat(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addUserMessage(text: String, attachmentUri: String? = null) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(
                id = System.currentTimeMillis().toString(),
                chatId = chatId, text = text, isUser = true,
                attachmentUri = attachmentUri, timestamp = System.currentTimeMillis()
            ))
            updateChatTitleIfNeeded(chatId, text)
        }
    }

    fun addAiMessage(text: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(
                id = System.currentTimeMillis().toString(),
                chatId = chatId, text = text, isUser = false, timestamp = System.currentTimeMillis()
            ))
        }
    }

    fun addImageMessage(imageUrl: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(
                id = System.currentTimeMillis().toString(),
                chatId = chatId, text = imageUrl, isUser = false,
                isImage = true, timestamp = System.currentTimeMillis()
            ))
        }
    }

    fun editMessage(messageId: String, currentText: String, newText: String, existingHistory: String) {
        viewModelScope.launch {
            val historyList = if (existingHistory.isBlank()) mutableListOf() else existingHistory.split("||").toMutableList()
            historyList.add(currentText)
            while (historyList.size > 5) { historyList.removeAt(0) }
            messageDao.updateMessage(messageId, newText, historyList.joinToString("||"))
        }
    }

    fun addPin(chatId: String, messageId: String, pinName: String) {
        viewModelScope.launch {
            pinDao.insertPin(PinnedMessageEntity(
                id = System.currentTimeMillis().toString(),
                chatId = chatId, messageId = messageId, pinName = pinName, timestamp = System.currentTimeMillis()
            ))
        }
    }

    fun generateMontage(prompt: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            isGeneratingVideo = true
            addUserMessage("طلب مونتاج: $prompt")
            val animeRepo = com.nexus.ai.data.AnimeMediaRepository()
            val musicRepo = com.nexus.ai.data.MusicRepository()

            val anime = animeRepo.searchAnime(prompt).firstOrNull()
            pendingVideoUrl = anime?.previewUrl ?: ""
            val music = musicRepo.getAudiusTrending().firstOrNull()
            pendingMusicUrl = music?.streamUrl ?: ""

            if (pendingVideoUrl.isNotBlank() && pendingMusicUrl.isNotBlank()) {
                webViewRef?.evaluateJavascript("analyzeVideoForMontage('$pendingVideoUrl')", null)
            } else {
                addAiMessage("عذراً، لم أجد المصادر.")
                isGeneratingVideo = false
            }
        }
    }

    fun onVideoAnalyzedReceived(clipsJson: String) {
        viewModelScope.launch {
            try {
                val jsonArray = org.json.JSONArray(clipsJson)
                val clips = mutableListOf<Pair<Int, Int>>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    clips.add(obj.getInt("start") to obj.getInt("end"))
                }
                val ffmpeg = com.nexus.ai.editor.FFmpegHelper()
                ffmpeg.createLongMontage(pendingVideoUrl, pendingMusicUrl, clips, getApplication()) { outputPath ->
                    if (outputPath != null) {
                        viewModelScope.launch {
                            messageDao.insertMessage(com.nexus.ai.data.MessageEntity(
                                id = System.currentTimeMillis().toString(),
                                chatId = currentChatId ?: "",
                                text = "تم إنشاء المونتاج الحماسي!",
                                isUser = false, isVideo = true, videoUri = outputPath,
                                timestamp = System.currentTimeMillis()
                            ))
                        }
                    }
                    isGeneratingVideo = false
                }
            } catch (e: Exception) { isGeneratingVideo = false }
        }
    }

    private suspend fun updateChatTitleIfNeeded(chatId: String, firstMessage: String) {
        val title = if (firstMessage.length > 20) firstMessage.take(20) + "..." else firstMessage
        chatDao.insertChat(ChatEntity(id = chatId, title = title, timestamp = System.currentTimeMillis()))
    }
}
