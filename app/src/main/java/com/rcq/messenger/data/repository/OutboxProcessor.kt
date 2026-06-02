package com.rcq.messenger.data.repository

import android.util.Log
import com.rcq.messenger.crypto.CryptoService
import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.data.api.SealedMessageRequest
import com.rcq.messenger.data.db.MessageDao
import com.rcq.messenger.data.db.PendingOutboxDao
import com.rcq.messenger.data.websocket.ConnectionState
import com.rcq.messenger.data.websocket.WebSocketService

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboxProcessor @Inject constructor(
    private val outboxDao: PendingOutboxDao,
    private val messageDao: MessageDao,
    private val api: RCQApiService,
    private val cryptoService: CryptoService,
    private val wsService: WebSocketService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "OutboxProcessor"

    init {
        observeConnection()
    }

    private fun observeConnection() {
        scope.launch {
            wsService.connectionState
                .map { it == ConnectionState.CONNECTED }
                .distinctUntilChanged()
                .filter { it }
                .collect { drain() }
        }
    }

    suspend fun drain() {
        val pending = outboxDao.getPending()
        if (pending.isEmpty()) return
        Log.d(tag, "Draining ${pending.size} pending messages")

        for (entry in pending) {
            try {
                val wrapped = cryptoService.encryptWrapped(
                    0L, entry.recipientUin, entry.plainContent, null
                )
                val response = api.sendSealedMessage(
                    SealedMessageRequest(
                        toUin = entry.recipientUin,
                        envelopeType = wrapped.envelopeType,
                        payload = wrapped.payload
                    )
                )
                if (response.isSuccessful) {
                    outboxDao.delete(entry)
                    messageDao.updateMessageStatus(entry.localId, "SENT")
                    Log.d(tag, "Drained ${entry.localId}")
                } else {
                    outboxDao.incrementRetry(entry.localId)
                    Log.w(tag, "Drain failed ${entry.localId}: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                outboxDao.incrementRetry(entry.localId)
                Log.w(tag, "Drain exception ${entry.localId}: ${e.message}")
            }
        }
    }
}
