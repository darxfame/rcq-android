package com.rcq.messenger.data.api

import com.rcq.messenger.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.PATCH
import okhttp3.MultipartBody
import okhttp3.ResponseBody

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val bio: String? = null,
    @SerialName("avatar_media_id") val avatarMediaId: String? = null,
    @SerialName("status_message") val statusMessage: String? = null
)

interface RCQApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // User — server exposes current-user data at /users/me/info (GET /users/me is PUT-only → 405)
    @GET("users/me/info")
    suspend fun getCurrentUser(): Response<User>

    @PUT("users/me")
    suspend fun updateProfile(@Body user: User): Response<User>

    @PATCH("users/me")
    suspend fun patchProfile(@Body request: UpdateProfileRequest): Response<User>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: Long): Response<User>

    @GET("users/{uin}/info")
    suspend fun getUserByUin(@Path("uin") uin: Long): Response<User>

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<User>>

    // Contacts
    @GET("contacts")
    suspend fun getContacts(): Response<List<User>>

    @DELETE("contacts/{id}")
    suspend fun removeContact(@Path("id") contactId: Long): Response<Unit>

    @POST("contacts/{id}/block")
    suspend fun blockContact(@Path("id") contactId: Long): Response<Unit>

    @POST("contacts/{id}/unblock")
    suspend fun unblockContact(@Path("id") contactId: Long): Response<Unit>

    // Contact Requests - iOS compatible
    @GET("contacts/pending")
    suspend fun getContactRequests(): Response<List<ContactRequest>>

    @POST("contacts/request")
    suspend fun sendContactRequest(@Body request: SendContactRequestBody): Response<okhttp3.ResponseBody>

    @POST("contacts/respond")
    suspend fun respondToContactRequest(@Body response: RespondContactRequestBody): Response<Unit>

    // Chats
    @GET("chats")
    suspend fun getChats(): Response<List<Chat>>

    @POST("chats")
    suspend fun createChat(@Body request: CreateChatRequest): Response<Chat>

    @GET("messages/queue")
    suspend fun getMessageQueue(@Query("ack") ack: String = "1"): Response<List<QueuedMessage>>

    @POST("messages/queue/ack")
    suspend fun ackMessageQueue(@Body body: QueueAckBody): Response<QueueAckResult>

    @POST("messages/sealed")
    suspend fun sendSealedMessage(@Body request: SealedMessageRequest): Response<SealedMessageResponse>

    // Kept for source compatibility only; server has no REST chat-message endpoints.
    @Deprecated(
        "Server has no REST message endpoints. Use POST /messages/sealed and WebSocket/control envelopes.",
        level = DeprecationLevel.ERROR
    )
    @GET("chats/{id}/messages")
    suspend fun getMessages(
        @Path("id") chatId: String,
        @Query("before") before: Long? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<Message>>

    @Deprecated(
        "Server has no REST message endpoints. Use POST /messages/sealed.",
        level = DeprecationLevel.ERROR
    )
    @POST("chats/{id}/messages")
    suspend fun sendMessage(
        @Path("id") chatId: String,
        @Body message: Message
    ): Response<Message>

    @Deprecated(
        "Server has no REST message edit endpoint. Use WebSocket/control envelope when available.",
        level = DeprecationLevel.ERROR
    )
    @PUT("chats/{id}/messages/{msgId}")
    suspend fun editMessage(
        @Path("id") chatId: String,
        @Path("msgId") messageId: String,
        @Body message: Message
    ): Response<Message>

    @Deprecated(
        "Server has no REST message delete endpoint. Use WebSocket/control envelope when available.",
        level = DeprecationLevel.ERROR
    )
    @DELETE("chats/{id}/messages/{msgId}")
    suspend fun deleteMessage(
        @Path("id") chatId: String,
        @Path("msgId") messageId: String
    ): Response<Unit>

    @POST("chats/{id}/messages/{msgId}/reactions")
    suspend fun addReaction(
        @Path("id") chatId: String,
        @Path("msgId") messageId: String,
        @Body reaction: Reaction
    ): Response<Message>

    @GET("groups")
    suspend fun getGroups(): Response<List<GroupApiResponse>>

    /** Search public groups by name substring — mirrors iOS GET /groups/search?q=&limit= */
    @GET("groups/search")
    suspend fun browsePublicGroups(
        @retrofit2.http.Query("q") query: String,
        @retrofit2.http.Query("limit") limit: Int = 20
    ): Response<List<GroupApiResponse>>

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<Group>

    @GET("groups/{id}")
    suspend fun getGroup(@Path("id") groupId: String): Response<GroupApiResponse>

    @PATCH("groups/{id}")
    suspend fun patchGroup(@Path("id") groupId: String, @Body patch: GroupPatchRequest): Response<GroupApiResponse>

    @GET("groups/{id}/preview")
    suspend fun getGroupPreview(@Path("id") groupId: Int): Response<GroupPreviewResponse>

    @POST("groups/{id}/join")
    suspend fun joinGroup(@Path("id") groupId: Int): Response<GroupApiResponse>

    @DELETE("groups/{id}")
    suspend fun deleteGroup(@Path("id") groupId: String): Response<Unit>

    @POST("groups/{id}/members")
    suspend fun addMember(
        @Path("id") groupId: String,
        @Body request: AddMemberRequest
    ): Response<GroupMember>

    @DELETE("groups/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") groupId: String,
        @Path("userId") userId: Long
    ): Response<Unit>

    // Audio Rooms
    @GET("rooms")
    suspend fun getAudioRooms(): Response<List<AudioRoom>>

    @POST("rooms")
    suspend fun createRoom(@Body request: CreateRoomRequest): Response<AudioRoom>

    @GET("rooms/{id}")
    suspend fun getRoom(@Path("id") roomId: String): Response<AudioRoom>

    @POST("rooms/{id}/join")
    suspend fun joinRoom(@Path("id") roomId: String): Response<AudioRoom>

    @POST("rooms/{id}/leave")
    suspend fun leaveRoom(@Path("id") roomId: String): Response<Unit>

    @POST("rooms/{id}/mute")
    suspend fun toggleMute(@Path("id") roomId: String): Response<Unit>

    @POST("rooms/{id}/raise-hand")
    suspend fun raiseHand(@Path("id") roomId: String): Response<Unit>

    // Stories
    @GET("stories")
    suspend fun getStories(): Response<List<Story>>

    @POST("stories")
    suspend fun createStory(@Body story: Story): Response<Story>

    @DELETE("stories/{id}")
    suspend fun deleteStory(@Path("id") storyId: String): Response<Unit>

    @POST("stories/{id}/view")
    suspend fun viewStory(@Path("id") storyId: String): Response<Unit>

    @POST("stories/{id}/reply")
    suspend fun replyToStory(
        @Path("id") storyId: String,
        @Body reply: StoryReply
    ): Response<Unit>


    // Nearby
    @GET("nearby")
    suspend fun getNearbyUsers(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Int = 1000
    ): Response<List<User>>

    // Settings
    @GET("settings")
    suspend fun getSettings(): Response<UserSettings>

    @PUT("settings")
    suspend fun updateSettings(@Body settings: UserSettings): Response<UserSettings>

    // Presence
    @POST("presence/status")
    suspend fun updatePresence(@Body request: PresenceUpdateRequest): Response<Unit>

    // Signal Protocol E2EE - Pre-Key Management
    @POST("keys/bundle")
    suspend fun uploadBundle(@Body request: RegisterBundleRequest): Response<Unit>

    @GET("keys/{uin}/bundle")
    suspend fun fetchPreKeyBundle(@Path("uin") uin: Long): Response<PreKeyBundleResponse>

    @POST("keys/prekeys")
    suspend fun replenishPreKeys(@Body request: ReplenishPreKeysRequest): Response<Unit>

    @GET("keys/me/status")
    suspend fun getKeyStatus(): Response<KeyStatusResponse>

    // Group sealed-sender fan-out
    @POST("messages/group-sealed")
    suspend fun sendGroupSealedMessage(@Body request: GroupSealedMessageRequest): Response<SealedMessageResponse>

    // Media Upload/Download
    @Multipart
    @POST("media/upload")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<MediaUploadResponse>

    @GET("media/{mediaId}")
    suspend fun downloadMedia(@Path("mediaId") mediaId: String): Response<ResponseBody>

    @GET("media/usage")
    suspend fun getMediaUsage(): Response<MediaUsageResponse>

    @DELETE("media/{mediaId}")
    suspend fun deleteMedia(@Path("mediaId") mediaId: String): Response<Unit>
}

