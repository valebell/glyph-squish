package com.valebell.glyphsquish.creature.data

import androidx.room.withTransaction
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureForms
import com.valebell.glyphsquish.creature.domain.CreatureProgression
import com.valebell.glyphsquish.creature.domain.CreatureRepository
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.slime.SlimeButtonAction
import kotlin.enums.enumEntries

class RoomCreatureRepository(
    private val database: SquishDatabase,
) : CreatureRepository {
    private val dao = database.creatureDao()

    override suspend fun loadOrCreate(): CreatureState =
        database.withTransaction {
            val stored = dao.loadCharacter()
            if (stored != null) {
                return@withTransaction stored.toDomain(
                    dao.loadUnlockedMoves().mapTo(linkedSetOf()) { move ->
                        enumValue<SlimeButtonAction>(move.move, "unlocked move")
                    },
                )
            }
            CreatureProgression.initialState().also { saveState(it) }
        }

    override suspend fun save(state: CreatureState) {
        database.withTransaction {
            val storedRevision = dao.loadCharacter()?.revision
            if (storedRevision != null) {
                check(state.revision >= storedRevision) {
                    "Cannot replace revision $storedRevision with stale revision ${state.revision}"
                }
            }
            saveState(state)
        }
    }

    private suspend fun saveState(state: CreatureState) {
        dao.saveCharacter(
            CreatureEntity(
                currentForm = state.currentForm.name,
                selectedMove = state.selectedMove.name,
                revision = state.revision,
                seenForms = state.seenForms.joinToString(",") { it.name },
            ),
        )
        val desiredMoves = state.unlockedMoves.mapTo(linkedSetOf()) { it.name }
        val storedMoves = dao.loadUnlockedMoves().mapTo(hashSetOf()) { it.move }
        if (storedMoves != desiredMoves) {
            dao.clearUnlockedMoves()
            dao.saveUnlockedMoves(desiredMoves.map(::UnlockedMoveEntity))
        }
    }

    private fun CreatureEntity.toDomain(unlockedMoves: Set<SlimeButtonAction>): CreatureState =
        CreatureState(
            currentForm = creatureFormValue(currentForm),
            selectedMove = enumValue(selectedMove, "selected move"),
            unlockedMoves = unlockedMoves,
            revision = revision,
            seenForms =
                seenForms?.split(",")?.filter { it.isNotEmpty() }?.mapTo(linkedSetOf(), ::creatureFormValue)
                    ?: CreatureForms.availableFor(unlockedMoves),
        )
}

private fun creatureFormValue(value: String): CreatureForm =
    if (value == LEGACY_CLOVER_FORM) {
        CreatureForm.BUTTERFLY
    } else {
        enumValue(value, "current form")
    }

private inline fun <reified T : Enum<T>> enumValue(
    value: String,
    field: String,
): T =
    enumEntries<T>().firstOrNull { entry -> entry.name == value }
        ?: error("Unknown $field '$value'")

private const val LEGACY_CLOVER_FORM = "CLOVER"
