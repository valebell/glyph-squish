package com.valebell.glyphsquish.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.valebell.glyphsquish.creature.domain.CreatureForm

/** Changes only launcher aliases; MainActivity and the Glyph Toy service stay enabled. */
internal class LauncherIconController(
    context: Context,
) {
    private val packageManager = context.packageManager
    private val packageName = context.packageName

    fun select(form: CreatureForm) {
        val changes =
            CreatureForm.entries.mapNotNull { candidate ->
                val component = component(candidate)
                val setting = packageManager.getComponentEnabledSetting(component)
                val enabled =
                    setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                        (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && candidate == CreatureForm.BLOB)
                val shouldEnable = candidate == form
                if (enabled == shouldEnable) {
                    null
                } else {
                    PackageManager.ComponentEnabledSetting(
                        component,
                        if (shouldEnable) {
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        },
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }
        // API 33+ applies the switch atomically, so there is always one launcher entry.
        if (changes.isNotEmpty()) packageManager.setComponentEnabledSettings(changes)
    }

    internal fun component(form: CreatureForm): ComponentName {
        val alias =
            when (form) {
                CreatureForm.BLOB -> "BlobLauncher"
                CreatureForm.SHARK -> "SharkLauncher"
                CreatureForm.CAT -> "CatLauncher"
                CreatureForm.STAR -> "StarLauncher"
                CreatureForm.BUTTERFLY -> "ButterflyLauncher"
            }
        return ComponentName(packageName, "$packageName.launcher.$alias")
    }
}
