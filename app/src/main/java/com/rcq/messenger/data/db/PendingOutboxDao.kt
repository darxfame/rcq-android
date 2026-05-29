package com.rcq.messenger.data.db

import androidx.room.*
import com.rcq.messenger.domain.model.PendingOutboxEntity

@Dao
interface PendingOutboxDao {
    @Query("SELECT * FROM pending_outbox WHERE retryCount < maxRetries ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: PendingOutboxEntity)

    @Delete
    suspend fun delete(entry: PendingOutboxEntity)

    @Query("UPDATE pending_outbox SET retryCount = retryCount + 1 WHERE localId = :id")
    suspend fun incrementRetry(id: String)

    @Query("SELECT COUNT(*) FROM pending_outbox WHERE retryCount < maxRetries")
    suspend fun pendingCount(): Int
}
