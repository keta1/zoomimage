/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.zoomimage.internal

import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.widget.ImageView.ScaleType
import com.github.panpf.zoomimage.Edge
import com.github.panpf.zoomimage.Logger
import com.github.panpf.zoomimage.Size
import com.github.panpf.zoomimage.internal.ScaleDragGestureDetector.OnActionListener
import com.github.panpf.zoomimage.internal.ScaleDragGestureDetector.OnGestureListener
import kotlin.math.abs
import kotlin.math.roundToInt

internal class ScaleDragHelper constructor(
    private val context: Context,
    private val logger: Logger,
    private val engine: ZoomEngine,
    val onUpdateMatrix: () -> Unit,
    val onViewDrag: (dx: Float, dy: Float) -> Unit,
    val onDragFling: (velocityX: Float, velocityY: Float) -> Unit,
    val onScaleChanged: (scaleFactor: Float, focusX: Float, focusY: Float) -> Unit,
) {

    private val view = engine.view

    /* Stores default scale and translate information */
    private val baseMatrix = Matrix()

    /* Stores zoom, translate and externally set rotation information generated by the user through touch events */
    private val supportMatrix = Matrix()

    /* Store the fused information of baseMatrix and supportMatrix for drawing */
    private val displayMatrix = Matrix()
    private val displayRectF = RectF()

    /* Cache the coordinates of the last zoom gesture, used when restoring zoom */
    private var lastScaleFocusX: Float = 0f
    private var lastScaleFocusY: Float = 0f

    private var flingRunnable: FlingRunnable? = null
    private var locationRunnable: LocationRunnable? = null
    private var animatedScaleRunnable: AnimatedScaleRunnable? = null
    private val scaleDragGestureDetector: ScaleDragGestureDetector
    private var _horScrollEdge: Edge = Edge.NONE
    private var _verScrollEdge: Edge = Edge.NONE
    private var blockParentIntercept: Boolean = false
    private var dragging = false
    private var manualScaling = false

    val horScrollEdge: Edge
        get() = _horScrollEdge
    val verScrollEdge: Edge
        get() = _verScrollEdge

    val isScaling: Boolean
        get() = animatedScaleRunnable?.isRunning == true || manualScaling

    val scale: Float
        get() = supportMatrix.getScale().scaleX
    val translation: Translation
        get() = supportMatrix.getTranslation()

    val baseScale: ScaleFactor
        get() = baseMatrix.getScale()
    val baseTranslation: Translation
        get() = baseMatrix.getTranslation()

    val displayScale: ScaleFactor
        get() = displayMatrix.apply { getDisplayMatrix(this) }.getScale()
    val displayTranslation: Translation
        get() = displayMatrix.apply { getDisplayMatrix(this) }.getTranslation()

    init {
        scaleDragGestureDetector = ScaleDragGestureDetector(context, object : OnGestureListener {
            override fun onDrag(dx: Float, dy: Float) = doDrag(dx, dy)

            override fun onFling(velocityX: Float, velocityY: Float) = doFling(velocityX, velocityY)

            override fun onScaleBegin(): Boolean = doScaleBegin()

            override fun onScale(
                scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float
            ) = doScale(scaleFactor, focusX, focusY, dx, dy)

            override fun onScaleEnd() = doScaleEnd()
        }).apply {
            onActionListener = object : OnActionListener {
                override fun onActionDown(ev: MotionEvent) = actionDown()
                override fun onActionUp(ev: MotionEvent) = actionUp()
                override fun onActionCancel(ev: MotionEvent) = actionUp()
            }
        }
    }

    fun reset() {
        resetBaseMatrix()
        resetSupportMatrix()
        checkAndApplyMatrix()
    }

    fun clean() {
        animatedScaleRunnable?.cancel()
        animatedScaleRunnable = null
        locationRunnable?.cancel()
        locationRunnable = null
        flingRunnable?.cancel()
        flingRunnable = null
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        /* Location operations cannot be interrupted */
        if (this.locationRunnable?.isRunning == true) {
            logger.d(ZoomEngine.MODULE) {
                "onTouchEvent. requestDisallowInterceptTouchEvent true. locating"
            }
            requestDisallowInterceptTouchEvent(true)
            return true
        }
        return scaleDragGestureDetector.onTouchEvent(event)
    }

    private fun resetBaseMatrix() {
        baseMatrix.apply {
            reset()
            val transform = engine.baseInitialTransform
            postScale(transform.scaleX, transform.scaleY)
            postTranslate(transform.translationX, transform.translationY)
            postRotate(engine.rotateDegrees.toFloat())
        }
    }

    private fun resetSupportMatrix() {
        supportMatrix.apply {
            reset()
            val transform = engine.supportInitialTransform
            postScale(transform.scaleX, transform.scaleY)
            postTranslate(transform.translationX, transform.translationY)
        }
    }

    private fun checkAndApplyMatrix() {
        if (checkMatrixBounds()) {
            onUpdateMatrix()
        }
    }

    private fun checkMatrixBounds(): Boolean {
        val displayRectF = displayRectF.apply { getDisplayRect(this) }
        if (displayRectF.isEmpty) {
            _horScrollEdge = Edge.NONE
            _verScrollEdge = Edge.NONE
            return false
        }

        var deltaX = 0f
        val viewWidth = engine.viewSize.width
        val displayWidth = displayRectF.width()
        when {
            displayWidth.toInt() <= viewWidth -> {
                deltaX = when (engine.scaleType) {
                    ScaleType.FIT_START -> -displayRectF.left
                    ScaleType.FIT_END -> viewWidth - displayWidth - displayRectF.left
                    else -> (viewWidth - displayWidth) / 2 - displayRectF.left
                }
            }

            displayRectF.left.toInt() > 0 -> {
                deltaX = -displayRectF.left
            }

            displayRectF.right.toInt() < viewWidth -> {
                deltaX = viewWidth - displayRectF.right
            }
        }

        var deltaY = 0f
        val viewHeight = engine.viewSize.height
        val displayHeight = displayRectF.height()
        when {
            displayHeight.toInt() <= viewHeight -> {
                deltaY = when (engine.scaleType) {
                    ScaleType.FIT_START -> -displayRectF.top
                    ScaleType.FIT_END -> viewHeight - displayHeight - displayRectF.top
                    else -> (viewHeight - displayHeight) / 2 - displayRectF.top
                }
            }

            displayRectF.top.toInt() > 0 -> {
                deltaY = -displayRectF.top
            }

            displayRectF.bottom.toInt() < viewHeight -> {
                deltaY = viewHeight - displayRectF.bottom
            }
        }

        // Finally actually translate the matrix
        supportMatrix.postTranslate(deltaX, deltaY)

        _verScrollEdge = when {
            displayHeight.toInt() <= viewHeight -> Edge.BOTH
            displayRectF.top.toInt() >= 0 -> Edge.START
            displayRectF.bottom.toInt() <= viewHeight -> Edge.END
            else -> Edge.NONE
        }
        _horScrollEdge = when {
            displayWidth.toInt() <= viewWidth -> Edge.BOTH
            displayRectF.left.toInt() >= 0 -> Edge.START
            displayRectF.right.toInt() <= viewWidth -> Edge.END
            else -> Edge.NONE
        }
        return true
    }

    fun translateBy(dx: Float, dy: Float) {
        supportMatrix.postTranslate(dx, dy)
        checkAndApplyMatrix()
    }

    fun translateTo(dx: Float, dy: Float) {
        val translation = translation
        supportMatrix.postTranslate(dx - translation.translationX, dy - translation.translationY)
        checkAndApplyMatrix()
    }

    fun location(xInDrawable: Float, yInDrawable: Float, animate: Boolean) {
        locationRunnable?.cancel()
        cancelFling()

        val (viewWidth, viewHeight) = engine.viewSize.takeIf { !it.isEmpty } ?: return
        val pointF = PointF(xInDrawable, yInDrawable).apply {
            rotatePoint(this, engine.rotateDegrees, engine.drawableSize)
        }
        val newX = pointF.x
        val newY = pointF.y
        val nowScale = scale
        if (nowScale.format(2) == engine.minScale.format(2)) {
            scale(
                newScale = engine.getNextStepScale(),
                focalX = engine.viewSize.width / 2f,
                focalY = engine.viewSize.height / 2f,
                animate = false
            )
        }

        val displayRectF = getDisplayRect()
        val currentScale = displayScale
        val scaleLocationX = (newX * currentScale.scaleX).toInt()
        val scaleLocationY = (newY * currentScale.scaleY).toInt()
        val scaledLocationX =
            scaleLocationX.coerceIn(0, displayRectF.width().toInt())
        val scaledLocationY =
            scaleLocationY.coerceIn(0, displayRectF.height().toInt())
        val centerLocationX = (scaledLocationX - viewWidth / 2).coerceAtLeast(0)
        val centerLocationY = (scaledLocationY - viewHeight / 2).coerceAtLeast(0)
        val startX = abs(displayRectF.left.toInt())
        val startY = abs(displayRectF.top.toInt())
        logger.d(ZoomEngine.MODULE) {
            "location. inDrawable=${xInDrawable}x${yInDrawable}, start=${startX}x${startY}, end=${centerLocationX}x${centerLocationY}"
        }
        if (animate) {
            locationRunnable?.cancel()
            locationRunnable = LocationRunnable(
                context = context,
                engine = engine,
                scaleDragHelper = this@ScaleDragHelper,
                startX = startX,
                startY = startY,
                endX = centerLocationX,
                endY = centerLocationY
            )
            locationRunnable?.start()
        } else {
            val dx = -(centerLocationX - startX).toFloat()
            val dy = -(centerLocationY - startY).toFloat()
            translateBy(dx, dy)
        }
    }

    fun scale(newScale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        animatedScaleRunnable?.cancel()
        val currentScale = scale
        if (animate) {
            animatedScaleRunnable = AnimatedScaleRunnable(
                engine = engine,
                scaleDragHelper = this@ScaleDragHelper,
                startScale = currentScale,
                endScale = newScale,
                scaleFocalX = focalX,
                scaleFocalY = focalY
            )
            animatedScaleRunnable?.start()
        } else {
            scaleBy(addScale = newScale / currentScale, focalX = focalX, focalY = focalY)
        }
    }

    fun getDisplayMatrix(matrix: Matrix) {
        matrix.set(baseMatrix)
        matrix.postConcat(supportMatrix)
    }

    fun getDisplayRect(rectF: RectF) {
        val drawableSize = engine.drawableSize
        rectF[0f, 0f, drawableSize.width.toFloat()] = drawableSize.height.toFloat()
        displayMatrix.apply { getDisplayMatrix(this) }.mapRect(rectF)
    }

    fun getDisplayRect(): RectF {
        return RectF().apply { getDisplayRect(this) }
    }

    /**
     * Gets the area that the user can see on the drawable (not affected by rotation)
     */
    fun getVisibleRect(rect: Rect) {
        rect.setEmpty()
        val displayRectF = displayRectF.apply { getDisplayRect(this) }.takeIf { !it.isEmpty } ?: return
        val viewSize = engine.viewSize.takeIf { !it.isEmpty } ?: return
        val drawableSize = engine.drawableSize.takeIf { !it.isEmpty } ?: return
        val (drawableWidth, drawableHeight) = drawableSize.let {
            if (engine.rotateDegrees % 180 == 0) it else Size(it.height, it.width)
        }
        val displayWidth = displayRectF.width()
        val displayHeight = displayRectF.height()
        val widthScale = displayWidth / drawableWidth
        val heightScale = displayHeight / drawableHeight
        var left: Float = if (displayRectF.left >= 0)
            0f else abs(displayRectF.left)
        var right: Float = if (displayWidth >= viewSize.width)
            viewSize.width + left else displayRectF.right - displayRectF.left
        var top: Float = if (displayRectF.top >= 0)
            0f else abs(displayRectF.top)
        var bottom: Float = if (displayHeight >= viewSize.height)
            viewSize.height + top else displayRectF.bottom - displayRectF.top
        left /= widthScale
        right /= widthScale
        top /= heightScale
        bottom /= heightScale
        rect.set(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
        reverseRotateRect(rect, engine.rotateDegrees, drawableSize)
    }

    /**
     * Gets the area that the user can see on the drawable (not affected by rotation)
     */
    fun getVisibleRect(): Rect {
        return Rect().apply { getVisibleRect(this) }
    }

    fun touchPointToDrawablePoint(touchPoint: PointF): Point? {
        val drawableSize = engine.drawableSize.takeIf { !it.isEmpty } ?: return null
        val displayRect = getDisplayRect()
        if (!displayRect.contains(touchPoint.x, touchPoint.y)) {
            return null
        }

        val zoomScale = displayScale
        val drawableX =
            ((touchPoint.x - displayRect.left) / zoomScale.scaleX).roundToInt()
                .coerceIn(0, drawableSize.width)
        val drawableY =
            ((touchPoint.y - displayRect.top) / zoomScale.scaleY).roundToInt()
                .coerceIn(0, drawableSize.height)
        return Point(drawableX, drawableY)
    }

    /**
     * Whether you can scroll horizontally in the specified direction
     *
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     */
    fun canScrollHorizontally(direction: Int): Boolean {
        return if (direction < 0) {
            horScrollEdge != Edge.START && horScrollEdge != Edge.BOTH
        } else {
            horScrollEdge != Edge.END && horScrollEdge != Edge.BOTH
        }
    }

    /**
     * Whether you can scroll vertically in the specified direction
     *
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     */
    fun canScrollVertically(direction: Int): Boolean {
        return if (direction < 0) {
            verScrollEdge != Edge.START && horScrollEdge != Edge.BOTH
        } else {
            verScrollEdge != Edge.END && horScrollEdge != Edge.BOTH
        }
    }

    private fun doDrag(dx: Float, dy: Float) {
        logger.d(ZoomEngine.MODULE) { "onDrag. dx: $dx, dy: $dy" }

        if (scaleDragGestureDetector.isScaling) {
            logger.d(ZoomEngine.MODULE) { "onDrag. isScaling" }
            return
        }

        supportMatrix.postTranslate(dx, dy)
        checkAndApplyMatrix()

        onViewDrag(dx, dy)

        val scaling = scaleDragGestureDetector.isScaling
        val disallowParentInterceptOnEdge = !engine.allowParentInterceptOnEdge
        val blockParent = blockParentIntercept
        val disallow = if (dragging || scaling || blockParent || disallowParentInterceptOnEdge) {
            logger.d(ZoomEngine.MODULE) {
                "onDrag. DisallowParentIntercept. dragging=$dragging, scaling=$scaling, blockParent=$blockParent, disallowParentInterceptOnEdge=$disallowParentInterceptOnEdge"
            }
            true
        } else {
            val slop = engine.view.resources.displayMetrics.density * 3
            val result = (horScrollEdge == Edge.NONE && (dx >= slop || dx <= -slop))
                    || (horScrollEdge == Edge.START && dx <= -slop)
                    || (horScrollEdge == Edge.END && dx >= slop)
                    || (verScrollEdge == Edge.NONE && (dy >= slop || dy <= -slop))
                    || (verScrollEdge == Edge.START && dy <= -slop)
                    || (verScrollEdge == Edge.END && dy >= slop)
            val type = if (result) "DisallowParentIntercept" else "AllowParentIntercept"
            logger.d(ZoomEngine.MODULE) {
                "onDrag. $type. scrollEdge=${horScrollEdge}-${verScrollEdge}, d=${dx}x${dy}"
            }
            dragging = result
            result
        }
        requestDisallowInterceptTouchEvent(disallow)
    }

    private fun doFling(velocityX: Float, velocityY: Float) {
        logger.d(ZoomEngine.MODULE) {
            "fling. velocity=($velocityX, $velocityY), translation=${translation.toShortString()}"
        }

        flingRunnable?.cancel()
        flingRunnable = FlingRunnable(
            context = context,
            engine = engine,
            scaleDragHelper = this@ScaleDragHelper,
            velocityX = velocityX.toInt(),
            velocityY = velocityY.toInt()
        )
        flingRunnable?.start()

        onDragFling(velocityX, velocityY)
    }

    private fun cancelFling() {
        flingRunnable?.cancel()
    }

    private fun doScaleBegin(): Boolean {
        logger.d(ZoomEngine.MODULE) { "onScaleBegin" }
        manualScaling = true
        return true
    }

    private fun scaleBy(addScale: Float, focalX: Float, focalY: Float) {
        supportMatrix.postScale(addScale, addScale, focalX, focalY)
        checkAndApplyMatrix()
    }

    internal fun doScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float) {
        logger.d(ZoomEngine.MODULE) {
            "onScale. scaleFactor: $scaleFactor, focusX: $focusX, focusY: $focusY, dx: $dx, dy: $dy"
        }

        /* Simulate a rubber band effect when zoomed to max or min */
        var newScaleFactor = scaleFactor
        lastScaleFocusX = focusX
        lastScaleFocusY = focusY
        val currentSupportScale = scale
        var newSupportScale = currentSupportScale * newScaleFactor
        if (newScaleFactor > 1.0f) {
            // The maximum zoom has been reached. Simulate the effect of pulling a rubber band
            val maxSupportScale = engine.maxScale
            if (currentSupportScale >= maxSupportScale) {
                var addScale = newSupportScale - currentSupportScale
                addScale *= 0.4f
                newSupportScale = currentSupportScale + addScale
                newScaleFactor = newSupportScale / currentSupportScale
            }
        } else if (newScaleFactor < 1.0f) {
            // The minimum zoom has been reached. Simulate the effect of pulling a rubber band
            val minSupportScale = engine.minScale
            if (currentSupportScale <= minSupportScale) {
                var addScale = newSupportScale - currentSupportScale
                addScale *= 0.4f
                newSupportScale = currentSupportScale + addScale
                newScaleFactor = newSupportScale / currentSupportScale
            }
        }

        supportMatrix.postScale(newScaleFactor, newScaleFactor, focusX, focusY)
        supportMatrix.postTranslate(dx, dy)
        checkAndApplyMatrix()

        onScaleChanged(newScaleFactor, focusX, focusY)
    }

    private fun doScaleEnd() {
        logger.d(ZoomEngine.MODULE) { "onScaleEnd" }
        manualScaling = false
        onUpdateMatrix()
    }

    private fun actionDown() {
        logger.d(ZoomEngine.MODULE) {
            "onActionDown. disallow parent intercept touch event"
        }

        lastScaleFocusX = 0f
        lastScaleFocusY = 0f
        dragging = false

        requestDisallowInterceptTouchEvent(true)

        cancelFling()
    }

    private fun actionUp() {
        /* Roll back to minimum or maximum scaling */
        val currentScale = scale.format(2)
        val minZoomScale = engine.minScale.format(2)
        val maxZoomScale = engine.maxScale.format(2)
        if (currentScale < minZoomScale) {
            val displayRectF = displayRectF.apply { getDisplayRect(this) }
            if (!displayRectF.isEmpty) {
                scale(minZoomScale, displayRectF.centerX(), displayRectF.centerY(), true)
            }
        } else if (currentScale > maxZoomScale) {
            val lastScaleFocusX = lastScaleFocusX
            val lastScaleFocusY = lastScaleFocusY
            if (lastScaleFocusX != 0f && lastScaleFocusY != 0f) {
                scale(maxZoomScale, lastScaleFocusX, lastScaleFocusY, true)
            }
        }
    }

    private fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        view.parent?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
}