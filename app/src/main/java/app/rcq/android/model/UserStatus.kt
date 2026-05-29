package app.rcq.android.model

/**
 * Presence status, ported from the iOS `UserStatus`. `wire` is the
 * server string; `invisible` is a local-only choice that other contacts
 * see as `offline` (the server already maps it down in their view).
 */
enum class UserStatus(val wire: String, val label: String) {
    ONLINE("online", "Online"),
    AWAY("away", "Away"),
    DND("dnd", "Do Not Disturb"),
    INVISIBLE("invisible", "Invisible"),
    OFFLINE("offline", "Offline");

    companion object {
        fun from(s: String?): UserStatus =
            entries.firstOrNull { it.wire == s } ?: OFFLINE
    }
}
