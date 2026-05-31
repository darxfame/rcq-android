package app.rcq.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.R
import app.rcq.android.Session
import app.rcq.android.security.PanicPinService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen PIN entry shown whenever the app is locked (panic-PIN). A correct
 * real PIN flips [PanicPinService.locked] to false, which recomposes the host
 * away from this screen and lets [Session.start] reopen the message DB under the
 * unlocked key. Wrong attempts escalate into a lockout countdown.
 */
@Composable
fun PinLockScreen(session: Session, onWiped: () -> Unit = {}, onAccountChanged: (Int) -> Unit = {}) {
    val c = RcqTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var lockedOutUntil by remember { mutableStateOf(PanicPinService.lockedOutUntil(context)) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lockedOutUntil) {
        while (lockedOutUntil != null && lockedOutUntil!! > System.currentTimeMillis()) {
            nowMs = System.currentTimeMillis()
            delay(500)
        }
        lockedOutUntil = null
    }
    val remainingSec: Long? = lockedOutUntil?.let { ((it - nowMs) / 1000 + 1).coerceAtLeast(0) }
    val canSubmit = pin.length >= 4 && remainingSec == null && !busy

    fun submit() {
        if (!canSubmit) return
        busy = true
        error = null
        scope.launch {
            val res = withContext(Dispatchers.Default) { PanicPinService.submit(context, pin) }
            busy = false
            when (res) {
                PanicPinService.SubmitResult.REAL -> {
                    // Real PIN reveals everything: make sure decoy mode is off.
                    app.rcq.android.data.AccountManager.exitDecoyMode()
                    // host recomposes away
                }
                PanicPinService.SubmitResult.DECOY -> {
                    // Switch to the decoy account + hide the rest, THEN unlock.
                    busy = true
                    withContext(Dispatchers.Default) { session.applyDecoyUnlock() }
                    // The active account changed — update the host's uin so the
                    // header shows the decoy's number, not the hidden account's.
                    session.uin?.let { onAccountChanged(it) }
                }
                PanicPinService.SubmitResult.WIPE -> {
                    // Duress wipe: erase everything, then drop to onboarding.
                    // No error shown — it silently resets to a fresh install.
                    busy = true
                    withContext(Dispatchers.Default) { session.wipeEverything() }
                    onWiped()
                }
                PanicPinService.SubmitResult.WRONG -> {
                    error = context.getString(R.string.pin_wrong)
                    pin = ""
                    lockedOutUntil = PanicPinService.lockedOutUntil(context)
                }
                PanicPinService.SubmitResult.LOCKED_OUT -> {
                    pin = ""
                    lockedOutUntil = PanicPinService.lockedOutUntil(context)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Lock, null, tint = c.accent, modifier = Modifier.size(48.dp))
        Text(
            stringResource(R.string.pin_lock_title),
            color = c.textPrimary,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(R.string.pin_lock_subtitle),
            color = c.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 12 && it.all { ch -> ch.isDigit() }) pin = it },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            enabled = remainingSec == null && !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        if (remainingSec != null) {
            Text(
                stringResource(R.string.pin_locked_out, remainingSec),
                color = c.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        error?.let {
            Text(it, color = Color(0xFFE5484D), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        }
        CapsuleButton(
            label = if (busy) stringResource(R.string.pin_busy) else stringResource(R.string.pin_unlock),
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) { submit() }
    }
}