// Request/Response classes
@kotlinx.serialization.Serializable
data class RegisterRequest(
    val nickname: String,
    val identity_key: String,
    val signing_key: String
)

@kotlinx.serialization.Serializable
data class AuthResponse(
    val uin: Long,
    val token: String
)

@kotlinx.serialization.Serializable
data class CreateChatRequest(
    val targetId: Long
)

@kotlinx.serialization.Serializable
data class CreateGroupRequest(
    val name: String,
    @kotlinx.serialization.SerialName("member_uins") val memberUins: List<Long>
)

@kotlinx.serialization.Serializable
data class SendContactRequestBody(
    @kotlinx.serialization.SerialName("to_uin") val toUin: Long
)

@kotlinx.serialization.Serializable
data class RespondContactRequestBody(
    val request_id: Long,
    val accept: Boolean
)

@kotlinx.serialization.Serializable
data class AddMemberRequest(
    val uin: Long
)

@kotlinx.serialization.Serializable
data class CreateRoomRequest(
    val title: String,
    val isPublic: Boolean = true,
    val maxSpeakers: Int = 10,
    val maxListeners: Int = 100
)

@kotlinx.serialization.Serializable
data class PresenceUpdateRequest(
    @kotlinx.serialization.SerialName("status") val status: String,
    @kotlinx.serialization.SerialName("status_message") val statusMessage: String? = null
)

