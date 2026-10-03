package com.valebell.glyphsquish.launcher

import com.valebell.glyphsquish.creature.domain.CreatureForm

/** Disabling a launch alias can finish its task, even with DONT_KILL_APP. */
internal class LauncherIconSync(
    private val applyIcon: (CreatureForm) -> Unit,
) {
    private val visibleCompanions = mutableSetOf<Any>()
    private var awaitingRecreation = false
    private var glyphActive = false
    private var pendingForm: CreatureForm? = null

    fun request(form: CreatureForm) {
        pendingForm = form
        applyWhenIdle()
    }

    fun companionStarted(owner: Any) {
        visibleCompanions += owner
        awaitingRecreation = false
    }

    fun companionStopped(
        owner: Any,
        changingConfigurations: Boolean,
    ) {
        visibleCompanions -= owner
        awaitingRecreation = changingConfigurations
        applyWhenIdle()
    }

    fun setGlyphActive(active: Boolean) {
        glyphActive = active
        applyWhenIdle()
    }

    private fun applyWhenIdle() {
        if (visibleCompanions.isNotEmpty() || awaitingRecreation || glyphActive) return
        val form = pendingForm ?: return
        applyIcon(form)
        pendingForm = null
    }
}
