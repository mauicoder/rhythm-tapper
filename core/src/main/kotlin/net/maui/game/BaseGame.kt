package net.maui.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable


/**
 * Created when program is launched;
 * manages the screens that appear during the game.
 */
abstract class BaseGame : Game() {

    init {
        game = this
    }

    override fun create() {
        // prepare for multiple classes/stages/actors to receive discrete input
        val im = InputMultiplexer()
        Gdx.input.inputProcessor = im

        // parameters for generating a custom bitmap font
        val fontGenerator =
            FreeTypeFontGenerator(Gdx.files.internal("assets/Kirsty.ttf"))
        val fontParameters = FreeTypeFontParameter()
        fontParameters.size = 32
        fontParameters.color = Color.WHITE
        fontParameters.borderWidth = 2f
        fontParameters.borderColor = Color.BLACK
        fontParameters.borderStraight = true
        fontParameters.minFilter = TextureFilter.Linear
        fontParameters.magFilter = TextureFilter.Linear

        val customFont = fontGenerator.generateFont(fontParameters)

        labelStyle = LabelStyle()
        labelStyle!!.font = customFont

        textButtonStyle = TextButtonStyle()

        val buttonTex = Texture(Gdx.files.internal("assets/button.png"))
        val buttonPatch = NinePatch(buttonTex, 24, 24, 24, 24)
        textButtonStyle!!.up = NinePatchDrawable(buttonPatch)
        textButtonStyle!!.font = customFont
        textButtonStyle!!.fontColor = Color.GRAY
    }

    companion object {
        /**
         * Stores reference to game; used when calling setActiveScreen method.
         */
        private lateinit var game: BaseGame

        var labelStyle: LabelStyle? = null // BitmapFont + Color
        var textButtonStyle: TextButtonStyle? = null // NPD + BitmapFont + Color

        /**
         * Used to switch screens while game is running.
         * Method is static to simplify usage.
         */
        fun setActiveScreen(s: BaseScreen?) {
            game.setScreen(s)
        }
    }
}
