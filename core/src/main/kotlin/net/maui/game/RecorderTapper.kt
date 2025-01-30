package net.maui.game

import games.spooky.gdx.nativefilechooser.NativeFileChooser
import net.maui.game.screen.RecorderScreen



/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms. */
class RecorderTapper(private val nativeFileChooser: NativeFileChooser) : BaseGame() {
    override fun create() {
        super.create()
        setActiveScreen(RecorderScreen(nativeFileChooser))
    }
}
