package app.rcq.android.net

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import app.rcq.android.BuildConfig
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Self-update for the website-distributed APK (there's no Play Store auto-
 * update). On launch the app fetches a small JSON manifest hosted next to the
 * APKs; if its `versionCode` is newer than this build it offers to download the
 * matching per-ABI APK and hand it to the system package installer. The user
 * still confirms the install (sideload installs always require consent) — they
 * just don't have to hunt down + download the file by hand.
 *
 * Hosting (founder): drop `latest.json` + the per-ABI APKs under
 * `https://rcq.app/android/`. On each release bump `versionCode` (must match the
 * build's) and keep the SAME signing key, or the update can't install in place.
 *
 * Manifest shape:
 *   {
 *     "versionCode": 2,
 *     "versionName": "0.2",
 *     "notes": "What changed",
 *     "url": "https://rcq.app/android/rcq-universal.apk",
 *     "abis": {
 *       "arm64-v8a":   "https://rcq.app/android/rcq-arm64-v8a.apk",
 *       "armeabi-v7a": "https://rcq.app/android/rcq-armeabi-v7a.apk",
 *       "x86_64":      "https://rcq.app/android/rcq-x86_64.apk"
 *     }
 *   }
 */
object UpdateChecker {
    private const val MANIFEST_URL = "https://rcq.app/android/latest.json"

    data class Update(val versionCode: Int, val versionName: String, val notes: String, val apkUrl: String)

    // Route through the censorship transport when it's engaged (the site may be
    // blocked on the same networks the transport exists to pierce).
    private fun client(): OkHttpClient {
        val b = OkHttpClient.Builder()
            .callTimeout(120, TimeUnit.SECONDS)
        SingBoxTransport.proxy()?.let { b.proxy(it) }
        return b.build()
    }

    /** The hosted update when it's newer than this build, else null. */
    suspend fun check(): Update? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(MANIFEST_URL).build()
            client().newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val obj = JsonParser.parseString(resp.body!!.string()).asJsonObject
                val vc = obj.get("versionCode")?.asInt ?: return@use null
                if (vc <= BuildConfig.VERSION_CODE) return@use null
                val abis = obj.getAsJsonObject("abis")
                val abiUrl = Build.SUPPORTED_ABIS.firstNotNullOfOrNull { abi -> abis?.get(abi)?.asString }
                val url = abiUrl ?: obj.get("url")?.asString ?: return@use null
                Update(vc, obj.get("versionName")?.asString ?: "$vc", obj.get("notes")?.asString.orEmpty(), url)
            }
        }.getOrNull()
    }

    /** Download the APK to cacheDir/files/ and launch the system installer.
     *  Returns false on any failure (network, write, no installer). */
    suspend fun downloadAndInstall(context: Context, update: Update): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "files").apply { mkdirs() }
            val apk = File(dir, "rcq-update-${update.versionCode}.apk")
            val req = Request.Builder().url(update.apkUrl).build()
            client().newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                apk.outputStream().use { out -> resp.body!!.byteStream().copyTo(out) }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
