package com.rcq.messenger.data.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rcq.messenger.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [
        UserEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        GroupEntity::class,
        StoryEntity::class,
        StoryItemEntity::class,
        CallEntity::class,
        PetEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class RCQDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao
    abstract fun storyDao(): StoryDao
    abstract fun callDao(): CallDao
    abstract fun petDao(): PetDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add Signal Protocol E2EE fields to messages table
                database.execSQL("ALTER TABLE messages ADD COLUMN ciphertext TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN signalType INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE messages ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

class RoomTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromMapLongString(value: Map<Long, String>): String = json.encodeToString(value)

    @TypeConverter
    fun toMapLongString(value: String): Map<Long, String> =
        try { json.decodeFromString(value) } catch (e: Exception) { emptyMap() }

    @TypeConverter
    fun fromLongList(value: List<Long>): String = json.encodeToString(value)

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        try { json.decodeFromString(value) } catch (e: Exception) { emptyList() }
}

// User Entity
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val nickname: String,
    val avatarUrl: String?,
    val status: String,
    val lastSeen: String?,
    val bio: String,
    val isBlocked: Boolean,
    val isFavorite: Boolean,
    val notificationSound: String?,
    val customNickname: String?,
    val tokens: Long,
    val isPremium: Boolean
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: Long): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

// Contact Entity
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val status: String = "offline",
    val lastSeen: String? = null,
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val notificationSound: String? = null,
    val customNickname: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTime: Long = 0
)

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE isBlocked = 0 ORDER BY nickname ASC")
    fun getContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isBlocked = 1")
    fun getBlockedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE userId = :userId")
    suspend fun getContactByUserId(userId: Long): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Long)

    @Query("DELETE FROM contacts")
    suspend fun clearAll()

    @Query("UPDATE contacts SET isBlocked = 1 WHERE userId = :userId")
    suspend fun blockContact(userId: Long)
}

// Chat Entity
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val targetId: Long,
    val targetNickname: String,
    val targetAvatar: String?,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChat(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChat(id: String)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun incrementUnreadCount(chatId: String, timestamp: Long)

    @Query("UPDATE chats SET updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateTimestamp(chatId: String, timestamp: Long)
}

// Message Entity — aligned with iOS Message model + Signal Protocol fields
@Entity(
    tableName = "messages",
    indices = [Index("chatId"), Index("timestamp")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: Long,
    val isFromMe: Boolean = false,
    val kind: String = "TEXT",
    val content: String = "",
    val mediaId: String? = null,
    val timestamp: Long = 0,
    val status: String = "SENT",
    val receivedWhileAway: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val reactions: String? = null,
    val thumbnailB64: String? = null,
    val durationSec: Double = 0.0,
    val ttlSeconds: Int? = null,
    val forwardedFromName: String? = null,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToAuthorName: String? = null,
    val editedAt: Long? = null,
    val premiumPriceTokens: Int? = null,
    val premiumUnlocked: Boolean = false,
    val albumId: String? = null,
    val fileName: String? = null,
    val fileMime: String? = null,
    val fileSizeBytes: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pollId: String? = null,
    val isForwarded: Boolean = false,
    val mentionedUserIds: String? = null,
    // Signal Protocol E2EE fields
    val ciphertext: String? = null, // Base64-encoded encrypted content
    val signalType: Int = 1, // Signal Protocol message type (1=PreKey, 2=Whisper)
    val isEncrypted: Boolean = false // Flag to indicate if message is encrypted
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getMessages(chatId: String, limit: Int = 50, offset: Int = 0): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND timestamp < :before ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMessagesBefore(chatId: String, before: Long, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessage(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteChatMessages(chatId: String)
}

// Group Entity
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String?,
    val description: String,
    val ownerId: Long,
    val memberCount: Int,
    val createdAt: Long,
    val isPinned: Boolean,
    val isMuted: Boolean
)

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroup(id: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)
}

// Story Entity
@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val userId: Long,
    val nickname: String,
    val avatarUrl: String?,
    val viewerCount: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean
)

@Entity(
    tableName = "story_items",
    foreignKeys = [ForeignKey(
        entity = StoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["storyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("storyId")]
)
data class StoryItemEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val type: String,
    val mediaUrl: String,
    val thumbnailUrl: String?,
    val caption: String?,
    val backgroundColor: String?,
    val duration: Int,
    val createdAt: Long
)

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM story_items WHERE storyId = :storyId ORDER BY createdAt ASC")
    fun getStoryItems(storyId: String): Flow<List<StoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryItems(items: List<StoryItemEntity>)

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteStory(id: String)
}

// Call Entity
@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val id: String,
    val type: String,
    val targetId: Long,
    val targetNickname: String,
    val targetAvatar: String?,
    val initiatorId: Long,
    val status: String,
    val startedAt: Long?,
    val endedAt: Long?,
    val duration: Long
)

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY startedAt DESC LIMIT :limit")
    fun getCalls(limit: Int = 50): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE status = 'MISSED' ORDER BY startedAt DESC")
    fun getMissedCalls(): Flow<List<CallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)

    @Update
    suspend fun updateCall(call: CallEntity)
}

// Pet Entity
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val rarity: String,
    val imageUrl: String,
    val equippedBy: Long?,
    val isForSale: Boolean,
    val salePrice: Long?
)

@Dao
interface PetDao {
    @Query("SELECT * FROM pets WHERE equippedBy = :userId")
    fun getEquippedPets(userId: Long): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets")
    fun getAllPets(): Flow<List<PetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPets(pets: List<PetEntity>)

    @Query("UPDATE pets SET equippedBy = :userId WHERE id = :petId")
    suspend fun equipPet(petId: String, userId: Long)

    @Query("UPDATE pets SET equippedBy = NULL WHERE id = :petId")
    suspend fun unequipPet(petId: String)
}