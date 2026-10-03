package com.valebell.glyphsquish

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.valebell.glyphsquish.creature.data.RoomCreatureRepository
import com.valebell.glyphsquish.creature.data.SquishDatabase
import com.valebell.glyphsquish.creature.runtime.CreatureRuntime
import com.valebell.glyphsquish.creature.runtime.CreatureRuntimeState
import com.valebell.glyphsquish.launcher.LauncherIconController
import com.valebell.glyphsquish.launcher.LauncherIconSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SquishApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var creatureRuntime: CreatureRuntime
        private set

    internal lateinit var launcherIconSync: LauncherIconSync
        private set

    override fun onCreate() {
        super.onCreate()
        val database =
            Room
                .databaseBuilder(
                    applicationContext,
                    SquishDatabase::class.java,
                    DATABASE_NAME,
                ).build()
        creatureRuntime =
            CreatureRuntime(
                repository = RoomCreatureRepository(database),
                scope = applicationScope,
            )
        val launcherIcon = LauncherIconController(this)
        launcherIconSync = LauncherIconSync(launcherIcon::select)
        applicationScope.launch {
            creatureRuntime.state
                .filterIsInstance<CreatureRuntimeState.Ready>()
                .map { it.creature.currentForm }
                .distinctUntilChanged()
                .collect(launcherIconSync::request)
        }
    }

    private companion object {
        const val DATABASE_NAME = "squish-character.db"
    }
}

fun Context.creatureRuntime(): CreatureRuntime = (applicationContext as SquishApplication).creatureRuntime

internal fun Context.launcherIconSync(): LauncherIconSync = (applicationContext as SquishApplication).launcherIconSync