@kotlinx.serialization.Serializable
data class UserSettings(
    val notifications: NotificationSettings = NotificationSettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val storage: StorageSettings = StorageSettings()
)

@kotlinx.serialization.Serializable
data class NotificationSettings(
    val messageNotifications: Boolean = true,
    val groupNotifications: Boolean = true,
    val callNotifications: Boolean = true,
    val storyNotifications: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showPreview: Boolean = true
)

@kotlinx.serialization.Serializable
data class PrivacySettings(
    val lastSeen: Boolean = true,
    val onlineStatus: Boolean = true,
    val readReceipts: Boolean = true,
    val typingIndicator: Boolean = true,
    val profilePhoto: ProfilePrivacy = ProfilePrivacy.EVERYONE,
    val about: ProfilePrivacy = ProfilePrivacy.EVERYONE
)

@kotlinx.serialization.Serializable
enum class ProfilePrivacy {
    EVERYONE, CONTACTS_ONLY, NOBODY
}

@kotlinx.serialization.Serializable
data class AppearanceSettings(
    val theme: String = "dark",
    val language: String = "en",
    val messageBubbleStyle: String = "rounded",
    val fontSize: Int = 14
)

@kotlinx.serialization.Serializable
data class StorageSettings(
    val autoDownloadWifi: Boolean = true,
    val autoDownloadCellular: Boolean = false,
    val saveToGallery: Boolean = true,
    val qualityPreset: String = "auto"
)

@kotlinx.serialization.Serializable
data class ContactRequest(
    val id: Long,
    @kotlinx.serialization.SerialName("from_uin") val fromUin: Long,
    val nickname: String,
    @kotlinx.serialization.SerialName("avatar_url") val avatarUrl: String? = null,
    @kotlinx.serialization.SerialName("state") val status: String = "pending"
)

// Signal Protocol E2EE Data Classes — field names match server keys.py exactly

