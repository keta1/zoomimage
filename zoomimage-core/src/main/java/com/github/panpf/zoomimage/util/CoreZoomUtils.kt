package com.github.panpf.zoomimage.util

import com.github.panpf.zoomimage.Edge
import com.github.panpf.zoomimage.ScrollEdge
import com.github.panpf.zoomimage.util.internal.format
import kotlin.math.max
import kotlin.math.roundToInt

fun calculateNextStepScale(
    stepScales: FloatArray,
    currentScale: Float,
    rangeOfError: Float = 0.1f
): Float {
    if (stepScales.isEmpty()) return currentScale
    val formattedCurrentScale = currentScale.format(1)
    return stepScales
        .find { it.format(1) > formattedCurrentScale + rangeOfError }
        ?: stepScales.first()
}

const val DefaultMediumScaleMinMultiple: Float = 3f

fun computeUserScales(
    contentSize: IntSizeCompat,
    contentOriginSize: IntSizeCompat,
    containerSize: IntSizeCompat,
    scaleMode: ScaleMode,   // todo 用 ContentScaleCompat 和 AlignmentCompat 替代
    baseScale: ScaleFactorCompat,
    mediumScaleMinMultiple: Float
): FloatArray {
    if (contentSize.isEmpty() || containerSize.isEmpty()) {
        return floatArrayOf(1.0f, 1.0f, 1.0f)
    } else if (scaleMode == ScaleMode.FILL_BOUNDS
        || baseScale.scaleX.format(2) != baseScale.scaleY.format(2)
    ) {
        val minScale = 1.0f
        val mediumScale = minScale * mediumScaleMinMultiple
        return floatArrayOf(minScale, mediumScale, mediumScale * 2f)
    } else {
        // The width and height of content fill the container at the same time
        val fillContainerScale = max(
            containerSize.width / contentSize.width.toFloat(),
            containerSize.height / contentSize.height.toFloat()
        )
        // Enlarge content to the same size as its original
        val contentOriginScale = if (contentOriginSize.isNotEmpty) {
            val widthScale = contentOriginSize.width / contentSize.width.toFloat()
            val heightScale = contentOriginSize.height / contentSize.height.toFloat()
            max(widthScale, heightScale)
        } else {
            1.0f
        }
        val minScale = baseScale.scaleX
        val mediumScale = floatArrayOf(
            contentOriginScale,
            fillContainerScale,
            minScale * mediumScaleMinMultiple
        ).maxOrNull()!!
        val maxScale = mediumScale * 2f
        return floatArrayOf(minScale, mediumScale, maxScale)
            .map { it / baseScale.scaleX }
            .toFloatArray()
    }
}

fun isSameDirection(srcSize: IntSizeCompat, dstSize: IntSizeCompat): Boolean {
    val srcAspectRatio = srcSize.width.toFloat().div(srcSize.height).format(2)
    val dstAspectRatio = dstSize.width.toFloat().div(dstSize.height).format(2)
    return (srcAspectRatio == 1.0f || dstAspectRatio == 1.0f)
            || (srcAspectRatio > 1.0f && dstAspectRatio > 1.0f)
            || (srcAspectRatio < 1.0f && dstAspectRatio < 1.0f)
}


fun computeScrollEdge(
    contentInContainerVisibleRect: IntRectCompat,
    contentVisibleRect: IntRectCompat,
): ScrollEdge {
    if (contentInContainerVisibleRect.isEmpty || contentVisibleRect.isEmpty)
        return ScrollEdge(horizontal = Edge.BOTH, vertical = Edge.BOTH)
    return ScrollEdge(
        horizontal = when {
            contentVisibleRect.width >= contentInContainerVisibleRect.width -> Edge.BOTH
            contentVisibleRect.left <= contentInContainerVisibleRect.left -> Edge.START
            contentVisibleRect.right >= contentInContainerVisibleRect.right -> Edge.END
            else -> Edge.NONE
        },
        vertical = when {
            contentVisibleRect.height >= contentInContainerVisibleRect.height -> Edge.BOTH
            contentVisibleRect.top <= contentInContainerVisibleRect.top -> Edge.START
            contentVisibleRect.bottom >= contentInContainerVisibleRect.bottom -> Edge.END
            else -> Edge.NONE
        },
    )
}

/**
 * Whether you can scroll horizontally or vertical in the specified direction
 *
 * @param direction Negative to check scrolling left, positive to check scrolling right.
 */
fun canScroll(horizontal: Boolean, direction: Int, scrollEdge: ScrollEdge): Boolean {
    return if (horizontal) {
        if (direction < 0) {
            scrollEdge.horizontal != Edge.START && scrollEdge.horizontal != Edge.BOTH
        } else {
            scrollEdge.horizontal != Edge.END && scrollEdge.horizontal != Edge.BOTH
        }
    } else {
        if (direction < 0) {
            scrollEdge.vertical != Edge.START && scrollEdge.vertical != Edge.BOTH
        } else {
            scrollEdge.vertical != Edge.END && scrollEdge.vertical != Edge.BOTH
        }
    }
}

