package net.maui.game.actor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage


/**
 * Extends functionality of the LibGDX Actor class.
 * by adding support for textures/animation,
 * collision polygons, movement, world boundaries, and camera scrolling.
 * Most game objects should extend this class; lists of extensions can be retrieved by stage and class name.
 * @see .Actor
 *
 * @author Lee Stemkoski
 */
class BaseActor(x: Float, y: Float, s: Stage) : Group() {
    private var animation: Animation<TextureRegion>?
    private var elapsedTime: Float
    private var animationPaused: Boolean

    private val velocityVec: Vector2
    private val accelerationVec: Vector2
    private var acceleration: Float
    private var maxSpeed: Float
    private var deceleration: Float

    private var boundaryPolygon: Polygon?

    init {
        // call constructor from Actor class
        // perform additional initialization tasks
        setPosition(x, y)
        s.addActor(this)

        // initialize animation data
        animation = null
        elapsedTime = 0f
        animationPaused = false

        // initialize physics data
        velocityVec = Vector2(0f, 0f)
        accelerationVec = Vector2(0f, 0f)
        acceleration = 0f
        maxSpeed = 1000f
        deceleration = 0f

        boundaryPolygon = null
    }

    /**
     * If this object moves completely past the world bounds,
     * adjust its position to the opposite side of the world.
     */
    fun wrapAroundWorld() {
        if (x + width < 0) x = worldBounds!!.width

        if (x > worldBounds!!.width) x = -width

        if (y + height < 0) y = worldBounds!!.height

        if (y > worldBounds!!.height) y = -height
    }

    /**
     * Align center of actor at given position coordinates.
     * @param x x-coordinate to center at
     * @param y y-coordinate to center at
     */
    fun centerAtPosition(x: Float, y: Float) {
        setPosition(x - width / 2, y - height / 2)
    }

    /**
     * Repositions this BaseActor so its center is aligned
     * with center of other BaseActor. Useful when one BaseActor spawns another.
     * @param other BaseActor to align this BaseActor with
     */
    fun centerAtActor(other: BaseActor) {
        centerAtPosition(other.x + other.width / 2, other.y + other.height / 2)
    }

    // ----------------------------------------------
    // Animation methods
    // ----------------------------------------------
    /**
     * Sets the animation used when rendering this actor; also sets actor size.
     * @param anim animation that will be drawn when actor is rendered
     */
    fun setAnimation(anim: Animation<TextureRegion>?) {
        animation = anim
        val tr = animation!!.getKeyFrame(0f)
        val w = tr.regionWidth.toFloat()
        val h = tr.regionHeight.toFloat()
        setSize(w, h)
        setOrigin(w / 2, h / 2)

        if (boundaryPolygon == null) setBoundaryRectangle()
    }

    /**
     * Creates an animation from images stored in separate files.
     * @param fileNames array of names of files containing animation images
     * @param frameDuration how long each frame should be displayed
     * @param loop should the animation loop
     * @return animation created (useful for storing multiple animations)
     */
    fun loadAnimationFromFiles(
        fileNames: Array<String?>,
        frameDuration: Float,
        loop: Boolean
    ): Animation<TextureRegion> {
        val fileCount = fileNames.size
        val textureArray = com.badlogic.gdx.utils.Array<TextureRegion>()

        for (n in 0 until fileCount) {
            val fileName = fileNames[n]
            val texture = Texture(Gdx.files.internal(fileName))
            texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
            textureArray.add(TextureRegion(texture))
        }

        val anim = Animation(frameDuration, textureArray)

        if (loop) anim.setPlayMode(Animation.PlayMode.LOOP)
        else anim.setPlayMode(Animation.PlayMode.NORMAL)

        if (animation == null) setAnimation(anim)

        return anim
    }

    /**
     * Creates an animation from a spritesheet: a rectangular grid of images stored in a single file.
     * @param fileName name of file containing spritesheet
     * @param rows number of rows of images in spritesheet
     * @param cols number of columns of images in spritesheet
     * @param frameDuration how long each frame should be displayed
     * @param loop should the animation loop
     * @return animation created (useful for storing multiple animations)
     */
    fun loadAnimationFromSheet(
        fileName: String?,
        rows: Int,
        cols: Int,
        frameDuration: Float,
        loop: Boolean
    ): Animation<TextureRegion> {
        val texture = Texture(Gdx.files.internal(fileName), true)
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
        val frameWidth = texture.width / cols
        val frameHeight = texture.height / rows

        val temp = TextureRegion.split(texture, frameWidth, frameHeight)

        val textureArray = com.badlogic.gdx.utils.Array<TextureRegion>()

        for (r in 0 until rows) for (c in 0 until cols) textureArray.add(temp[r][c])

        val anim = Animation(frameDuration, textureArray)

        if (loop) anim.setPlayMode(Animation.PlayMode.LOOP)
        else anim.setPlayMode(Animation.PlayMode.NORMAL)

        if (animation == null) setAnimation(anim)

        return anim
    }

