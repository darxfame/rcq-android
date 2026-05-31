package com.rcq.messenger.data.repository

import android.util.Log
import com.rcq.messenger.data.api.*
import com.rcq.messenger.data.db.*
import com.rcq.messenger.domain.model.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.rcq.messenger.di.PreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val api: RCQApiService,
    private val groupDao: GroupDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object { private const val TAG = "GroupRepository" }

    fun getGroups(): Flow<List<Group>> = combine(
        groupDao.getGroups(),
        dataStore.data.map { it[PreferencesKeys.USER_UIN] ?: 0L }
    ) { entities, ownUin ->
        entities
            .filter { ownUin == 0L || ownUin in it.memberIds }
            .map { it.toDomain() }
    }

    suspend fun syncGroups(): Result<Unit> = runCatching {
        Log.d(TAG, "syncGroups: fetching from server...")
        val response = api.getGroups()
        Log.d(TAG, "syncGroups: HTTP ${response.code()}")
        if (response.isSuccessful) {
            val groups = response.body() ?: emptyList()
            Log.d(TAG, "syncGroups: got ${groups.size} groups: ${groups.map { "${it.id}/${it.name}" }}")
            groupDao.insertGroups(groups.map { it.toGroupEntity() })
        } else {
            val err = response.errorBody()?.string()
            Log.e(TAG, "syncGroups: server error ${response.code()} — $err")
            Unit
        }
    }.onFailure { e -> Log.e(TAG, "syncGroups: exception — ${e.message}", e) }

    /** Search public groups by name/description — server-side, not limited to user's groups */
    suspend fun searchPublicGroups(query: String): Result<List<Group>> = runCatching {
        val response = api.browsePublicGroups(query)
        if (response.isSuccessful) {
            response.body()?.map { it.toGroupEntity().toDomain() } ?: emptyList()
        } else {
            Log.e(TAG, "browsePublicGroups: HTTP ${response.code()} — ${response.errorBody()?.string()}")
            emptyList()
        }
    }.onFailure { e -> Log.e(TAG, "browsePublicGroups: exception — ${e.message}", e) }

    suspend fun createGroup(name: String, memberIds: List<Long>): Result<Group> = runCatching {
        api.createGroup(CreateGroupRequest(name = name, memberUins = memberIds)).let { response ->
            if (response.isSuccessful) response.body()!!.also {
                groupDao.insertGroup(it.toEntity())
            }
            else throw Exception("Failed to create group")
        }
    }

    suspend fun getGroup(groupId: String): Result<Group> = runCatching {
        api.getGroup(groupId).let { response ->
            if (response.isSuccessful) response.body()!!.toGroupEntity().toDomain()
            else throw Exception("Group not found")
        }
    }

    suspend fun updateGroup(group: Group): Result<Group> = runCatching {
        api.updateGroup(group.id, group).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to update group")
        }
    }

    suspend fun addMember(groupId: String, userId: Long): Result<Unit> = runCatching {
        api.addMember(groupId, AddMemberRequest(userId)).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to add member")
        }
    }

    suspend fun removeMember(groupId: String, userId: Long): Result<Unit> = runCatching {
        api.removeMember(groupId, userId).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to remove member")
        }
    }
}

private fun com.rcq.messenger.data.api.GroupApiResponse.toGroupEntity() = GroupEntity(
    id = id.toString(),
    name = name,
    avatarUrl = null,
    description = description,
    creatorId = ownerUin.toLong(),
    memberIds = members.map { it.uin.toLong() },
    adminIds = members.filter { it.role == "admin" || it.role == "owner" }.map { it.uin.toLong() },
    createdAt = System.currentTimeMillis()
)

private fun GroupEntity.toDomain() = Group(
    id = id, name = name, avatarUrl = avatarUrl, description = description ?: "",
    ownerId = creatorId, adminIds = adminIds, memberIds = memberIds,
    memberCount = memberIds.size, createdAt = createdAt, settings = GroupSettings()
)

private fun Group.toEntity() = GroupEntity(
    id = id, name = name, avatarUrl = avatarUrl, description = description,
    creatorId = ownerId, memberIds = memberIds, adminIds = adminIds,
    createdAt = createdAt
)

