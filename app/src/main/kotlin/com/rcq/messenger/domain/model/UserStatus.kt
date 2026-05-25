package com.rcq.messenger.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User presence status — synchronized with iOS via WebSocket and presence API.
 * 
 * Mapping from iOS:
 * - online → ONLINE
 * - away → AWAY
 * - dnd → DND (Do Not Disturb)
 * - invisible → INVISIBLE
 * - offline → OFFLINE
 */
@Serializable
enum class UserStatus {
    @SerialName("online")
    ONLINE,
    
    @SerialName("away")
    AWAY,
    
    @SerialName("dnd")
    DND,
    
    @SerialName("invisible")
    INVISIBLE,
    
    @SerialName("offline")
    OFFLINE;
    
    fun toDisplayString(): String = when (this) {
        ONLINE -> "Online"
        AWAY -> "Away"
        DND -> "Do Not Disturb"
        INVISIBLE -> "Invisible"
        OFFLINE -> "Offline"
    }
}