    /**
     * Convenience method for creating a 1-frame animation from a single texture.
     * @param fileName names of image file
     * @return animation created (useful for storing multiple animations)
     */
    fun loadTexture(fileName: String?): Animation<TextureRegion> {
        val fileNames = arrayOfNulls<String>(1)
        fileNames[0] = fileName
        return loadAnimationFromFiles(fileNames, 1f, true)
    }

    /**
     * Set the pause state of the animation.
     * @param pause true to pause animation, false to resume animation
     */
    fun setAnimationPaused(pause: Boolean) {
        animationPaused = pause
    }

    val isAnimationFinished: Boolean
        /**
         * Checks if animation is complete: if play mode is normal (not looping)
         * and elapsed time is greater than time corresponding to last frame.
         * @return
         */
        get() = animation!!.isAnimationFinished(elapsedTime)

    /**
     * Sets the opacity of this actor.
     * @param opacity value from 0 (transparent) to 1 (opaque)
     */
    fun setOpacity(opacity: Float) {
        color.a = opacity
    }

    // ----------------------------------------------
    // physics/motion methods
    // ----------------------------------------------
    /**
     * Set acceleration of this object.
     * @param acc Acceleration in (pixels/second) per second.
     */
    fun setAcceleration(acc: Float) {
        acceleration = acc
    }

    /**
     * Set deceleration of this object.
     * Deceleration is only applied when object is not accelerating.
     * @param dec Deceleration in (pixels/second) per second.
     */
    fun setDeceleration(dec: Float) {
        deceleration = dec
    }

    /**
     * Set maximum speed of this object.
     * @param ms Maximum speed of this object in (pixels/second).
     */
    fun setMaxSpeed(ms: Float) {
        maxSpeed = ms
    }

    var speed: Float
        /**
         * Calculates the speed of movement (in pixels/second).
         * @return speed of movement (pixels/second)
         */
        get() = velocityVec.len()
        /**
         * Set the speed of movement (in pixels/second) in current direction.
         * If current speed is zero (direction is undefined), direction will be set to 0 degrees.
         * @param speed of movement (pixels/second)
         */
        set(speed) {
            // if length is zero, then assume motion angle is zero degrees
            if (velocityVec.len() == 0f) velocityVec[speed] = 0f
            else velocityVec.setLength(speed)
        }

    val isMoving: Boolean
        /**
         * Determines if this object is moving (if speed is greater than zero).
         * @return false when speed is zero, true otherwise
         */
        get() = (speed > 0)

    var motionAngle: Float
        /**
         * Get the angle of motion (in degrees), calculated from the velocity vector.
         * <br></br>
         * To align actor image angle with motion angle, use `setRotation( getMotionAngle() )`.
         * @return angle of motion (degrees)
         */
        get() = velocityVec.angle()
        /**
         * Sets the angle of motion (in degrees).
         * If current speed is zero, this will have no effect.
         * @param angle of motion (degrees)
         */
        set(angle) {
            velocityVec.setAngle(angle)
        }

    /**
     * Update accelerate vector by angle and value stored in acceleration field.
     * Acceleration is applied by `applyPhysics` method.
     * @param angle Angle (degrees) in which to accelerate.
     * @see .acceleration
     *
     * @see .applyPhysics
     */
    fun accelerateAtAngle(angle: Float) {
        accelerationVec.add(
            Vector2(acceleration, 0f).setAngle(angle)
        )
    }

    /**
     * Update accelerate vector by current rotation angle and value stored in acceleration field.
     * Acceleration is applied by `applyPhysics` method.
     * @see .acceleration
     *
     * @see .applyPhysics
     */
    fun accelerateForward() {
        accelerateAtAngle(rotation)
    }

