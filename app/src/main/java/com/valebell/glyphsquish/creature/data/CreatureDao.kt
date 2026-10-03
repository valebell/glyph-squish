package com.valebell.glyphsquish.creature.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CreatureDao {
    @Query("SELECT * FROM character WHERE id = 1")
    suspend fun loadCharacter(): CreatureEntity?

    @Query("SELECT * FROM unlocked_moves ORDER BY move")
    suspend fun loadUnlockedMoves(): List<UnlockedMoveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCharacter(character: CreatureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUnlockedMoves(moves: List<UnlockedMoveEntity>)

    @Query("DELETE FROM unlocked_moves")
    suspend fun clearUnlockedMoves()
}
