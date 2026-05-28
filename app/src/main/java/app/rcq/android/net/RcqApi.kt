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
 * Minimal REST client for the RCQ backend. Talks the same wire protocol as
 * the iOS client (rcq-spec) against the production instance by default.
 * Only the registration endpoint is wired so far; contacts + messaging
 * follow in later milestones.
 */
class RcqApi(private val baseUrl: String = DEFAULT_BASE_URL) {

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    data class RegisterRequest(
        val nickname: String,
        val identity_key: String,   // base64 raw X25519 public
        val signing_key: String,    // base64 raw Ed25519 public
        val inviter_uin: Int? = null,
    )

    data class RegisterResponse(
        val uin: Int,
        val token: String,
    )

    /** POST /auth/register — mints a new anonymous account (rcq-spec 2.2).
     *  No password: the returned JWT and the locally-held private keys are
     *  the only credential, so the caller must persist them immediately. */
    suspend fun register(req: RegisterRequest): RegisterResponse = withContext(Dispatchers.IO) {
        val body = gson.toJson(req).toRequestBody(JSON)
        val request = Request.Builder()
            .url("$baseUrl/auth/register")
            .post(body)
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("register failed: HTTP ${resp.code} ${text.take(200)}")
            }
            gson.fromJson(text, RegisterResponse::class.java)
                ?: throw IOException("register: empty/unparseable response")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.rcq.app"
        private val JSON = "application/json".toMediaType()
    }
}
