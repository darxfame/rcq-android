package com.rcq.messenger.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.rcq.messenger.domain.model.PetEntity

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getPets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun getPet(id: String): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPets(pets: List<PetEntity>)

    @Delete
    suspend fun deletePet(pet: PetEntity)

    @Query("UPDATE pets SET equippedBy = :userId WHERE id = :petId")
    suspend fun equipPet(petId: String, userId: Long)

    @Query("UPDATE pets SET equippedBy = NULL WHERE id = :petId")
    suspend fun unequipPet(petId: String)
}
