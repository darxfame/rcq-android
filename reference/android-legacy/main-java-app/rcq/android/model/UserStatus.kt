package app.rcq.android.model

import app.rcq.android.R

/**
 * Presence status, ported from the iOS `UserStatus`. `wire` is the
 * server string; `invisible` is a local-only choice that other contacts
 * see as `offline` (the server already maps it down in their view).
 * `label` is the English fallback; `labelRes` is the localized string.
 */
enum class UserStatus(val wire: String, val label: String, val labelRes: Int) {
    ONLINE("online", "Online", R.string.status_online),
    AWAY("away", "Away", R.string.status_away),
    DND("dnd", "Do Not Disturb", R.string.status_dnd),
    INVISIBLE("invisible", "Invisible", R.string.status_invisible),
    OFFLINE("offline", "Offline", R.string.status_offline);

    companion object {
        fun from(s: String?): UserStatus =
            entries.firstOrNull { it.wire == s } ?: OFFLINE
    }
}
