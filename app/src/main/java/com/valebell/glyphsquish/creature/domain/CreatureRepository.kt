package com.valebell.glyphsquish.creature.domain

interface CreatureRepository {
    suspend fun loadOrCreate(): CreatureState

    suspend fun save(state: CreatureState)
}
