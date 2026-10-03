package com.valebell.glyphsquish.launcher

import com.valebell.glyphsquish.creature.domain.CreatureForm
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherIconSyncTest {
    private val applied = mutableListOf<CreatureForm>()
    private val sync = LauncherIconSync(applied::add)

    @Test
    fun visibleCompanionCoalescesChangesUntilItStops() {
        val activity = Any()
        sync.companionStarted(activity)
        CreatureForm.entries.forEach(sync::request)
        assertEquals(emptyList<CreatureForm>(), applied)
        sync.companionStopped(activity, changingConfigurations = false)
        assertEquals(listOf(CreatureForm.BUTTERFLY), applied)
    }

    @Test
    fun rotationKeepsTheCurrentAliasUntilTheRecreatedActivityStops() {
        val oldActivity = Any()
        val newActivity = Any()
        sync.companionStarted(oldActivity)
        sync.request(CreatureForm.CAT)
        sync.companionStopped(oldActivity, changingConfigurations = true)
        sync.request(CreatureForm.STAR)
        assertEquals(emptyList<CreatureForm>(), applied)
        sync.companionStarted(newActivity)
        assertEquals(emptyList<CreatureForm>(), applied)
        sync.companionStopped(newActivity, changingConfigurations = false)
        assertEquals(listOf(CreatureForm.STAR), applied)
    }

    @Test
    fun overlappingActivitiesDoNotReleaseTheIconEarly() {
        val first = Any()
        val second = Any()
        sync.companionStarted(first)
        sync.companionStarted(second)
        sync.request(CreatureForm.SHARK)
        sync.companionStopped(first, changingConfigurations = false)
        assertEquals(emptyList<CreatureForm>(), applied)
        sync.companionStopped(second, changingConfigurations = false)
        assertEquals(listOf(CreatureForm.SHARK), applied)
    }

    @Test
    fun glyphSessionBlocksUpdatesEvenAfterCompanionCloses() {
        val activity = Any()
        sync.companionStarted(activity)
        sync.setGlyphActive(true)
        sync.request(CreatureForm.SHARK)
        sync.companionStopped(activity, changingConfigurations = false)
        sync.request(CreatureForm.CAT)
        assertEquals(emptyList<CreatureForm>(), applied)
        sync.setGlyphActive(false)
        assertEquals(listOf(CreatureForm.CAT), applied)
    }

    @Test
    fun companionStillBlocksUpdatesAfterGlyphUnbinds() {
        val activity = Any()
        sync.setGlyphActive(true)
        sync.companionStarted(activity)
        sync.request(CreatureForm.STAR)
        sync.setGlyphActive(false)
        assertEquals(emptyList<CreatureForm>(), applied)
        sync.companionStopped(activity, changingConfigurations = false)
        assertEquals(listOf(CreatureForm.STAR), applied)
    }

    @Test
    fun idleStartupRestoresTheSavedFormAndDoesNotReplayFlushedRequests() {
        sync.request(CreatureForm.CAT)
        sync.setGlyphActive(false)
        assertEquals(listOf(CreatureForm.CAT), applied)
    }
}
