package com.nexus.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long,
    val isPinned: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val text: String,
    val isUser: Boolean,
    val attachmentUri: String? = null,
    val timestamp: Long,
    val editHistory: String = "",
    val isImage: Boolean = false,
    val isVideo: Boolean = false,
    val videoUri: String? = null
)

@Entity(tableName = "pinned_messages")
data class PinnedMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val messageId: String,
    val pinName: String,
    val timestamp: Long
)
