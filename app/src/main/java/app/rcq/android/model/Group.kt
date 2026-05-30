package app.rcq.android.model

/** A group member, mirrors the iOS RCQGroupMember / server GroupMemberOut. */
data class GroupMember(
    val uin: Int,
    val nickname: String,
    val role: String,            // owner | admin | member
    val status: String? = null,  // live presence
    val identityKey: String,     // base64 raw X25519 public — we encrypt to this
    val signingKey: String? = null,
) {
    val presence: UserStatus get() = UserStatus.from(status)
}

/**
 * A group, mirrors the iOS RCQGroup / server GroupOut. RCQ groups have no
 * group key — a group message is just a per-recipient fan-out of the same
 * v=1 sealed envelope used for 1:1 (rcq-spec 6.4). So all the model needs
 * is the roster (each member's identity key) plus settings.
 */
data class RcqGroup(
    val id: Int,
    val name: String,
    val description: String? = null,
    val ownerUin: Int,
    val postPolicy: String = "all",   // "all" | "owner_only"
    val isClosed: Boolean = false,
    val membersHidden: Boolean = false,
    val pinnedText: String? = null,
    val avatarMediaId: String? = null,
    val avatarMediaKey: String? = null,
    val members: List<GroupMember> = emptyList(),
    val createdAt: Long? = null,
) {
    /** Broadcast mode (owner_only) is enforced client-side; the server
     *  can't see who's posting under sealed sender. */
    fun canPost(ownUin: Int): Boolean = postPolicy != "owner_only" || ownUin == ownerUin

    fun memberName(uin: Int): String =
        members.firstOrNull { it.uin == uin }?.nickname ?: "$uin"
}
