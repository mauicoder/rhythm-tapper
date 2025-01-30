package net.maui.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import games.spooky.gdx.nativefilechooser.NativeFileChooser
import games.spooky.gdx.nativefilechooser.NativeFileChooserCallback
import games.spooky.gdx.nativefilechooser.NativeFileChooserConfiguration
import net.maui.game.BaseGame
import net.maui.game.model.SongData
import java.io.FilenameFilter
import java.lang.Exception


class RecorderScreen(private val nativeFileChooser: NativeFileChooser) : BaseScreen() {
    lateinit var music: Music
    lateinit var songData: SongData
    var lastSongPosition: Float = 0f
    var recording: Boolean = false
    lateinit var loadButton: TextButton
    lateinit var recordButton: TextButton
    lateinit var saveButton: TextButton

    override fun initialize() {
        recording = false
        loadButton = TextButton("Load Music File", BaseGame.textButtonStyle)
        loadButton.addListener { e: Event ->
            if (!isTouchDownEvent(e)) return@addListener false
            val conf = nativeFileChooserConfiguration("ogg", "audio/*")

            nativeFileChooser.chooseFile(conf, object : NativeFileChooserCallback {
                override fun onCancellation() {
                }

                override fun onError(exception: Exception) {
                }

                override fun onFileChosen(file: FileHandle) {
                    music = Gdx.audio.newMusic(file)
                    songData = SongData()
                    songData.songName =  file.name()
                }

            })
            true
        }
        recordButton = TextButton("Record Keystrokes", BaseGame.textButtonStyle)
        recordButton.addListener { e: Event ->
            if (!isTouchDownEvent(e)) return@addListener false
            if (!recording) {
                music.play()
                recording = true
                lastSongPosition = 0f
            }
            true
        }
        saveButton = TextButton("Save Keystroke File", BaseGame.textButtonStyle)
        saveButton.addListener { e: Event ->
            if (!isTouchDownEvent(e)) return@addListener false
            val conf = nativeFileChooserConfiguration()
            nativeFileChooser.chooseFile(conf, object : NativeFileChooserCallback {
                override fun onCancellation() {
                }

                override fun onError(exception: Exception) {
                }

                override fun onFileChosen(file: FileHandle) {
                    songData.writeToFile(file)
                }
            })
            true
        }
        uiTable.add(loadButton)
        uiTable.row()
        uiTable.add(recordButton)
        uiTable.row()
        uiTable.add(saveButton)
    }

    private fun nativeFileChooserConfiguration(extension: String? = null, mimeType: String? = null): NativeFileChooserConfiguration {
        val conf = NativeFileChooserConfiguration()
        conf.directory = Gdx.files.absolute(System.getProperty("user.dir"));

        // Filter out all files which do not have the .ogg extension and are not of an audio MIME type - belt and braces
        mimeType?.let { conf.mimeFilter = mimeType }
        extension?.let { conf.nameFilter = FilenameFilter { _, name -> name.endsWith(extension) } }
        return conf
    }

    override fun update(dt: Float) {
        if ( recording )
        {
            if ( music.isPlaying)
                lastSongPosition = music.position
            else // song just finished
            {
                recording = false;
                songData.songDuration = lastSongPosition
            }
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        if (recording) {
            val key: String = Input.Keys.toString(keycode)
            val time = music.position
            songData.addKeyTime(key, time)
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }
}
