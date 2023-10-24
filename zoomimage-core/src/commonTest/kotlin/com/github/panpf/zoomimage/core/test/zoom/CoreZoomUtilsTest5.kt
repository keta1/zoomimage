package com.github.panpf.zoomimage.core.test.zoom

import com.github.panpf.zoomimage.util.IntSizeCompat
import com.github.panpf.zoomimage.util.OffsetCompat
import com.github.panpf.zoomimage.util.ScaleFactorCompat
import com.github.panpf.zoomimage.util.TransformCompat
import com.github.panpf.zoomimage.util.TransformOriginCompat
import com.github.panpf.zoomimage.util.copy
import com.github.panpf.zoomimage.util.plus
import com.github.panpf.zoomimage.util.round
import com.github.panpf.zoomimage.zoom.AlignmentCompat
import com.github.panpf.zoomimage.zoom.ContentScaleCompat
import com.github.panpf.zoomimage.zoom.ReadMode
import com.github.panpf.zoomimage.zoom.ScalesCalculator
import com.github.panpf.zoomimage.zoom.calculateContentVisibleRect
import com.github.panpf.zoomimage.zoom.calculateInitialZoom
import com.github.panpf.zoomimage.zoom.calculateRestoreContentVisibleCenterUserTransform
import com.github.panpf.zoomimage.zoom.checkParamsChanges
import com.github.panpf.zoomimage.zoom.transformAboutEquals
import org.junit.Assert
import org.junit.Test

class CoreZoomUtilsTest5 {