fun limitScaleWithRubberBand(
    currentScale: Float,
    targetScale: Float,
    minScale: Float,
    maxScale: Float,
    rubberBandRatio: Float = 2f
): Float {
    return when {
        targetScale > maxScale -> {
            val addScale = targetScale - currentScale
            val rubberBandMaxScale = maxScale * rubberBandRatio
            val overScale = targetScale - maxScale
            val overMaxScale = rubberBandMaxScale - maxScale
            val progress = overScale / overMaxScale
            // Multiplying by 0.5f is to be a little slower
            val limitedAddScale = addScale * (1 - progress) * 0.5f
            currentScale + limitedAddScale
        }

        targetScale < minScale -> {
            val addScale = targetScale - currentScale
            val rubberBandMinScale = minScale / rubberBandRatio
            val overScale = targetScale - minScale
            val overMinScale = rubberBandMinScale - minScale
            val progress = overScale / overMinScale
            // Multiplying by 0.5f is to be a little slower
            val limitedAddScale = addScale * (1 - progress) * 0.5f
            currentScale + limitedAddScale
        }

        else -> targetScale
    }
}

enum class ScaleMode {
    CROP,
    FIT,
    FILL_UNILATERAL,
    FILL_BOUNDS,
    INSIDE,
    NONE,
}


/**
 * base rotation center is content center
 */
fun computeContentInContainerRect(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    rotation: Int,
): IntRectCompat {
    require(rotation % 90 == 0) { "rotation must be multiple of 90" }
    if (rotation % 180 == 0) {
        return IntRectCompat(
            left = 0,
            top = 0,
            right = containerSize.width,
            bottom = containerSize.height,
        )
    } else {
        val contentCenter = contentSize.center
        //  - 1 is subtracting the center point
        val left = contentCenter.x - (contentSize.height / 2f).roundToInt() - 1
        val top = contentCenter.y - (contentSize.width / 2f).roundToInt() - 1
        return IntRectCompat(
            left = left,
            top = top,
            right = left + contentSize.height,
            bottom = top + contentSize.width,
        )
    }
}

// todo crop 时效果不对
fun computeBaseTransform(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
): TransformCompat {
    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return TransformCompat.Origin
    }
    val rotatedContentSize = contentSize.rotate(rotation)
    val rotatedContentScaleFactor = contentScale.computeScaleFactor(
        srcSize = rotatedContentSize.toSize(),
        dstSize = containerSize.toSize()
    )
    val scaledRotatedContentSize = rotatedContentSize.times(rotatedContentScaleFactor)
    val scaledRotatedContentAlignmentOffset = alignment.align(
        size = scaledRotatedContentSize,
        space = containerSize,
        ltrLayout = true,
    )
    val rotatedContentInContainerRect = computeContentInContainerRect(
        containerSize = containerSize,
        contentSize = contentSize,
        rotation = rotation,
    )
    val rotateRectifyOffset = IntOffsetCompat.Zero - rotatedContentInContainerRect.topLeft
    val scaledRotateRectifyOffset = rotateRectifyOffset * rotatedContentScaleFactor
    val finalOffset = scaledRotatedContentAlignmentOffset + scaledRotateRectifyOffset
    return TransformCompat(scale = rotatedContentScaleFactor, offset = finalOffset.toOffset())
}

fun computeContentInContainerRect(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    scale: Float,
    offset: OffsetCompat,
    rotation: Int,
): RectCompat {
    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return RectCompat.Zero
    }
    require(rotation % 90 == 0) { "rotation must be a multiple of 90, rotation: $rotation" }
    val contentScaleFactor = contentScale.computeScaleFactor(
        srcSize = contentSize.toSize(),
        dstSize = containerSize.toSize()
    )
    val scaledContentSize = contentSize.toSize().times(contentScaleFactor)
    val alignmentOffset = alignment.align(
        size = scaledContentSize.round(),
        space = containerSize,
        ltrLayout = true,
    )
    val contentInContainerRect = RectCompat(
        left = alignmentOffset.x.toFloat(),
        top = alignmentOffset.y.toFloat(),
        right = alignmentOffset.x + scaledContentSize.width,
        bottom = alignmentOffset.y + scaledContentSize.height,
    )

    val rotatedContentInContainerRect = contentInContainerRect.rotate(rotation)
    val offsetContentInContainerRect = rotatedContentInContainerRect.translate(offset)
    @Suppress("UnnecessaryVariable") val scaledContentInContainerRect =
        offsetContentInContainerRect.scale(scale)
    return scaledContentInContainerRect
}