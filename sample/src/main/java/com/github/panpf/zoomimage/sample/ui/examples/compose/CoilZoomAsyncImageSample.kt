package com.github.panpf.zoomimage.sample.ui.examples.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil.request.ImageRequest
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomAsyncImageLogger
import com.github.panpf.zoomimage.sample.R
import com.github.panpf.zoomimage.sample.util.sketchUri2CoilModel

@Composable
fun CoilZoomAsyncImageSample(sketchImageUri: String) {
    BaseZoomImageSample(
        logger = rememberCoilZoomAsyncImageLogger(),
        sketchImageUri = sketchImageUri,
        supportIgnoreExifOrientation = false
    ) { contentScale, alignment, zoomableState, subsamplingState, _, scrollBarSpec, onLongPress ->
        val context = LocalContext.current
        val coilData =
            remember(key1 = sketchImageUri) { sketchUri2CoilModel(context, sketchImageUri) }
        CoilZoomAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).apply {
                data(coilData)
                crossfade(true)
            }.build(),
            contentDescription = "CoilZoomAsyncImage",
            contentScale = contentScale,
            alignment = alignment,
            modifier = Modifier.fillMaxSize(),
            zoomableState = zoomableState,
            subsamplingState = subsamplingState,
            scrollBarSpec = scrollBarSpec,
            onLongPress = onLongPress,
        )
    }
}

@Preview
@Composable
private fun CoilZoomAsyncImageSamplePreview() {
    CoilZoomAsyncImageSample(newResourceUri(R.drawable.im_placeholder))
}