    @Test
    fun testCheckParamsChanges() {
        val containerSize = IntSizeCompat(800, 572)
        val contentSize = IntSizeCompat(6799, 4882)
        val contentOriginSize = IntSizeCompat.Zero
        val contentScale = ContentScaleCompat.Fit
        val alignment = AlignmentCompat.Center
        val rotation = 0
        val readMode: ReadMode? = null
        val scalesCalculator = ScalesCalculator.Dynamic

        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize,
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize,
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(0, this)
        }
        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(),
            lastContentSize = contentSize.copy(),
            lastContentOriginSize = contentOriginSize.copy(),
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(0, this)
        }

        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(width = containerSize.width + 1),
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize,
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(1, this)
        }
        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(height = containerSize.height + 1),
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize,
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(1, this)
        }

        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(width = containerSize.width + 1),
            lastContentSize = contentSize.copy(width = contentSize.width + 1),
            lastContentOriginSize = contentOriginSize,
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(-1, this)
        }
        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(width = containerSize.width + 1),
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize.copy(width = contentOriginSize.width + 1),
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(-1, this)
        }
        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(width = containerSize.width + 1),
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize,
            lastContentScale = ContentScaleCompat.Inside,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(-1, this)
        }
        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(width = containerSize.width + 1),
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize,
            lastContentScale = contentScale,
            lastAlignment = AlignmentCompat.BottomEnd,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(-1, this)
        }
        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(width = containerSize.width + 1),
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize,
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation + 90,
            lastReadMode = readMode,
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(-1, this)
        }
        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(width = containerSize.width + 1),
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize,
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = ReadMode(ReadMode.SIZE_TYPE_HORIZONTAL),
            lastScalesCalculator = scalesCalculator,
        ).apply {
            Assert.assertEquals(-1, this)
        }
        checkParamsChanges(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            readMode = readMode,
            scalesCalculator = scalesCalculator,
            lastContainerSize = containerSize.copy(width = containerSize.width + 1),
            lastContentSize = contentSize,
            lastContentOriginSize = contentOriginSize,
            lastContentScale = contentScale,
            lastAlignment = alignment,
            lastRotation = rotation,
            lastReadMode = readMode,
            lastScalesCalculator = ScalesCalculator.Fixed,
        ).apply {
            Assert.assertEquals(-1, this)
        }
    }

    @Test
    fun testCalculateRestoreContentVisibleCenterUserTransform() {
        val containerSize = IntSizeCompat(800, 572)
        val contentSize = IntSizeCompat(6799, 4882)
        val contentOriginSize = IntSizeCompat.Zero
        val contentScale = ContentScaleCompat.Fit
        val alignment = AlignmentCompat.Center
        val rotation = 0
        val readMode: ReadMode? = null
        val scalesCalculator = ScalesCalculator.Dynamic
        val lastBaseTransform = TransformCompat(
            scale = ScaleFactorCompat(0.117165096f, 0.117165096f),
            offset = OffsetCompat(2.0f, 0.0f),
            rotationOrigin = TransformOriginCompat(4.249375f, 4.2674823f)
        )
        val lastUserTransform = TransformCompat(
            scale = ScaleFactorCompat(8.5349655f, 8.5349655f),
            offset = OffsetCompat(-3948.0999f, -2968.974f),
            rotationOrigin = TransformOriginCompat(4.249375f, 4.2674823f)
        )
        val lastTransform = (lastBaseTransform + lastUserTransform).apply {
            Assert.assertEquals(
                "TransformCompat(scale=1.0x1.0, " +
                        "offset=-3931.03x-2968.97, " +
                        "rotation=0.0, " +
                        "scaleOrigin=0.0x0.0, " +
                        "rotationOrigin=4.25x4.27)",
                this.toString()
            )
        }
        val contentVisibleCenterPoint = calculateContentVisibleRect(
            containerSize = containerSize,
            contentSize = contentSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = rotation,
            userScale = lastUserTransform.scaleX,
            userOffset = lastUserTransform.offset,
        ).center.apply {
            Assert.assertEquals("OffsetCompat(4331.0, 3255.0)", this.toString())
        }

        listOf(
            IntSizeCompat(1900, 1072),
            IntSizeCompat(1072, 1900),
            IntSizeCompat(880, 433),
            IntSizeCompat(433, 880),
        ).forEach { newContainerSize ->
            val newInitialZoom = calculateInitialZoom(
                containerSize = newContainerSize,
                contentSize = contentSize,
                contentOriginSize = contentOriginSize,
                contentScale = contentScale,
                alignment = alignment,
                rotation = rotation,
                readMode = readMode,
                scalesCalculator = scalesCalculator
            )
            val newBaseTransform = newInitialZoom.baseTransform
            val newUserTransform = calculateRestoreContentVisibleCenterUserTransform(
                containerSize = newContainerSize,
                contentSize = contentSize,
                contentScale = contentScale,
                alignment = alignment,
                rotation = rotation,
                newBaseTransform = newBaseTransform,
                lastTransform = lastTransform,
                lastContentVisibleCenter = contentVisibleCenterPoint.round(),
            )

            val newContentVisibleCenterPoint = calculateContentVisibleRect(
                containerSize = newContainerSize,
                contentSize = contentSize,
                contentScale = contentScale,
                alignment = alignment,
                rotation = rotation,
                userScale = newUserTransform.scaleX,
                userOffset = newUserTransform.offset,
            ).center

            Assert.assertEquals(
                "newContainerSize: $newContainerSize. assert x",
                contentVisibleCenterPoint.x,
                newContentVisibleCenterPoint.x,
                1f
            )
            Assert.assertEquals(
                "newContainerSize: $newContainerSize. assert y",
                contentVisibleCenterPoint.y,
                newContentVisibleCenterPoint.y,
                1f
            )
        }
    }

    @Test
    fun testTransformAboutEquals() {
        val transform = TransformCompat(
            scale = ScaleFactorCompat(1.3487f, 8.44322f),
            offset = OffsetCompat(199.9872f, 80.232f),
        )

        transformAboutEquals(one = transform, two = transform.copy()).apply {
            Assert.assertTrue(this)
        }
        transformAboutEquals(
            one = transform,
            two = transform.copy(
                scale = ScaleFactorCompat(
                    transform.scaleX + 0.002f,
                    transform.scaleY - 0.003f
                )
            )
        ).apply {
            Assert.assertTrue(this)
        }
        transformAboutEquals(
            one = transform,
            two = transform.copy(
                scale = ScaleFactorCompat(
                    transform.scaleX + 0.008f,
                    transform.scaleY - 0.007f
                )
            )
        ).apply {
            Assert.assertFalse(this)
        }
        transformAboutEquals(
            one = transform,
            two = transform.copy(
                offset = OffsetCompat(
                    transform.offsetX + 0.002f,
                    transform.offsetY - 0.003f
                )
            )
        ).apply {
            Assert.assertTrue(this)
        }
        transformAboutEquals(
            one = transform,
            two = transform.copy(
                offset = OffsetCompat(
                    transform.offsetX + 0.008f,
                    transform.offsetY - 0.007f
                )
            )
        ).apply {
            Assert.assertFalse(this)
        }
    }
}