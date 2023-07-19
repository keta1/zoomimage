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
package com.github.panpf.zoomimage.subsampling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.github.panpf.zoomimage.Logger
import com.github.panpf.zoomimage.core.IntRectCompat
import com.github.panpf.zoomimage.core.IntSizeCompat
import com.github.panpf.zoomimage.core.internal.requiredMainThread
import com.github.panpf.zoomimage.core.internal.requiredWorkThread
import com.github.panpf.zoomimage.core.toShortString
import com.github.panpf.zoomimage.subsampling.internal.ExifOrientationHelper
import com.github.panpf.zoomimage.subsampling.internal.freeBitmap
import com.github.panpf.zoomimage.subsampling.internal.isInBitmapError
import com.github.panpf.zoomimage.subsampling.internal.isSrcRectError
import com.github.panpf.zoomimage.subsampling.internal.setInBitmapForRegion
import kotlinx.coroutines.runBlocking
import java.util.LinkedList

class TileDecoder constructor(
    logger: Logger,
    private val imageSource: ImageSource,
    private val tileBitmapPool: TileBitmapPool?,
    private val imageInfo: ImageInfo,
) {

    private val logger = logger.newLogger(module = "SubsamplingTileDecoder")
    private var destroyed = false
    private val decoderPool = LinkedList<BitmapRegionDecoder>()
    private val exifOrientationHelper = ExifOrientationHelper(imageInfo.exifOrientation)
    private val addedImageSize = exifOrientationHelper.addToSize(imageInfo.size)

    @WorkerThread
    fun decode(tile: Tile): Bitmap? {
        requiredWorkThread()
        if (destroyed) return null
        return useDecoder { decoder ->
            decodeRegion(decoder, tile.srcRect, tile.inSampleSize)
                ?.let { applyExifOrientation(it) }
        }
    }

    @WorkerThread
    private fun decodeRegion(
        regionDecoder: BitmapRegionDecoder,
        srcRect: IntRectCompat,
        inSampleSize: Int
    ): Bitmap? {
        requiredWorkThread()

        val imageSize = imageInfo.size
        val newSrcRect = exifOrientationHelper.addToRect(srcRect, imageSize)
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmapPool = tileBitmapPool
        bitmapPool?.setInBitmapForRegion(
            logger = logger,
            options = decodeOptions,
            regionSize = IntSizeCompat(newSrcRect.width, newSrcRect.height),
            imageMimeType = imageInfo.mimeType,
            imageSize = addedImageSize,
            caller = "tile:decodeRegion"
        )

        return try {
            regionDecoder.decodeRegion(newSrcRect.toAndroidRect(), decodeOptions)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            val inBitmap = decodeOptions.inBitmap
            if (inBitmap != null && isInBitmapError(throwable)) {
                logger.e {
                    "decodeRegion. Bitmap region decode inBitmap error. '${imageSource.key}'"
                }

                if (bitmapPool != null) {
                    bitmapPool.freeBitmap(
                        logger = logger,
                        bitmap = inBitmap,
                        caller = "tile:decodeRegion:error"
                    )
                } else {
                    inBitmap.recycle()
                }

                decodeOptions.inBitmap = null
                try {
                    regionDecoder.decodeRegion(newSrcRect.toAndroidRect(), decodeOptions)
                } catch (throwable1: Throwable) {
                    throwable1.printStackTrace()
                    logger.e(throwable) {
                        "decodeRegion. Bitmap region decode error. srcRect=${newSrcRect}. '${imageSource.key}'"
                    }
                    null
                }
            } else if (isSrcRectError(throwable)) {
                logger.e(throwable) {
                    "decodeRegion. Bitmap region decode srcRect error. " +
                            "imageSize=${imageSize.toShortString()}, " +
                            "srcRect=${newSrcRect.toShortString()}, " +
                            "inSampleSize=${decodeOptions.inSampleSize}. " +
                            "'${imageSource.key}'"
                }
                null
            } else {
                null
            }
        }
    }

    @WorkerThread
    private fun applyExifOrientation(bitmap: Bitmap): Bitmap {
        requiredWorkThread()

        val newBitmap = exifOrientationHelper.applyToBitmap(
            logger = logger,
            inBitmap = bitmap,
            bitmapPool = tileBitmapPool,
        )
        return if (newBitmap != null && newBitmap != bitmap) {
            val bitmapPool = tileBitmapPool
            if (bitmapPool != null) {
                bitmapPool.freeBitmap(
                    logger = logger,
                    bitmap = bitmap,
                    caller = "tile:applyExifOrientation"
                )
            } else {
                bitmap.recycle()
            }
            newBitmap
        } else {
            bitmap
        }
    }

    @MainThread
    fun destroy(caller: String) {
        requiredMainThread()
        if (destroyed) return
        destroyed = true
        logger.d { "destroy. $caller. '${imageSource.key}'" }
        synchronized(decoderPool) {
            decoderPool.forEach {
                it.recycle()
            }
            decoderPool.clear()
        }
    }

    @WorkerThread
    private fun useDecoder(block: (decoder: BitmapRegionDecoder) -> Bitmap?): Bitmap? {
        requiredWorkThread()

        synchronized(decoderPool) {
            if (destroyed) {
                return null
            }
        }

        var bitmapRegionDecoder: BitmapRegionDecoder? = synchronized(decoderPool) {
            decoderPool.poll()
        }
        if (bitmapRegionDecoder == null) {
            bitmapRegionDecoder = runBlocking {
                imageSource.openInputStream()
            }.getOrNull()?.buffered()?.use {
                if (VERSION.SDK_INT >= VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(it)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(it, false)
                }
            }
        }
        if (bitmapRegionDecoder == null) {
            return null
        }

        val bitmap = block(bitmapRegionDecoder)

        synchronized(decoderPool) {
            if (destroyed) {
                bitmapRegionDecoder.recycle()
            } else {
                decoderPool.add(bitmapRegionDecoder)
            }
        }

        return bitmap
    }

    private fun IntRectCompat.toAndroidRect(): Rect {
        return Rect(left, top, right, bottom)
    }
}