@kotlinx.serialization.Serializable
data class PreKeyData(
    val id: Int,
    @kotlinx.serialization.SerialName("public") val key: String
)

@kotlinx.serialization.Serializable
data class SignedPreKeyData(
    val id: Int,
    @kotlinx.serialization.SerialName("public") val key: String,
    val signature: String
)

@kotlinx.serialization.Serializable
data class KyberPreKeyData(
    val id: Int,
    @kotlinx.serialization.SerialName("public") val key: String,
    val signature: String
)

// POST /keys/bundle — BundleIn on the server
@kotlinx.serialization.Serializable
data class RegisterBundleRequest(
    @kotlinx.serialization.SerialName("signal_identity_key") val signalIdentityKey: String,
    @kotlinx.serialization.SerialName("registration_id") val registrationId: Int,
    @kotlinx.serialization.SerialName("signed_prekey") val signedPreKey: SignedPreKeyData,
    @kotlinx.serialization.SerialName("kyber_prekey") val kyberPreKey: KyberPreKeyData,
    @kotlinx.serialization.SerialName("one_time_prekeys") val oneTimePreKeys: List<PreKeyData> = emptyList()
)

// POST /keys/prekeys — replenish OPK pool
@kotlinx.serialization.Serializable
data class ReplenishPreKeysRequest(
    @kotlinx.serialization.SerialName("one_time_prekeys") val oneTimePreKeys: List<PreKeyData>
)

@kotlinx.serialization.Serializable
data class KeyStatusResponse(
    @kotlinx.serialization.SerialName("has_bundle") val hasBundle: Boolean,
    @kotlinx.serialization.SerialName("one_time_prekey_count") val oneTimePreKeyCount: Int,
    @kotlinx.serialization.SerialName("target_count") val targetCount: Int,
    @kotlinx.serialization.SerialName("signed_prekey_age_seconds") val signedPreKeyAgeSeconds: Int? = null
)

// GET /keys/{uin}/bundle — BundleOut on the server
@kotlinx.serialization.Serializable
data class PreKeyBundleResponse(
    val uin: Long,
    @kotlinx.serialization.SerialName("registration_id") val registrationId: Int,
    @kotlinx.serialization.SerialName("signal_identity_key") val identityKey: String,
    @kotlinx.serialization.SerialName("signed_prekey") val signedPreKey: SignedPreKeyData,
    @kotlinx.serialization.SerialName("kyber_prekey") val kyberPreKey: KyberPreKeyData,
    @kotlinx.serialization.SerialName("one_time_prekey") val preKey: PreKeyData? = null
)

// Group sealed-sender fan-out
@kotlinx.serialization.Serializable
data class GroupSealedRecipient(
    @kotlinx.serialization.SerialName("to_uin") val toUin: Long,
    val payload: String
)

@kotlinx.serialization.Serializable
data class GroupSealedMessageRequest(
    @kotlinx.serialization.SerialName("group_id") val groupId: Int,
    @kotlinx.serialization.SerialName("envelope_type") val envelopeType: String = "message",
    val payloads: List<GroupSealedRecipient>
)

// Media API Data Classes
@kotlinx.serialization.Serializable
data class MediaUploadResponse(
    val mediaId: String,
    val url: String,
    val size: Long,
    val mimeType: String? = null
)

@kotlinx.serialization.Serializable
data class MediaUsageResponse(
    val usedBytes: Long,
    val totalBytes: Long,
    val fileCount: Int
)

