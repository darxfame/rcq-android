package com.rcq.messenger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rcq.messenger.domain.model.ContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contact Data Access Object.
 * 
 * CRITICAL FIX (0.1):
 * - insertAll() uses REPLACE strategy to handle updates on sync
 * - Primary key is now 'userId' (UIN) instead of auto-generated 'id'
 * - This prevents duplicate contacts when syncing from server
 */
@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY nickname ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE userId = :userId")
    suspend fun getContactByUserId(userId: Long): ContactEntity?
    
    @Query("SELECT * FROM contacts WHERE blocked = 0 ORDER BY nickname ASC")
    fun getActiveContacts(): Flow<List<ContactEntity>>
    
    @Query("SELECT * FROM contacts WHERE blocked = 1 ORDER BY nickname ASC")
    fun getBlockedContacts(): Flow<List<ContactEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)
    
    @Update
    suspend fun update(contact: ContactEntity)
    
    @Query("DELETE FROM contacts WHERE userId = :userId")
    suspend fun deleteContact(userId: Long)
    
    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
    
    @Query("UPDATE contacts SET blocked = 1 WHERE userId = :userId")
    suspend fun blockContact(userId: Long)
    
    @Query("UPDATE contacts SET blocked = 0 WHERE userId = :userId")
    suspend fun unblockContact(userId: Long)
}
