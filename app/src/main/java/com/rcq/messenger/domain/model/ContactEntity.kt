package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val status: String = "OFFLINE",
    val lastSeen: String? = null,
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val notificationSound: String? = null,
    val customNickname: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val identityKey: String? = null,
    val signingKey: String? = null,
    val signalIdentityKey: String? = null,
    val statusMessage: String? = null,
    // true  = явно добавлен в друзья (из GET /contacts или ContactResponse accepted).
    // false = участник группы, кэшированный только для Signal E2EE — не показывается в UI.
    val isContact: Boolean = false
)
