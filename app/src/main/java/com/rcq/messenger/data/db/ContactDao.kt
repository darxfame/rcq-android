package com.rcq.messenger.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.rcq.messenger.domain.model.ContactEntity

@Dao
interface ContactDao {
    // Только явно добавленные контакты (isContact = 1). Участники групп (isContact = 0)
    // хранятся в той же таблице для Signal E2EE, но в UI не появляются.
    @Query("SELECT * FROM contacts WHERE isContact = 1 AND isBlocked = 0 ORDER BY nickname ASC")
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

    @Query("UPDATE contacts SET customNickname = :nickname WHERE userId = :userId")
    suspend fun updateCustomNickname(userId: Long, nickname: String?)

    @Query("UPDATE contacts SET isFavorite = :isFavorite WHERE userId = :userId")
    suspend fun setFavorite(userId: Long, isFavorite: Boolean)

    @Query("UPDATE contacts SET signingKey = :signingKey WHERE userId = :userId")
    suspend fun updateSigningKey(userId: Long, signingKey: String)

    @Query("UPDATE contacts SET status = :status WHERE userId = :userId")
    suspend fun updateStatus(userId: Long, status: String)
}