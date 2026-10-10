package com.valebell.glyphsquish.slime

import android.content.ComponentName
import com.nothing.ketchum.GlyphMatrixManager
import java.lang.ref.WeakReference

// GlyphMatrixManager keeps its Callback after unInit() (verified from the bundled SDK
// bytecode), so a direct callback would let the process-wide singleton retain a
// destroyed service. This bridge only holds the session weakly and detaches on release.
class GlyphMatrixCallbackBridge(
    session: GlyphMatrixSessionCallbacks,
) : GlyphMatrixManager.Callback {
    private val sessionReference = WeakReference(session)

    override fun onServiceConnected(componentName: ComponentName?) {
        sessionReference.get()?.onGlyphServiceConnected()
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        sessionReference.get()?.onGlyphServiceDisconnected()
    }

    fun detach() {
        sessionReference.clear()
    }
}

interface GlyphMatrixSessionCallbacks {
    fun onGlyphServiceConnected()

    fun onGlyphServiceDisconnected()
}
