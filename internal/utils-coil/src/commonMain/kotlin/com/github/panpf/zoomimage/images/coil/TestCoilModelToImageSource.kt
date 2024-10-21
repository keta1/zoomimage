package com.github.panpf.zoomimage.images.coil

import coil3.Extras
import coil3.ImageLoader
import coil3.PlatformContext
import com.github.panpf.zoomimage.coil.CoilModelToImageSource
import com.github.panpf.zoomimage.subsampling.ImageSource

class TestCoilModelToImageSource : CoilModelToImageSource {

    override suspend fun modelToImageSource(
        context: PlatformContext,
        imageLoader: ImageLoader,
        model: Any,
        extras: Extras
    ): ImageSource.Factory? {
        return null
    }
}