package com.nexus.ai.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.ai.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val messageDao = db.messageDao()
    private val chatDao = db.chatDao()
    private val pinDao = db.pinnedMessageDao()
    
    var currentChatId by mutableStateOf<String?>(null)
        private set
        
    var currentSkill by mutableStateOf("General")
    var currentThinkingLevel by mutableStateOf("Medium")
    var currentModel by mutableStateOf("Auto")
    
    var isGeneratingVideo by mutableStateOf(false)
    var pendingVideoUrl by mutableStateOf("")
    var pendingMusicUrl by mutableStateOf("")

    fun startNewChat() {
        val newId = System.currentTimeMillis().toString()
        currentChatId = newId
        viewModelScope.launch {
            try {
                chatDao.insertChat(ChatEntity(id = newId, title = "محادثة جديدة", timestamp = System.currentTimeMillis()))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun loadChat(chatId: String) { currentChatId = chatId }

    fun clearAllChats() {
        viewModelScope.launch {
            try {
                db.clearAllTables()
                startNewChat()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun getMessages(chatId: String) = messageDao.getMessagesForChat(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPins(chatId: String) = pinDao.getPinsForChat(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addUserMessage(text: String, attachmentUri: String? = null) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            try {
                messageDao.insertMessage(MessageEntity(id = System.currentTimeMillis().toString(), chatId = chatId, text = text, isUser = true, attachmentUri = attachmentUri, timestamp = System.currentTimeMillis()))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addAiMessage(text: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            try {
                messageDao.insertMessage(MessageEntity(id = System.currentTimeMillis().toString(), chatId = chatId, text = text, isUser = false, timestamp = System.currentTimeMillis()))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addImageMessage(imageUrl: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            try {
                messageDao.insertMessage(MessageEntity(id = System.currentTimeMillis().toString(), chatId = chatId, text = imageUrl, isUser = false, isImage = true, timestamp = System.currentTimeMillis()))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun editMessage(messageId: String, currentText: String, newText: String, existingHistory: String) {
        viewModelScope.launch {
            try {
                val historyList = if (existingHistory.isBlank()) mutableListOf() else existingHistory.split("||").toMutableList()
                historyList.add(currentText)
                while (historyList.size > 5) { historyList.removeAt(0) }
                messageDao.updateMessage(messageId, newText, historyList.joinToString("||"))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addPin(chatId: String, messageId: String, pinName: String) {
        viewModelScope.launch {
            try {
                pinDao.insertPin(PinnedMessageEntity(id = System.currentTimeMillis().toString(), chatId = chatId, messageId = messageId, pinName = pinName, timestamp = System.currentTimeMillis()))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun generateMontage(prompt: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            isGeneratingVideo = true
            addUserMessage("طلب مونتاج: $prompt")
            isGeneratingVideo = false 
        }
    }
    
    fun onVideoAnalyzedReceived(clipsJson: String) { isGeneratingVideo = false }
}
