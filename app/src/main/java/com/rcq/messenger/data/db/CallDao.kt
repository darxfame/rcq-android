package com.rcq.messenger.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.rcq.messenger.domain.model.CallEntity

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY startTime DESC")
    fun getCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls ORDER BY startTime DESC LIMIT :limit")
    fun getCalls(limit: Int): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE status = 'MISSED' ORDER BY startTime DESC")
    fun getMissedCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE id = :id")
    suspend fun getCall(id: String): CallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(calls: List<CallEntity>)

    @Delete
    suspend fun deleteCall(call: CallEntity)
}
