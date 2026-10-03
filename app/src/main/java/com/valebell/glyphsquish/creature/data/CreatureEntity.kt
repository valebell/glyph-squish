package com.valebell.glyphsquish.creature.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character")
data class CreatureEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val currentForm: String,
    val selectedMove: String,
    val revision: Long,
    val seenForms: String? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(tableName = "unlocked_moves")
data class UnlockedMoveEntity(
    @PrimaryKey
    val move: String,
)
