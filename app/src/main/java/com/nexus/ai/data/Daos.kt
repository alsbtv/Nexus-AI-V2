package com.nexus.ai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, timestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)
    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    @Query("UPDATE messages SET text = :newText, editHistory = :history WHERE id = :messageId")
    suspend fun updateMessage(messageId: String, newText: String, history: String)
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)
}

@Dao
interface PinnedMessageDao {
    @Query("SELECT * FROM pinned_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getPinsForChat(chatId: String): Flow<List<PinnedMessageEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: PinnedMessageEntity)
    @Query("DELETE FROM pinned_messages WHERE chatId = :chatId")
    suspend fun deletePinsForChat(chatId: String)
}
