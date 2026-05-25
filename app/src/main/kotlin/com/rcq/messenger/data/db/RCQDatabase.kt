package com.rcq.messenger.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rcq.messenger.domain.model.ChatEntity
import com.rcq.messenger.domain.model.ContactEntity
import com.rcq.messenger.domain.model.MessageEntity
import com.rcq.messenger.domain.model.UserEntity

/**
 * RCQ Database — Room database instance.
 * 
 * CRITICAL FIX (0.1):
 * - Updated to include new ContactEntity with fixed primary key
 * - Incremented version for migration support
 * 
 * Entities:
 * - UserEntity: cached user profiles
 * - ContactEntity: user's contacts (PK: userId)
 * - ChatEntity: conversations/threads
 * - MessageEntity: message history with reactions
 */
@Database(
    entities = [
        UserEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class RCQDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
