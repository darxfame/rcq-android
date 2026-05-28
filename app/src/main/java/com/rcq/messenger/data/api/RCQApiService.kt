package com.rcq.messenger.data.api

import com.rcq.messenger.domain.model.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody

interface RCQApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // User
    @GET("users/me")
    suspend fun getCurrentUser(): Response<User>

    @PUT("users/me")
    suspend fun updateProfile(@Body user: User): Response<User>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: Long): Response<User>

    @GET("users/{uin}/info")
    suspend fun getUserByUin(@Path("uin") uin: Long): Response<User>

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<User>>

    // Contacts
    @GET("contacts")
    suspend fun getContacts(): Response<List<User>>

    @POST("contacts")
    suspend fun addContact(@Body request: AddContactRequest): Response<Contact>

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
    suspend fun getMessageQueue(): Response<List<Message>>

    @GET("chats/{id}/messages")
    suspend fun getMessages(
        @Path("id") chatId: String,
        @Query("before") before: Long? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<Message>>

    @POST("chats/{id}/messages")
    suspend fun sendMessage(
        @Path("id") chatId: String,
        @Body message: Message
    ): Response<Message>

    @PUT("chats/{id}/messages/{msgId}")
    suspend fun editMessage(
        @Path("id") chatId: String,
        @Path("msgId") messageId: String,
        @Body message: Message
    ): Response<Message>

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

    // Groups
    @GET("groups")
    suspend fun getGroups(): Response<List<Group>>

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<Group>

    @GET("groups/{id}")
    suspend fun getGroup(@Path("id") groupId: String): Response<Group>

    @PUT("groups/{id}")
    suspend fun updateGroup(@Path("id") groupId: String, @Body group: Group): Response<Group>

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

    // Calls
    @GET("calls")
    suspend fun getCallHistory(): Response<CallLog>

    @POST("calls/initiate")
    suspend fun initiateCall(@Body request: InitiateCallRequest): Response<Call>

    @POST("calls/{id}/accept")
    suspend fun acceptCall(@Path("id") callId: String): Response<Call>

    @POST("calls/{id}/decline")
    suspend fun declineCall(@Path("id") callId: String): Response<Unit>

    @POST("calls/{id}/end")
    suspend fun endCall(@Path("id") callId: String): Response<Unit>

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

    // Marketplace
    @GET("marketplace")
    suspend fun getMarketplaceItems(
        @Query("category") category: MarketplaceCategory? = null
    ): Response<List<MarketplaceItem>>

    @POST("marketplace")
    suspend fun createListing(@Body item: MarketplaceItem): Response<MarketplaceItem>

    @GET("marketplace/{id}")
    suspend fun getItem(@Path("id") itemId: String): Response<MarketplaceItem>

    @POST("marketplace/{id}/buy")
    suspend fun buyItem(@Path("id") itemId: String): Response<Unit>

    @POST("marketplace/{id}/bid")
    suspend fun placeBid(
        @Path("id") itemId: String,
        @Body bid: Bid
    ): Response<Bid>

    // Games
    @GET("games/{type}/state")
    suspend fun getGameState(@Path("type") gameType: GameType): Response<GameState>

    @POST("games/{type}/bet")
    suspend fun placeBet(
        @Path("type") gameType: GameType,
        @Body bet: Bet
    ): Response<Bet>

    @POST("games/{type}/cashout")
    suspend fun cashout(@Path("type") gameType: GameType): Response<Bet>

    // Pets
    @GET("pets")
    suspend fun getUserPets(): Response<List<Pet>>

    @POST("pets/{id}/equip")
    suspend fun equipPet(@Path("id") petId: String): Response<Unit>

    @POST("pets/{id}/unequip")
    suspend fun unequipPet(@Path("id") petId: String): Response<Unit>

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
    @POST("crypto/register-keys")
    suspend fun uploadPreKeys(@Body request: UploadPreKeysRequest): Response<UploadPreKeysResponse>

    @GET("crypto/keys/{uin}")
    suspend fun fetchPreKeyBundle(@Path("uin") uin: Long): Response<PreKeyBundleResponse>

    @POST("crypto/keys/refresh")
    suspend fun refreshPreKeys(@Body request: RefreshPreKeysRequest): Response<Unit>

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
data class AddContactRequest(
    val userId: Long,
    val nickname: String? = null
)

@kotlinx.serialization.Serializable
data class CreateChatRequest(
    val targetId: Long
)

@kotlinx.serialization.Serializable
data class CreateGroupRequest(
    val name: String,
    val memberIds: List<Long>,
    val avatarUrl: String? = null
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
    val userId: Long
)

@kotlinx.serialization.Serializable
data class CreateRoomRequest(
    val title: String,
    val isPublic: Boolean = true,
    val maxSpeakers: Int = 10,
    val maxListeners: Int = 100
)

@kotlinx.serialization.Serializable
data class InitiateCallRequest(
    val targetId: Long,
    val type: CallType
)

@kotlinx.serialization.Serializable
data class PresenceUpdateRequest(
    @kotlinx.serialization.SerialName("status") val status: String
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

// Signal Protocol E2EE Data Classes
@kotlinx.serialization.Serializable
data class UploadPreKeysRequest(
    val preKeys: List<PreKeyData>,
    val signedPreKey: SignedPreKeyData,
    val identityKey: String
)

@kotlinx.serialization.Serializable
data class PreKeyData(
    val id: Int,
    val key: String // Base64-encoded public key
)

@kotlinx.serialization.Serializable
data class SignedPreKeyData(
    val id: Int,
    val key: String, // Base64-encoded public key
    val signature: String // Base64-encoded signature
)

@kotlinx.serialization.Serializable
data class UploadPreKeysResponse(
    val success: Boolean,
    val message: String? = null
)

@kotlinx.serialization.Serializable
data class PreKeyBundleResponse(
    val registrationId: Int,
    val deviceId: Int,
    val preKey: PreKeyData?,
    val signedPreKey: SignedPreKeyData,
    val identityKey: String
)

@kotlinx.serialization.Serializable
data class RefreshPreKeysRequest(
    val preKeys: List<PreKeyData>
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