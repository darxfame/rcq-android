package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceItem(
    val id: String,
    val sellerId: Long,
    val sellerNickname: String,
    val title: String,
    val description: String,
    val price: Long,
    val currency: String = "TOKENS",
    val category: MarketplaceCategory,
    val mediaUrls: List<String> = emptyList(),
    val status: ItemStatus = ItemStatus.ACTIVE,
    val views: Int = 0,
    val createdAt: Long,
    val expiresAt: Long? = null
)

@Serializable
enum class MarketplaceCategory {
    AVATARS,
    PETS,
    FRAMES,
    BADGES,
    NICKNAMES,
    STICKERS,
    THEMES,
    OTHER
}

@Serializable
enum class ItemStatus {
    ACTIVE,
    SOLD,
    EXPIRED,
    REMOVED
}

@Serializable
data class Bid(
    val id: String,
    val itemId: String,
    val bidderId: Long,
    val bidderNickname: String,
    val amount: Long,
    val createdAt: Long,
    val isWinning: Boolean = false
)