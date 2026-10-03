package com.valebell.glyphsquish.creature.domain

import com.valebell.glyphsquish.slime.SlimeButtonAction

enum class CreatureForm {
    BLOB,
    SHARK,
    CAT,
    STAR,
    BUTTERFLY,
}

object CreatureForms {
    fun requiredMove(form: CreatureForm): SlimeButtonAction? =
        when (form) {
            CreatureForm.BLOB -> null
            CreatureForm.SHARK -> SlimeButtonAction.RICOCHET
            CreatureForm.CAT -> SlimeButtonAction.ECHO
            CreatureForm.STAR -> SlimeButtonAction.THUNDERBOLT
            CreatureForm.BUTTERFLY -> SlimeButtonAction.HELIX
        }

    fun availableFor(unlockedMoves: Set<SlimeButtonAction>): Set<CreatureForm> =
        CreatureForm.entries
            .filterTo(linkedSetOf()) { form ->
                requiredMove(form)?.let(unlockedMoves::contains) != false
            }

    fun nextUnlocked(
        current: CreatureForm,
        unlockedMoves: Set<SlimeButtonAction>,
    ): CreatureForm {
        val available = availableFor(unlockedMoves).toList()
        if (available.size <= 1) return current
        val currentIndex = available.indexOf(current).coerceAtLeast(0)
        return available[(currentIndex + 1) % available.size]
    }
}
