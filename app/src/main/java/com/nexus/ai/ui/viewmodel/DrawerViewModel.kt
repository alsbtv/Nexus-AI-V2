package com.nexus.ai.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.ai.data.AppDatabase
import com.nexus.ai.data.ChatEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrawerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()

    val chats = chatDao.getAllChats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    var isMultiSelectMode by mutableStateOf(false)
    val selectedChats = mutableStateListOf<String>()

    fun toggleMultiSelect(chatId: String) {
        if (selectedChats.contains(chatId)) selectedChats.remove(chatId) else selectedChats.add(chatId)
        isMultiSelectMode = selectedChats.isNotEmpty()
    }

    fun exitMultiSelect() {
        selectedChats.clear()
        isMultiSelectMode = false
    }

    fun deleteChat(id: String) {
        viewModelScope.launch {
            chatDao.deleteChatById(id)
            db.messageDao().deleteMessagesForChat(id)
            db.pinnedMessageDao().deletePinsForChat(id)
        }
    }

    fun renameChat(id: String, newTitle: String) {
        viewModelScope.launch {
            val existing = chats.value.find { it.id == id }
            if (existing != null) {
                chatDao.insertChat(existing.copy(title = newTitle))
            }
        }
    }
    
    fun addChat(id: String, title: String) {
        viewModelScope.launch {
            chatDao.insertChat(ChatEntity(id = id, title = title, timestamp = System.currentTimeMillis()))
        }
    }
}