// Matches iOS RCQGroup exactly (confirmed from rcq-ios/Models/Group.swift)
@kotlinx.serialization.Serializable
data class GroupApiResponse(
    val id: Int,
    val name: String,
    val description: String? = null,
    @kotlinx.serialization.SerialName("owner_uin") val ownerUin: Int,
    @kotlinx.serialization.SerialName("avatar_seed") val avatarSeed: Int = 0,
    @kotlinx.serialization.SerialName("post_policy") val postPolicy: String = "all",
    @kotlinx.serialization.SerialName("is_closed") val isClosed: Boolean = false,
    @kotlinx.serialization.SerialName("members_hidden") val membersHidden: Boolean = false,
    @kotlinx.serialization.SerialName("pinned_text") val pinnedText: String? = null,
    @kotlinx.serialization.SerialName("pinned_at") val pinnedAt: String? = null,
    @kotlinx.serialization.SerialName("pinned_by") val pinnedBy: Int? = null,
    @kotlinx.serialization.SerialName("avatar_media_id") val avatarMediaId: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_key") val avatarMediaKey: String? = null,
    @kotlinx.serialization.SerialName("created_at") val createdAt: String = "",
    @kotlinx.serialization.SerialName("member_count") val memberCount: Int = 0,
    val members: List<GroupMemberApi> = emptyList()
)

@kotlinx.serialization.Serializable
data class GroupPatchRequest(
    val name: String? = null,
    val description: String? = null,
    @kotlinx.serialization.SerialName("post_policy") val postPolicy: String? = null,
    @kotlinx.serialization.SerialName("is_closed") val isClosed: Boolean? = null,
    @kotlinx.serialization.SerialName("members_hidden") val membersHidden: Boolean? = null,
    @kotlinx.serialization.SerialName("pinned_text") val pinnedText: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_id") val avatarMediaId: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_key") val avatarMediaKey: String? = null
)

@kotlinx.serialization.Serializable
data class GroupPreviewResponse(
    val id: Int,
    val name: String,
    val description: String? = null,
    @kotlinx.serialization.SerialName("member_count") val memberCount: Int,
    @kotlinx.serialization.SerialName("is_closed") val isClosed: Boolean = false,
    @kotlinx.serialization.SerialName("owner_uin") val ownerUin: Int,
    @kotlinx.serialization.SerialName("owner_nickname") val ownerNickname: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_id") val avatarMediaId: String? = null,
    @kotlinx.serialization.SerialName("avatar_media_key") val avatarMediaKey: String? = null
)

@kotlinx.serialization.Serializable
data class GroupMemberApi(
    val uin: Int,
    val nickname: String,
    val role: String = "member",
    val status: String = "offline",
    @kotlinx.serialization.SerialName("identity_key") val identityKey: String = "",
    @kotlinx.serialization.SerialName("signing_key") val signingKey: String = "",
    @kotlinx.serialization.SerialName("signal_identity_key") val signalIdentityKey: String? = null
)

// Matches iOS POST /messages/sealed body (rcq-ios/Services/MessageService.swift:655)
// { to_uin, envelope_type, payload } — payload is the encrypted envelope blob
@kotlinx.serialization.Serializable
data class SealedMessageRequest(
    @kotlinx.serialization.SerialName("to_uin") val toUin: Long,
    @kotlinx.serialization.SerialName("envelope_type") val envelopeType: String = "message",
    val payload: String  // AES/Signal-encrypted envelope — no plaintext
)

@kotlinx.serialization.Serializable
data class SealedMessageResponse(
    val delivered: Boolean = false,
    val queued: Boolean = false,
    @kotlinx.serialization.SerialName("server_time") val serverTime: String = ""
)

// Offline queue — matches server OfflineMessage / OfflineGroupMessage schemas
@kotlinx.serialization.Serializable
data class QueuedMessage(
    val id: Int,
    @kotlinx.serialization.SerialName("envelope_type") val envelopeType: String,
    val payload: String,
    @kotlinx.serialization.SerialName("received_at") val receivedAt: String,
    @kotlinx.serialization.SerialName("group_id") val groupId: Int? = null
)

@kotlinx.serialization.Serializable
data class QueueAckBody(
    @kotlinx.serialization.SerialName("direct_ids") val directIds: List<Int>,
    @kotlinx.serialization.SerialName("group_ids") val groupIds: List<Int>
)

@kotlinx.serialization.Serializable
data class QueueAckResult(
    val deleted: Int = 0
)
