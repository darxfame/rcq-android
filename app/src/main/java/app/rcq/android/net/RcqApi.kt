package app.rcq.android.net

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal REST client for the RCQ backend, talking the same wire protocol
 * as the iOS client (rcq-spec) against prod by default. Registration,
 * peer lookup, and 1:1 sealed send are wired; contacts roster + media come
 * later.
 */
class RcqApi(private val baseUrl: String = DEFAULT_BASE_URL) {

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    @Volatile
    private var token: String? = null
    fun setToken(t: String?) { token = t }

    // ── register (rcq-spec 2.2) ──────────────────────────────────────

    data class RegisterRequest(
        val nickname: String,
        val identity_key: String,
        val signing_key: String,
        val inviter_uin: Int? = null,
    )

    data class RegisterResponse(val uin: Int, val token: String)

    suspend fun register(req: RegisterRequest): RegisterResponse = withContext(Dispatchers.IO) {
        post("/auth/register", gson.toJson(req), authed = false, RegisterResponse::class.java)
    }

    // ── peer lookup (rcq-spec 3.1) ───────────────────────────────────

    data class UserInfo(
        val uin: Int,
        val nickname: String?,
        val identity_key: String?,   // base64 raw X25519 public
        val signing_key: String?,    // base64 raw Ed25519 public
    )

    suspend fun userInfo(uin: Int): UserInfo = withContext(Dispatchers.IO) {
        get("/users/$uin/info", authed = true, UserInfo::class.java)
    }

    // ── 1:1 send (rcq-spec 6.2.1) ────────────────────────────────────

    data class SendRequest(val to_uin: Int, val envelope_type: String, val payload: String)
    data class SendResponse(val delivered: Boolean = false, val queued: Boolean = false)

    suspend fun sendSealed(toUin: Int, payloadB64: String, envelopeType: String = "message"): SendResponse =
        withContext(Dispatchers.IO) {
            post(
                "/messages/sealed",
                gson.toJson(SendRequest(toUin, envelopeType, payloadB64)),
                authed = false, // sealed-sender is anonymous by design
                SendResponse::class.java,
            )
        }

    // ── offline queue drain (rcq-spec 6.3.1) ─────────────────────────

    data class QueuedEnvelope(
        val id: Int,
        val envelope_type: String?,
        val payload: String?,
        val received_at: String?,
        val group_id: Int? = null,
    )

    suspend fun drainQueue(): List<QueuedEnvelope> = withContext(Dispatchers.IO) {
        get("/messages/queue", authed = true, Array<QueuedEnvelope>::class.java).toList()
    }

    // ── contacts (rcq-spec 4) ────────────────────────────────────────

    data class ContactRow(
        val uin: Int,
        val nickname: String?,
        val status: String?,
        val identity_key: String?,
        val signing_key: String?,
    )

    suspend fun contacts(): List<ContactRow> = withContext(Dispatchers.IO) {
        get("/contacts", authed = true, Array<ContactRow>::class.java).toList()
    }

    data class PendingRow(
        val id: Int,
        val from_uin: Int,
        val nickname: String?,
        val state: String?,
    )

    suspend fun pending(): List<PendingRow> = withContext(Dispatchers.IO) {
        get("/contacts/pending", authed = true, Array<PendingRow>::class.java).toList()
    }

    data class ContactRequestBody(val to_uin: Int)
    data class ContactRequestResponse(val id: Int = 0, val state: String? = null, val auto: Boolean = false)

    suspend fun requestContact(toUin: Int): ContactRequestResponse = withContext(Dispatchers.IO) {
        post("/contacts/request", gson.toJson(ContactRequestBody(toUin)), authed = true, ContactRequestResponse::class.java)
    }

    data class RespondBody(val request_id: Int, val accept: Boolean)
    data class RespondResponse(val state: String? = null)

    suspend fun respondContact(requestId: Int, accept: Boolean): RespondResponse = withContext(Dispatchers.IO) {
        post("/contacts/respond", gson.toJson(RespondBody(requestId, accept)), authed = true, RespondResponse::class.java)
    }

    // ── plumbing ─────────────────────────────────────────────────────

    private fun <T> post(path: String, json: String, authed: Boolean, type: Class<T>): T {
        val builder = Request.Builder()
            .url("$baseUrl$path")
            .post(json.toRequestBody(JSON))
        if (authed) token?.let { builder.header("Authorization", "Bearer $it") }
        return execute(builder.build(), type)
    }

    private fun <T> get(path: String, authed: Boolean, type: Class<T>): T {
        val builder = Request.Builder().url("$baseUrl$path").get()
        if (authed) token?.let { builder.header("Authorization", "Bearer $it") }
        return execute(builder.build(), type)
    }

    private fun <T> execute(request: Request, type: Class<T>): T {
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: ${text.take(200)}")
            }
            return gson.fromJson(text, type) ?: throw IOException("empty/unparseable response")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.rcq.app"
        private val JSON = "application/json".toMediaType()
    }
}
