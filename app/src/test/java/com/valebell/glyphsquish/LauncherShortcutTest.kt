package com.valebell.glyphsquish

import com.valebell.glyphsquish.ui.home.SquishSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherShortcutTest {
    @Test
    fun onlyKnownShortcutActionsChooseAGallery() {
        assertEquals(SquishSection.FORMS, sectionForShortcut("com.valebell.glyphsquish.OPEN_FORMS"))
        assertEquals(SquishSection.MOVES, sectionForShortcut("com.valebell.glyphsquish.OPEN_MOVES"))
        assertNull(sectionForShortcut(null))
        assertNull(sectionForShortcut("android.intent.action.MAIN"))
        assertNull(sectionForShortcut("com.valebell.glyphsquish.OPEN_SETTINGS"))
    }
}