    /**
     * Adjust velocity vector based on acceleration vector,
     * then adjust position based on velocity vector. <br></br>
     * If not accelerating, deceleration value is applied. <br></br>
     * Speed is limited by maxSpeed value. <br></br>
     * Acceleration vector reset to (0,0) at end of method. <br></br>
     * @param dt Time elapsed since previous frame (delta time); typically obtained from `act` method.
     * @see .acceleration
     *
     * @see .deceleration
     *
     * @see .maxSpeed
     */
    fun applyPhysics(dt: Float) {
        // apply acceleration
        velocityVec.add(accelerationVec.x * dt, accelerationVec.y * dt)

        var speed = speed

        // decrease speed (decelerate) when not accelerating
        if (accelerationVec.len() == 0f) speed -= deceleration * dt

        // keep speed within set bounds
        speed = MathUtils.clamp(speed, 0f, maxSpeed)

        // update velocity
        this.speed = speed

        // update position according to value stored in velocity vector
        moveBy(velocityVec.x * dt, velocityVec.y * dt)

        // reset acceleration
        accelerationVec[0f] = 0f
    }

    // ----------------------------------------------
    // Collision polygon methods
    // ----------------------------------------------
    /**
     * Set rectangular-shaped collision polygon.
     * This method is automatically called when animation is set,
     * provided that the current boundary polygon is null.
     * @see .setAnimation
     */
    fun setBoundaryRectangle() {
        val w = width
        val h = height

        val vertices = floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h)
        boundaryPolygon = Polygon(vertices)
    }

    /**
     * Replace default (rectangle) collision polygon with an n-sided polygon. <br></br>
     * Vertices of polygon lie on the ellipse contained within bounding rectangle.
     * Note: one vertex will be located at point (0,width);
     * a 4-sided polygon will appear in the orientation of a diamond.
     * @param numSides number of sides of the collision polygon
     */
    fun setBoundaryPolygon(numSides: Int) {
        val w = width
        val h = height

        val vertices = FloatArray(2 * numSides)
        for (i in 0 until numSides) {
            val angle = i * 6.28f / numSides
            // x-coordinate
            vertices[2 * i] = w / 2 * MathUtils.cos(angle) + w / 2
            // y-coordinate
            vertices[2 * i + 1] = h / 2 * MathUtils.sin(angle) + h / 2
        }
        boundaryPolygon = Polygon(vertices)
    }

    /**
     * Returns bounding polygon for this BaseActor, adjusted by Actor's current position and rotation.
     * @return bounding polygon for this BaseActor
     */
    fun getBoundaryPolygon(): Polygon? {
        boundaryPolygon!!.setPosition(x, y)
        boundaryPolygon!!.setOrigin(originX, originY)
        boundaryPolygon!!.rotation = rotation
        boundaryPolygon!!.setScale(scaleX, scaleY)
        return boundaryPolygon
    }

    /**
     * Determine if this BaseActor overlaps other BaseActor (according to collision polygons).
     * @param other BaseActor to check for overlap
     * @return true if collision polygons of this and other BaseActor overlap
     * @see .setBoundaryRectangle
     *
     * @see .setBoundaryPolygon
     */
    fun overlaps(other: BaseActor): Boolean {
        val poly1 = this.getBoundaryPolygon()
        val poly2 = other.getBoundaryPolygon()

        // initial test to improve performance
        if (!poly1!!.boundingRectangle.overlaps(poly2!!.boundingRectangle)) return false

        return Intersector.overlapConvexPolygons(poly1, poly2)
    }

    /**
     * Implement a "solid"-like behavior:
     * when there is overlap, move this BaseActor away from other BaseActor
     * along minimum translation vector until there is no overlap.
     * @param other BaseActor to check for overlap
     * @return direction vector by which actor was translated, null if no overlap
     */
    fun preventOverlap(other: BaseActor): Vector2? {
        val poly1 = this.getBoundaryPolygon()
        val poly2 = other.getBoundaryPolygon()

        // initial test to improve performance
        if (!poly1!!.boundingRectangle.overlaps(poly2!!.boundingRectangle)) return null

        val mtv = MinimumTranslationVector()
        val polygonOverlap = Intersector.overlapConvexPolygons(poly1, poly2, mtv)

        if (!polygonOverlap) return null

        this.moveBy(mtv.normal.x * mtv.depth, mtv.normal.y * mtv.depth)
        return mtv.normal
    }

    /**
     * Determine if this BaseActor is near other BaseActor (according to collision polygons).
     * @param distance amount (pixels) by which to enlarge collision polygon width and height
     * @param other BaseActor to check if nearby
     * @return true if collision polygons of this (enlarged) and other BaseActor overlap
     * @see .setBoundaryRectangle
     *
     * @see .setBoundaryPolygon
     */
    fun isWithinDistance(distance: Float, other: BaseActor): Boolean {
        val poly1 = this.getBoundaryPolygon()
        val scaleX = (this.width + 2 * distance) / this.width
        val scaleY = (this.height + 2 * distance) / this.height
        poly1!!.setScale(scaleX, scaleY)

        val poly2 = other.getBoundaryPolygon()


        // initial test to improve performance
        if (!poly1.boundingRectangle.overlaps(poly2!!.boundingRectangle)) return false

        return Intersector.overlapConvexPolygons(poly1, poly2)
    }

    /**
     * If an edge of an object moves past the world bounds,
     * adjust its position to keep it completely on screen.
     */
    fun boundToWorld() {
        if (x < 0) x = 0f
        if (x + width > worldBounds!!.width) x = worldBounds!!.width - width
        if (y < 0) y = 0f
        if (y + height > worldBounds!!.height) y = worldBounds!!.height - height
    }

    /**
     * Center camera on this object, while keeping camera's range of view
     * (determined by screen size) completely within world bounds.
     */
    fun alignCamera() {
        val cam = stage.camera
        val v = stage.viewport

        // center camera on actor
        cam.position[x + this.originX, y + this.originY] = 0f

        // bound camera to layout
        cam.position.x = MathUtils.clamp(
            cam.position.x,
            cam.viewportWidth / 2,
            worldBounds!!.width - cam.viewportWidth / 2
        )
        cam.position.y = MathUtils.clamp(
            cam.position.y,
            cam.viewportHeight / 2,
            worldBounds!!.height - cam.viewportHeight / 2
        )
        cam.update()
    }

    // ----------------------------------------------
    // Actor methods: act and draw
    // ----------------------------------------------
    /**
     * Processes all Actions and related code for this object;
     * automatically called by act method in Stage class.
     * @param dt elapsed time (second) since last frame (supplied by Stage act method)
     */
    override fun act(dt: Float) {
        super.act(dt)

        if (!animationPaused) elapsedTime += dt
    }

    /**
     * Draws current frame of animation; automatically called by draw method in Stage class. <br></br>
     * If color has been set, image will be tinted by that color. <br></br>
     * If no animation has been set or object is invisible, nothing will be drawn.
     * @param batch (supplied by Stage draw method)
     * @param parentAlpha (supplied by Stage draw method)
     * @see .setColor
     *
     * @see .setVisible
     */
    override fun draw(batch: Batch, parentAlpha: Float) {
        // apply color tint effect

        val c = color
        batch.setColor(c.r, c.g, c.b, c.a)

        if (animation != null && isVisible) batch.draw(
            animation!!.getKeyFrame(elapsedTime),
            x, y, originX, originY,
            width, height, scaleX, scaleY, rotation
        )

        super.draw(batch, parentAlpha)
    }

    companion object {
        /**
         * Get world dimensions
         * @return Rectangle whose width/height represent world bounds
         */
        // stores size of game world for all actors
        var worldBounds: Rectangle? = null
            private set

        /**
         * Set world dimensions for use by methods boundToWorld() and scrollTo().
         * @param width width of world
         * @param height height of world
         */
        fun setWorldBounds(width: Float, height: Float) {
            worldBounds = Rectangle(0f, 0f, width, height)
        }

        /**
         * Set world dimensions for use by methods boundToWorld() and scrollTo().
         * @param BaseActor whose size determines the world bounds (typically a background image)
         */
        fun setWorldBounds(ba: BaseActor) {
            setWorldBounds(ba.width, ba.height)
        }

        // ----------------------------------------------
        // Instance list methods
        // ----------------------------------------------
        /**
         * Retrieves a list of all instances of the object from the given stage with the given class name
         * or whose class extends the class with the given name.
         * If no instances exist, returns an empty list.
         * Useful when coding interactions between different types of game objects in update method.
         * @param stage Stage containing BaseActor instances
         * @param className name of a class that extends the BaseActor class
         * @return list of instances of the object in stage which extend with the given class name
         */
        fun getList(stage: Stage, className: String?): ArrayList<BaseActor> {
            val list = ArrayList<BaseActor>()

            var theClass: Class<*>? = null
            try {
                theClass = Class.forName(className)
            } catch (error: Exception) {
                error.printStackTrace()
            }

            for (a in stage.actors) {
                if (theClass!!.isInstance(a)) list.add(a as BaseActor)
            }

            return list
        }

        /**
         * Returns number of instances of a given class (that extends BaseActor).
         * @param className name of a class that extends the BaseActor class
         * @return number of instances of the class
         */
        fun count(stage: Stage, className: String?): Int {
            return getList(stage, className).size
        }
    }
}
