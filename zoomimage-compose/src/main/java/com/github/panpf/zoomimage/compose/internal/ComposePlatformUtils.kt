package com.github.panpf.zoomimage.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


@Composable
internal fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}

@Composable
internal fun Float.toDp(): Dp {
    return with(LocalDensity.current) { this@toDp.toDp() }
}


internal fun Size.toShortString(): String =
    if (isSpecified) "${width.format(2)}x${height.format(2)}" else "Unspecified"

internal fun Offset.toShortString(): String =
    if (isSpecified) "${x.format(2)}x${y.format(2)}" else "Unspecified"

internal fun Rect.toShortString(): String =
    "[${left.format(2)}x${top.format(2)},${right.format(2)}x${bottom.format(2)}]"

internal fun ScaleFactor.toShortString(): String = "${scaleX.format(2)}x${scaleY.format(2)}"

internal fun IntSize.toShortString(): String = "${width}x${height}"

internal fun IntOffset.toShortString(): String = "${x}x${y}"

internal fun IntRect.toShortString(): String = "[${left}x${top},${right}x${bottom}]"

internal fun TransformOrigin.toShortString(): String =
    "${pivotFractionX.format(2)}x${pivotFractionY.format(2)}"


internal fun IntSize.toOffset(): Offset = Offset(x = width.toFloat(), y = height.toFloat())

internal fun IntSize.toIntOffset(): IntOffset = IntOffset(x = width, y = height)

internal fun Size.toOffset(): Offset = Offset(x = width, y = height)

internal fun Size.roundToOffset(): IntOffset = IntOffset(x = width.roundToInt(), y = height.roundToInt())

internal fun IntOffset.toSize(): Size = Size(width = x.toFloat(), height = y.toFloat())

internal fun IntOffset.toIntSize(): IntSize = IntSize(width = x, height = y)

internal fun Offset.toSize(): Size = Size(width = x, height = y)

internal fun Offset.roundToSize(): IntSize = IntSize(width = x.roundToInt(), height = y.roundToInt())


internal fun Size.isAvailable(): Boolean = isSpecified && !isEmpty()

internal fun Size.isNotAvailable(): Boolean = isUnspecified || isEmpty()


internal fun Size.rotate(rotateDegrees: Int): Size =
    if (rotateDegrees % 180 == 0) this else Size(height, width)


internal fun Size.round(): IntSize =
    IntSize(width.roundToInt(), height.roundToInt())


@Stable
operator fun Size.times(scaleFactor: ScaleFactor): Size =
    Size(
        width = this.width * scaleFactor.scaleX,
        height = this.height * scaleFactor.scaleY,
    )

@Stable
operator fun Size.div(scaleFactor: ScaleFactor): Size =
    Size(
        width = this.width / scaleFactor.scaleX,
        height = this.height / scaleFactor.scaleY,
    )

@Stable
operator fun IntSize.times(scaleFactor: ScaleFactor): IntSize =
    IntSize(
        width = (this.width * scaleFactor.scaleX).roundToInt(),
        height = (this.height * scaleFactor.scaleY).roundToInt()
    )

@Stable
operator fun IntSize.div(scaleFactor: ScaleFactor): IntSize =
    IntSize(
        width = (this.width / scaleFactor.scaleX).roundToInt(),
        height = (this.height / scaleFactor.scaleY).roundToInt()
    )

@Stable
operator fun IntSize.times(scale: Float): IntSize =
    IntSize(
        width = (this.width * scale).roundToInt(),
        height = (this.height * scale).roundToInt()
    )

@Stable
operator fun IntSize.div(scale: Float): IntSize =
    IntSize(
        width = (this.width / scale).roundToInt(),
        height = (this.height / scale).roundToInt()
    )

operator fun Offset.times(scaleFactor: ScaleFactor): Offset =
    Offset(x * scaleFactor.scaleX, y * scaleFactor.scaleY)

operator fun Offset.div(scaleFactor: ScaleFactor): Offset =
    Offset(x = x / scaleFactor.scaleX, y = y / scaleFactor.scaleY)

operator fun IntOffset.times(scaleFactor: ScaleFactor): IntOffset =
    IntOffset(
        x = (x * scaleFactor.scaleX).roundToInt(),
        y = (y * scaleFactor.scaleY).roundToInt()
    )

operator fun IntOffset.div(scaleFactor: ScaleFactor): IntOffset =
    IntOffset(
        x = (x / scaleFactor.scaleX).roundToInt(),
        y = (y / scaleFactor.scaleY).roundToInt()
    )


@Stable
fun IntSize.isEmpty(): Boolean = width == 0 || height == 0

@Stable
fun IntSize.isNotEmpty(): Boolean = !isEmpty()


internal fun IntSize.rotate(rotation: Int): IntSize {
    return if (rotation % 180 == 0) this else IntSize(height, width)
}

/**
 * Rotates the given offset around the origin by the given angle in degrees.
 *
 * A positive angle indicates a counterclockwise rotation around the right-handed 2D Cartesian
 * coordinate system.
 *
 * See: [Rotation matrix](https://en.wikipedia.org/wiki/Rotation_matrix)
 */
fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}


