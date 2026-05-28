package com.rcq.messenger.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val rarity: String,
    val imageUrl: String,
    val equippedBy: Long? = null,
    val isForSale: Boolean = false,
    val salePrice: Long? = null
)