@Singleton
class AudioRoomRepository @Inject constructor(
    private val api: RCQApiService
) {
    suspend fun getAudioRooms(): Result<List<AudioRoom>> = runCatching {
        api.getAudioRooms().let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to get rooms")
        }
    }

    suspend fun createRoom(title: String, isPublic: Boolean): Result<AudioRoom> = runCatching {
        api.createRoom(CreateRoomRequest(title, isPublic)).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to create room")
        }
    }

    suspend fun getRoom(roomId: String): Result<AudioRoom> = runCatching {
        api.getRoom(roomId).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Room not found")
        }
    }

    suspend fun joinRoom(roomId: String): Result<AudioRoom> = runCatching {
        api.joinRoom(roomId).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to join room")
        }
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> = runCatching {
        api.leaveRoom(roomId).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to leave room")
        }
    }

    suspend fun toggleMute(roomId: String): Result<Unit> = runCatching {
        api.toggleMute(roomId).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to toggle mute")
        }
    }

    suspend fun raiseHand(roomId: String): Result<Unit> = runCatching {
        api.raiseHand(roomId).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to raise hand")
        }
    }
}

@Singleton
class CallRepository @Inject constructor(
    private val api: RCQApiService,
    private val callDao: CallDao
) {
    fun getCalls(limit: Int = 50): Flow<List<Call>> = callDao.getCalls(limit).map { entities ->
        entities.map { it.toDomain() }
    }

    fun getMissedCalls(): Flow<List<Call>> = callDao.getMissedCalls().map { entity ->
        entity.map { it.toDomain() }
    }

    suspend fun syncCallHistory(): Result<Unit> = runCatching {
        api.getCallHistory().let { response ->
            if (response.isSuccessful) {
                response.body()?.calls?.forEach { callDao.insertCall(it.toEntity()) }
            }
        }
    }

    suspend fun initiateCall(targetId: Long, type: CallType): Result<Call> = runCatching {
        api.initiateCall(InitiateCallRequest(targetId, type)).let { response ->
            if (response.isSuccessful) response.body()!!.also {
                callDao.insertCall(it.toEntity())
            }
            else throw Exception("Failed to initiate call")
        }
    }

    suspend fun acceptCall(callId: String): Result<Call> = runCatching {
        api.acceptCall(callId).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to accept call")
        }
    }

    suspend fun declineCall(callId: String): Result<Unit> = runCatching {
        api.declineCall(callId).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to decline call")
        }
    }

    suspend fun endCall(callId: String): Result<Unit> = runCatching {
        api.endCall(callId).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to end call")
        }
    }
}

private fun CallEntity.toDomain() = Call(
    id = id, type = CallType.valueOf(type),
    targetId = participantIds.firstOrNull() ?: 0L,
    targetNickname = "", targetAvatar = null,
    initiatorId = initiatorId, status = CallStatus.valueOf(status),
    startedAt = startTime, endedAt = endTime, duration = duration
)

private fun Call.toEntity() = CallEntity(
    id = id,
    type = type.name,
    status = status.name,
    participantIds = listOf(initiatorId), // Convert single target to list
    initiatorId = initiatorId,
    startTime = startedAt ?: 0L,
    endTime = endedAt,
    duration = duration,
    isGroupCall = false
)

@Singleton
class StoryRepository @Inject constructor(
    private val api: RCQApiService,
    private val storyDao: StoryDao
) {
    fun getStories(): Flow<List<Story>> = storyDao.getStories().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun syncStories(): Result<Unit> = runCatching {
        api.getStories().let { response ->
            if (response.isSuccessful) {
                response.body()?.forEach { story ->
                    storyDao.insertStory(story.toEntity())
                    storyDao.insertStoryItems(story.items.map { it.toEntity(story.id) })
                }
            }
        }
    }

    suspend fun createStory(story: Story): Result<Story> = runCatching {
        api.createStory(story).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to create story")
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> = runCatching {
        api.deleteStory(storyId).let { response ->
            if (response.isSuccessful) storyDao.deleteStoryById(storyId)
            else throw Exception("Failed to delete story")
        }
    }

    suspend fun viewStory(storyId: String): Result<Unit> = runCatching {
        api.viewStory(storyId).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to view story")
        }
    }

    suspend fun replyToStory(storyId: String, reply: StoryReply): Result<Unit> = runCatching {
        api.replyToStory(storyId, reply).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to reply to story")
        }
    }
}

private fun StoryEntity.toDomain() = Story(
    id = id, userId = userId, nickname = nickname ?: "",
    avatarUrl = avatarUrl, items = emptyList(),
    viewerCount = viewerCount, createdAt = createdAt,
    expiresAt = expiresAt, isActive = isActive
)

private fun Story.toEntity() = StoryEntity(
    id = id, userId = userId, nickname = nickname,
    avatarUrl = avatarUrl, viewerCount = viewerCount,
    createdAt = createdAt, expiresAt = expiresAt, isActive = isActive
)

private fun StoryItem.toEntity(storyId: String) = StoryItemEntity(
    id = id, storyId = storyId, type = type.name,
    mediaUrl = mediaUrl, thumbnailUrl = thumbnailUrl,
    caption = caption, backgroundColor = backgroundColor,
    duration = duration.toLong(), timestamp = createdAt
)