private val transformOriginTopStart by lazy { TransformOrigin(0f, 0f) }
val TransformOrigin.Companion.TopStart: TransformOrigin
    get() = transformOriginTopStart

@Stable
internal operator fun ScaleFactor.times(scaleFactor: ScaleFactor) =
    ScaleFactor(scaleX * scaleFactor.scaleX, scaleY * scaleFactor.scaleY)

@Stable
internal operator fun ScaleFactor.div(scaleFactor: ScaleFactor) =
    ScaleFactor(scaleX / scaleFactor.scaleX, scaleY / scaleFactor.scaleY)

internal fun ScaleFactor(scale: Float): ScaleFactor = ScaleFactor(scale, scale)


internal fun Rect.scale(scale: Float): Rect =
    Rect(
        left = (left * scale),
        top = (top * scale),
        right = (right * scale),
        bottom = (bottom * scale),
    )

internal fun IntRect.scale(scale: Float): IntRect =
    IntRect(
        left = (left * scale).roundToInt(),
        top = (top * scale).roundToInt(),
        right = (right * scale).roundToInt(),
        bottom = (bottom * scale).roundToInt(),
    )

internal fun IntRect.scale(scale: ScaleFactor): IntRect =
    IntRect(
        left = (left * scale.scaleX).roundToInt(),
        top = (top * scale.scaleY).roundToInt(),
        right = (right * scale.scaleX).roundToInt(),
        bottom = (bottom * scale.scaleY).roundToInt(),
    )

internal fun Rect.restoreScale(scale: Float): Rect =
    Rect(
        left = (left / scale),
        top = (top / scale),
        right = (right / scale),
        bottom = (bottom / scale),
    )

internal fun Rect.restoreScale(scaleFactor: ScaleFactor): Rect =
    Rect(
        left = (left / scaleFactor.scaleX),
        top = (top / scaleFactor.scaleY),
        right = (right / scaleFactor.scaleX),
        bottom = (bottom / scaleFactor.scaleY),
    )

internal fun IntRect.restoreScale(scale: Float): IntRect =
    IntRect(
        left = (left / scale).roundToInt(),
        top = (top / scale).roundToInt(),
        right = (right / scale).roundToInt(),
        bottom = (bottom / scale).roundToInt(),
    )

internal fun IntRect.restoreScale(scaleFactor: ScaleFactor): IntRect =
    IntRect(
        left = (left / scaleFactor.scaleX).roundToInt(),
        top = (top / scaleFactor.scaleY).roundToInt(),
        right = (right / scaleFactor.scaleX).roundToInt(),
        bottom = (bottom / scaleFactor.scaleY).roundToInt(),
    )

internal fun IntRect.limitTo(rect: IntRect): IntRect =
    IntRect(
        left = left.coerceAtLeast(rect.left),
        top = top.coerceAtLeast(rect.top),
        right = right.coerceIn(rect.left, rect.right),
        bottom = bottom.coerceIn(rect.top, rect.bottom),
    )

internal fun Rect.limitTo(rect: Rect): Rect =
    Rect(
        left = left.coerceAtLeast(rect.left),
        top = top.coerceAtLeast(rect.top),
        right = right.coerceIn(rect.left, rect.right),
        bottom = bottom.coerceIn(rect.top, rect.bottom),
    )

internal val ContentScale.name: String
    get() = when (this) {
        ContentScale.FillWidth -> "FillWidth"
        ContentScale.FillHeight -> "FillHeight"
        ContentScale.FillBounds -> "FillBounds"
        ContentScale.Fit -> "Fit"
        ContentScale.Crop -> "Crop"
        ContentScale.Inside -> "Inside"
        ContentScale.None -> "None"
        else -> "Unknown"
    }

internal val Alignment.name: String
    get() = when (this) {
        Alignment.TopStart -> "TopStart"
        Alignment.TopCenter -> "TopCenter"
        Alignment.TopEnd -> "TopEnd"
        Alignment.CenterStart -> "CenterStart"
        Alignment.Center -> "Center"
        Alignment.CenterEnd -> "CenterEnd"
        Alignment.BottomStart -> "BottomStart"
        Alignment.BottomCenter -> "BottomCenter"
        Alignment.BottomEnd -> "BottomEnd"
        else -> "Unknown"
    }

internal val Alignment.isStart: Boolean
    get() = this == Alignment.TopStart || this == Alignment.CenterStart || this == Alignment.BottomStart
internal val Alignment.isHorizontalCenter: Boolean
    get() = this == Alignment.TopCenter || this == Alignment.Center || this == Alignment.BottomCenter
internal val Alignment.isCenter: Boolean
    get() = this == Alignment.Center
internal val Alignment.isEnd: Boolean
    get() = this == Alignment.TopEnd || this == Alignment.CenterEnd || this == Alignment.BottomEnd
internal val Alignment.isTop: Boolean
    get() = this == Alignment.TopStart || this == Alignment.TopCenter || this == Alignment.TopEnd
internal val Alignment.isVerticalCenter: Boolean
    get() = this == Alignment.CenterStart || this == Alignment.Center || this == Alignment.CenterEnd
internal val Alignment.isBottom: Boolean
    get() = this == Alignment.BottomStart || this == Alignment.BottomCenter || this == Alignment.BottomEnd
