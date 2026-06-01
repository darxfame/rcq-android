package app.rcq.android.security

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.rcq.android.crypto.BiometricVault

/**
 * Drives the [BiometricPrompt] UI for the two panic-PIN biometric flows. Lives
 * apart from [BiometricVault] (the crypto) because the prompt must be shown from
 * a [FragmentActivity] on the main thread; everything here is UI glue.
 */
object BiometricGate {

    /** Show the prompt to ENABLE biometric unlock: a success authorises a fresh
     *  encrypt cipher that seals [payload] (the real-slot blob). [onResult] gets
     *  true on success, false on cancel/error. Runs on the main thread. */
    fun enable(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negative: String,
        payload: ByteArray,
        onResult: (Boolean) -> Unit,
    ) {
        val cipher = try {
            BiometricVault.encryptCipher()
        } catch (e: Exception) {
            onResult(false); return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val c = result.cryptoObject?.cipher
                    if (c == null) { onResult(false); return }
                    val ok = runCatching {
                        BiometricVault.persist(activity, c, payload); true
                    }.getOrDefault(false)
                    onResult(ok)
                }

                override fun onAuthenticationError(code: Int, msg: CharSequence) = onResult(false)
                // A single non-match isn't terminal; the prompt keeps retrying.
                override fun onAuthenticationFailed() {}
            },
        )
        prompt.authenticate(promptInfo(title, subtitle, negative), BiometricPrompt.CryptoObject(cipher))
    }

    /** Show the prompt to UNLOCK with biometrics: a success authorises the
     *  decrypt cipher and yields the stored blob, or null if there's nothing to
     *  unlock with / the user cancelled. Runs on the main thread. */
    fun unlock(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negative: String,
        onResult: (ByteArray?) -> Unit,
    ) {
        val cipher = BiometricVault.decryptCipher(activity)
        if (cipher == null) { onResult(null); return }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val c = result.cryptoObject?.cipher
                    onResult(c?.let { BiometricVault.open(activity, it) })
                }

                override fun onAuthenticationError(code: Int, msg: CharSequence) = onResult(null)
                override fun onAuthenticationFailed() {}
            },
        )
        prompt.authenticate(promptInfo(title, subtitle, negative), BiometricPrompt.CryptoObject(cipher))
    }

    private fun promptInfo(title: String, subtitle: String, negative: String) =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negative)
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
}
