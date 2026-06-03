package app.rcq.android.data

/**
 * One local identity tied to one RCQ server. The same device can hold
 * several of these — different servers, different UINs, fully independent
 * contact lists and chat history. All per-account storage (SecureStore
 * keys, the MessageDb file, LocalStores/VisitStore prefixes) is keyed off
 * [id]. The Android analogue of the iOS `Account` struct.
 *
 * Sensitive fields (UIN, JWT, identity keys) DON'T live here — those stay
 * in [SecureStore] under this account's prefix. `Account` is just the
 * metadata needed to route storage reads, persisted as JSON by
 * [AccountManager].
 */
data class Account(
    /** Stable local UUID. Prefix for every per-account storage slot.
     *  Generated locally, never shared with the server (the server-side
     *  identity is the UIN, which lives in SecureStore under this prefix). */
    val id: String,

    /** Bare host this account is registered on (e.g. an org island), or
     *  null for the default public server — same "null means default"
     *  convention as [SecureStore.serverHost]. */
    val serverHost: String?,

    /** Wall-clock millis the account was added on this device. Used for
     *  stable ordering in the switcher (oldest first = primary identity). */
    val createdAt: Long,

    /** Optional human label for the switcher. When null the UI falls back
     *  to the live nickname peeked from SecureStore, then the host. */
    val displayLabel: String? = null,
)
