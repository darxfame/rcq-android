package com.rcq.messenger.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.rcq.messenger.domain.model.ChatEntity

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats")
    suspend fun getAllChatRows(): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChat(id: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE targetId = :targetId LIMIT 1")
    suspend fun getChatByTargetId(targetId: Long): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChat(id: String)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun incrementUnreadCount(chatId: String, timestamp: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun clearUnreadCount(chatId: String)

    @Query("UPDATE chats SET isPinned = :pinned WHERE id = :chatId")
    suspend fun setPinned(chatId: String, pinned: Boolean)

    @Query("UPDATE chats SET isMuted = :muted WHERE id = :chatId")
    suspend fun setMuted(chatId: String, muted: Boolean)

    @Query("UPDATE chats SET isArchived = :archived WHERE id = :chatId")
    suspend fun setArchived(chatId: String, archived: Boolean)

    @Query("UPDATE chats SET updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateTimestamp(chatId: String, timestamp: Long)

    @Query("DELETE FROM chats")
    suspend fun clearAll()
}
