package com.valebell.glyphsquish

import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.slime.SlimeButtonAction

internal fun CreatureForm.nameResource(): Int =
    when (this) {
        CreatureForm.BLOB -> R.string.form_blob
        CreatureForm.SHARK -> R.string.form_shark
        CreatureForm.CAT -> R.string.form_cat
        CreatureForm.STAR -> R.string.form_star
        CreatureForm.BUTTERFLY -> R.string.form_butterfly
    }

internal fun SlimeButtonAction.nameResource(): Int =
    when (this) {
        SlimeButtonAction.BURST -> R.string.button_action_burst
        SlimeButtonAction.HELIX -> R.string.button_action_helix
        SlimeButtonAction.ECHO -> R.string.button_action_echo
        SlimeButtonAction.RICOCHET -> R.string.button_action_ricochet
        SlimeButtonAction.THUNDERBOLT -> R.string.button_action_thunderbolt
    }

internal fun SlimeButtonAction.descriptionResource(): Int =
    when (this) {
        SlimeButtonAction.BURST -> R.string.button_action_burst_description
        SlimeButtonAction.HELIX -> R.string.button_action_helix_description
        SlimeButtonAction.ECHO -> R.string.button_action_echo_description
        SlimeButtonAction.RICOCHET -> R.string.button_action_ricochet_description
        SlimeButtonAction.THUNDERBOLT -> R.string.button_action_thunderbolt_description
    }

internal fun SlimeButtonAction.recipeResource(): Int? =
    when (this) {
        SlimeButtonAction.BURST -> null
        SlimeButtonAction.HELIX -> R.string.button_action_helix_recipe
        SlimeButtonAction.ECHO -> R.string.button_action_echo_recipe
        SlimeButtonAction.RICOCHET -> R.string.button_action_ricochet_recipe
        SlimeButtonAction.THUNDERBOLT -> R.string.button_action_thunderbolt_recipe
    }
