package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class GameState(
    val gameId: String,
    val type: GameType,
    val status: GameStatus,
    val currentMultiplier: Double = 1.0,
    val crashPoint: Double? = null,
    val activeBets: List<Bet> = emptyList(),
    val recentResults: List<GameResult> = emptyList(),
    val lastUpdate: Long
)

@kotlinx.serialization.Serializable
enum class GameType {
    CRASH,
    HILO,
    ROULETTE,
    PET_HUNT,
    RPS,
    DICE
}

@kotlinx.serialization.Serializable
enum class GameStatus {
    WAITING,
    RUNNING,
    ENDED
}

@kotlinx.serialization.Serializable
data class Bet(
    val id: String,
    val gameId: String,
    val userId: Long,
    val amount: Long,
    val autoCashout: Double? = null,
    val cashedOut: Boolean = false,
    val cashoutMultiplier: Double? = null,
    val cashedOutAmount: Long? = null,
    val timestamp: Long
)

@kotlinx.serialization.Serializable
data class GameResult(
    val timestamp: Long,
    val result: String,
    val multiplier: Double? = null,
    val winnerIds: List<Long> = emptyList()
)