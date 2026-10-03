package com.valebell.glyphsquish.creature.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CreatureEntity::class,
        UnlockedMoveEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SquishDatabase : RoomDatabase() {
    abstract fun creatureDao(): CreatureDao
}
