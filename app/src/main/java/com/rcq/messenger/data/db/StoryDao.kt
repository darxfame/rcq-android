package com.rcq.messenger.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.rcq.messenger.domain.model.StoryEntity
import com.rcq.messenger.domain.model.StoryItemEntity

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    fun getStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserStories(userId: Long): Flow<List<StoryEntity>>

    @Query("SELECT * FROM story_items WHERE storyId = :storyId ORDER BY timestamp ASC")
    fun getStoryItems(storyId: String): Flow<List<StoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryItem(item: StoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryItems(items: List<StoryItemEntity>)

    @Delete
    suspend fun deleteStory(story: StoryEntity)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: String)

    @Delete
    suspend fun deleteStoryItem(item: StoryItemEntity)
}