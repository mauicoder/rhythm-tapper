@file:JvmName("Lwjgl3Launcher")

package net.maui.game.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import games.spooky.gdx.nativefilechooser.desktop.DesktopFileChooser
import net.maui.game.RecorderTapper
import net.maui.game.RhythmTapper

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
      return
    Lwjgl3Application(RecorderTapper(DesktopFileChooser()), Lwjgl3ApplicationConfiguration().apply {
        setTitle("Rhythm Tapper")
        setWindowedMode(640, 480)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    })
}
