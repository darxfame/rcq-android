package com.rcq.messenger.data.repository

import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.data.api.UserSettings
import com.rcq.messenger.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val api: RCQApiService
) {
    suspend fun getGameState(gameType: GameType): Result<GameState> = runCatching {
        api.getGameState(gameType).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to get game state")
        }
    }

    suspend fun placeBet(gameType: GameType, amount: Long, autoCashout: Double? = null): Result<Bet> = runCatching {
        val bet = Bet(
            id = java.util.UUID.randomUUID().toString(),
            gameId = "",
            userId = 0,
            amount = amount,
            autoCashout = autoCashout,
            timestamp = System.currentTimeMillis()
        )
        api.placeBet(gameType, bet).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to place bet")
        }
    }

    suspend fun cashout(gameType: GameType): Result<Bet> = runCatching {
        api.cashout(gameType).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to cashout")
        }
    }
}

@Singleton
class MarketplaceRepository @Inject constructor(
    private val api: RCQApiService
) {
    suspend fun getItems(category: MarketplaceCategory? = null): Result<List<MarketplaceItem>> = runCatching {
        api.getMarketplaceItems(category).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to get items")
        }
    }

    suspend fun getItem(itemId: String): Result<MarketplaceItem> = runCatching {
        api.getItem(itemId).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Item not found")
        }
    }

    suspend fun createListing(item: MarketplaceItem): Result<MarketplaceItem> = runCatching {
        api.createListing(item).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to create listing")
        }
    }

    suspend fun buyItem(itemId: String): Result<Unit> = runCatching {
        api.buyItem(itemId).let { response ->
            if (!response.isSuccessful) throw Exception("Failed to buy item")
        }
    }

    suspend fun placeBid(itemId: String, amount: Long): Result<Bid> = runCatching {
        val bid = Bid(
            id = java.util.UUID.randomUUID().toString(),
            itemId = itemId,
            bidderId = 0,
            bidderNickname = "",
            amount = amount,
            createdAt = System.currentTimeMillis()
        )
        api.placeBid(itemId, bid).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to place bid")
        }
    }
}

@Singleton
class PetRepository @Inject constructor(
    private val api: RCQApiService,
    private val petDao: com.rcq.messenger.data.db.PetDao
) {
    fun getUserPets() = petDao.getAllPets()

    suspend fun syncPets(): Result<Unit> = runCatching {
        api.getUserPets().let { response ->
            if (response.isSuccessful) {
                petDao.insertPets(response.body()!!.map { it.toEntity() })
            }
        }
    }

    suspend fun equipPet(petId: String, userId: Long): Result<Unit> = runCatching {
        api.equipPet(petId).let { response ->
            if (response.isSuccessful) petDao.equipPet(petId, userId)
            else throw Exception("Failed to equip pet")
        }
    }

    suspend fun unequipPet(petId: String): Result<Unit> = runCatching {
        api.unequipPet(petId).let { response ->
            if (response.isSuccessful) petDao.unequipPet(petId)
            else throw Exception("Failed to unequip pet")
        }
    }
}

private fun Pet.toEntity() = com.rcq.messenger.data.db.PetEntity(
    id = id, name = name, type = type.name, rarity = rarity.name,
    imageUrl = imageUrl, equippedBy = equippedBy,
    isForSale = isForSale, salePrice = salePrice
)

@Singleton
class NearbyRepository @Inject constructor(
    private val api: RCQApiService
) {
    suspend fun getNearbyUsers(lat: Double, lon: Double, radius: Int = 1000): Result<List<User>> = runCatching {
        api.getNearbyUsers(lat, lon, radius).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to get nearby users")
        }
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    private val api: RCQApiService
) {
    suspend fun getSettings(): Result<UserSettings> = runCatching {
        api.getSettings().let { response ->
            if (response.isSuccessful) response.body()!!
            else UserSettings()
        }
    }

    suspend fun updateSettings(settings: UserSettings): Result<UserSettings> = runCatching {
        api.updateSettings(settings).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to update settings")
        }
    }
}