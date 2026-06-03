package app.rcq.android.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/** Walk the context-wrapper chain to the hosting [FragmentActivity] (needed to
 *  host a BiometricPrompt), or null if there isn't one. */
internal fun Context.findFragmentActivity(): FragmentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}
