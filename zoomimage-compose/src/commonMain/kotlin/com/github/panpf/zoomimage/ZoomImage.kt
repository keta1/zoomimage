/*
 * Copyright (C) 2023 panpf <panpfpanpf@outlook.com>
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

package com.github.panpf.zoomimage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.github.panpf.zoomimage.compose.ZoomState
import com.github.panpf.zoomimage.compose.internal.MyImage
import com.github.panpf.zoomimage.compose.internal.round
import com.github.panpf.zoomimage.compose.rememberZoomState
import com.github.panpf.zoomimage.compose.subsampling.subsampling
import com.github.panpf.zoomimage.compose.zoom.ScrollBarSpec
import com.github.panpf.zoomimage.compose.zoom.mouseZoom
import com.github.panpf.zoomimage.compose.zoom.zoom
import com.github.panpf.zoomimage.compose.zoom.zoomScrollBar
import kotlin.math.roundToInt

/**
 * A native Image component that zoom and subsampling huge images
 *
 * Example usages:
 *
 * ```kotlin
 * val zoomState: ZoomState by rememberZoomState()
 * val context = LocalContext.current
 * LaunchedEffect(zoomState.subsampling) {
 *     val imageSource = ImageSource.fromResource(context, R.drawable.huge_image)
 *     zoomState.subsampling.setImageSource(imageSource)
 * }
 * ZoomImage(
 *     painter = painterResource(R.drawable.huge_image_thumbnail),
 *     contentDescription = "view image",
 *     modifier = Modifier.fillMaxSize(),
 *     zoomState = zoomState,
 * )
 * ```
 *
 * @param contentDescription text used by accessibility services to describe what this image
 * represents. This should always be provided unless this image is used for decorative purposes,
 * and does not represent a meaningful action that a user can take. This text should be
 * localized, such as by using androidx.compose.ui.res.stringResource or similar
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [Painter] in the given
 * bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [Painter]
 * @param alpha Optional opacity to be applied to the [Painter] when it is rendered onscreen
 * the default renders the [Painter] completely opaque
 * @param colorFilter Optional colorFilter to apply for the [Painter] when it is rendered onscreen
 * @param zoomState The state to control zoom
 * @param scrollBar Controls whether scroll bars are displayed and their style
 * @param onLongPress Called when the user long presses the image
 * @param onTap Called when the user taps the image
 * @see com.github.panpf.zoomimage.compose.common.test.ZoomImageTest
 */
@Composable
fun ZoomImage(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    zoomState: ZoomState = rememberZoomState(),
    scrollBar: ScrollBarSpec? = ScrollBarSpec.Default,
    onLongPress: ((Offset) -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null,
) {
    zoomState.zoomable.contentScale = contentScale
    zoomState.zoomable.alignment = alignment
    zoomState.zoomable.contentSize = remember(painter.intrinsicSize) {
        painter.intrinsicSize.round()
    }

    // It seems that mouseScrollScale must be inside BoxWithConstraints to take effect
    BoxWithConstraints(modifier = modifier.mouseZoom(zoomState.zoomable)) {
        /*
         * Here use BoxWithConstraints and then actively set containerSize,
         * In order to prepare the transform in advance, so that when the position of the image needs to be adjusted,
         * the position change will not be seen by the user
         */
        val density = LocalDensity.current
        val newContainerSize = remember(density, maxWidth, maxHeight) {
            val width = with(density) { maxWidth.toPx() }.roundToInt()
            val height = with(density) { maxHeight.toPx() }.roundToInt()
            IntSize(width = width, height = height)
        }
        zoomState.zoomable.containerSize = newContainerSize

        MyImage(
            painter = painter,
            contentDescription = contentDescription,
            alignment = Alignment.TopStart,
            contentScale = ContentScale.None,
            alpha = alpha,
            colorFilter = colorFilter,
            clipToBounds = false,
            modifier = Modifier
                .matchParentSize()
                .zoom(zoomState.zoomable, onLongPress = onLongPress, onTap = onTap)
                .subsampling(zoomState.zoomable, zoomState.subsampling),
        )

        if (scrollBar != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .zoomScrollBar(zoomState.zoomable, scrollBar)
            )
        }

        // TODO Shift plus Arrow key to move, Short press to move, long press to move continuously
    }
}