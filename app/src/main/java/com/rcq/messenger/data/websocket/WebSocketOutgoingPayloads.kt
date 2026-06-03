package com.rcq.messenger.data.websocket

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object WebSocketOutgoingPayloads {
    private val json = Json { encodeDefaults = true }

    fun typing(toUin: Long, active: Boolean): String =
        json.encodeToString(JsonObject.serializer(), buildJsonObject {
            put("type", "typing")
            put("to_uin", toUin)
            put("active", active)
        })
}
