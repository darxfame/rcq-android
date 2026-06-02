package com.rcq.messenger.media

import android.content.Context
import android.net.Uri
import com.rcq.messenger.crypto.CryptoService
import com.rcq.messenger.data.api.RCQApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: RCQApiService,
    private val cryptoService: CryptoService
) {

    private val mediaDir = File(context.filesDir, "media")

    init {
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
    }

    suspend fun uploadMedia(
        uri: Uri,
        mediaType: MediaType,
        recipientUin: Long? = null
    ): Result<MediaUploadResult> = withContext(Dispatchers.IO) {
        try {
            // Read file from URI
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))

            val originalBytes = inputStream.readBytes()
            inputStream.close()

            // Encrypt file content if recipient specified
            val encryptedBytes = if (recipientUin != null) {
                val base64Content = android.util.Base64.encodeToString(originalBytes, android.util.Base64.NO_WRAP)
                val encrypted = cryptoService.encryptMessage(recipientUin, base64Content)
                encrypted.ciphertext.toByteArray()
            } else {
                originalBytes
            }

            // Create temporary file for upload
            val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(encryptedBytes) }

            // Prepare multipart request
            val requestFile = tempFile.asRequestBody("application/octet-stream".toMediaType())
            val body = MultipartBody.Part.createFormData("file", "media_file", requestFile)

            // Upload to server
            val response = apiService.uploadMedia(body)

            // Clean up temp file
            tempFile.delete()

            if (response.isSuccessful) {
                val uploadResponse = response.body()!!
                Result.success(
                    MediaUploadResult(
                        mediaId = uploadResponse.mediaId,
                        url = uploadResponse.url,
                        size = originalBytes.size.toLong(),
                        mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
                        isEncrypted = recipientUin != null
                    )
                )
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadMedia(
        mediaId: String,
        senderUin: Long? = null,
        signalType: Int = 1
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Download from server
            val response = apiService.downloadMedia(mediaId)

            if (response.isSuccessful) {
                val responseBody = response.body()!!
                val encryptedBytes = responseBody.bytes()

                // Decrypt if sender specified
                val decryptedBytes = if (senderUin != null) {
                    val encryptedContent = String(encryptedBytes)
                    val decryptedBase64 = cryptoService.decryptMessage(senderUin, encryptedContent, signalType)
                    android.util.Base64.decode(decryptedBase64, android.util.Base64.NO_WRAP)
                } else {
                    encryptedBytes
                }

                // Save to local media directory
                val localFile = File(mediaDir, mediaId)
                FileOutputStream(localFile).use { it.write(decryptedBytes) }

                Result.success(localFile)
            } else {
                Result.failure(Exception("Download failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordVoiceMessage(): Result<File> = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement actual voice recording
            // For now, create a placeholder file
            val voiceFile = File(mediaDir, "voice_${System.currentTimeMillis()}.m4a")
            voiceFile.createNewFile()
            Result.success(voiceFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLocalMediaFile(mediaId: String): File? {
        val file = File(mediaDir, mediaId)
        return if (file.exists()) file else null
    }

    fun deleteLocalMedia(mediaId: String): Boolean {
        val file = File(mediaDir, mediaId)
        return if (file.exists()) file.delete() else false
    }

    fun getMediaCacheSize(): Long {
        return mediaDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    fun clearMediaCache() {
        mediaDir.listFiles()?.forEach { it.delete() }
    }
}

data class MediaUploadResult(
    val mediaId: String,
    val url: String,
    val size: Long,
    val mimeType: String,
    val isEncrypted: Boolean
)

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    VOICE,
    DOCUMENT
}

// API Response models
@kotlinx.serialization.Serializable
data class MediaUploadResponse(
    val mediaId: String,
    val url: String,
    val size: Long
)