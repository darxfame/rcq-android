package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Pet(
    val id: String,
    val name: String,
    val type: PetType,
    val rarity: PetRarity,
    val imageUrl: String,
    val animationUrls: Map<String, String> = emptyMap(),
    val stats: PetStats = PetStats(),
    val equippedBy: Long? = null,
    val isForSale: Boolean = false,
    val salePrice: Long? = null
)

@Serializable
enum class PetType {
    CAT, DOG, BIRD, FISH, RABBIT, DRAGON, UNICORN, PHOENIX, ROBOT, GHOST
}

@Serializable
enum class PetRarity {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
}

@Serializable
data class PetStats(
    val health: Int = 100,
    val happiness: Int = 100,
    val hunger: Int = 100,
    val energy: Int = 100
)

@Serializable
data class EquippedPet(
    val petId: String,
    val petType: PetType,
    val imageUrl: String,
    val position: PetPosition = PetPosition.BOTTOM_RIGHT
)

@Serializable
enum class PetPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}