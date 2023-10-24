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

package com.github.panpf.zoomimage.subsampling.internal

import com.github.panpf.zoomimage.subsampling.Tile
import com.github.panpf.zoomimage.util.IntOffsetCompat
import com.github.panpf.zoomimage.util.IntRectCompat
import com.github.panpf.zoomimage.util.IntSizeCompat
import com.github.panpf.zoomimage.util.isEmpty
import com.github.panpf.zoomimage.util.limitTo
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Calculates the preferred size of the tile based on the container size, typically half the container size
 *
 * @see [com.github.panpf.zoomimage.core.test.subsampling.internal.TileManageUtilsTest.testCalculatePreferredTileSize]
 */
internal fun calculatePreferredTileSize(containerSize: IntSizeCompat): IntSizeCompat {
    return containerSize / 2
}

/**
 * Calculates the sample size of the subsampling when the specified scaling factor is calculated
 *
 * If the size of the thumbnail is the original image divided by 16, then when the scaling factor is from 1.0 to 17.9, the node that changes the sample size is [[1.0:16, 1.5:8, 2.9:4, 5.7:2, 11.1:1]]
 *
 * @see [com.github.panpf.zoomimage.core.test.subsampling.internal.TileManageUtilsTest.testFindSampleSize]
 */
internal fun findSampleSize(
    imageSize: IntSizeCompat,
    thumbnailSize: IntSizeCompat,
    scale: Float
): Int {
    // A scale less than 1f indicates that the thumbnail is not enlarged, so subsampling is not required
    if (imageSize.isEmpty() || thumbnailSize.isEmpty() || scale <= 0) {
        return 0
    }
    val scaledFactor = (imageSize.width / (thumbnailSize.width * scale)).format(1)
    val sampleSize = closestPowerOfTwo(scaledFactor)
    @Suppress("UnnecessaryVariable") val limitedSampleSize = sampleSize.coerceAtLeast(1)
    return limitedSampleSize
}

/**
 * Find the closest power of two to the given number, rounding up.
 *
 * Results from 1.0 to 17.9 are [[1.0:1, 1.5:2, 2.9:4, 5.7:8, 11.4:16]]
 *
 * @see [com.github.panpf.zoomimage.core.test.subsampling.internal.TileManageUtilsTest.testClosestPowerOfTwo]
 */
internal fun closestPowerOfTwo(number: Float): Int {
    val logValue = log2(number) // Takes the logarithm of the input number
    val closestInteger = logValue.roundToInt()  // Take the nearest integer
    val powerOfTwo = 2.0.pow(closestInteger)    // Calculate the nearest power of 2
    return powerOfTwo.toInt()
}

/**
 * Calculates the size of the tile grid based on the [sampleSize] and [preferredTileSize].
 * In addition, the calculation result is not allowed to exceed the [maxGridSize] limit
 *
 * @see [com.github.panpf.zoomimage.core.test.subsampling.internal.TileManageUtilsTest.testCalculateGridSize]
 */
internal fun calculateGridSize(
    imageSize: IntSizeCompat,
    preferredTileSize: IntSizeCompat,
    sampleSize: Int,
    maxGridSize: IntOffsetCompat? = null
): IntOffsetCompat {
    val xTiles =
        ceil((imageSize.width / sampleSize.toFloat()) / preferredTileSize.width.toFloat()).toInt()
    val yTiles =
        ceil((imageSize.height / sampleSize.toFloat()) / preferredTileSize.height.toFloat()).toInt()
    val initialGridSize = IntOffsetCompat(xTiles, yTiles)
    val gridSize = if (maxGridSize != null) {
        IntOffsetCompat(
            initialGridSize.x.coerceAtMost(maxGridSize.x),
            initialGridSize.y.coerceAtMost(maxGridSize.y)
        )
    } else {
        initialGridSize
    }
    return gridSize
}

/**
 * Calculates a list of tiles with different sample sizes based on the [preferredTileSize].
 * Also, the grid size will never exceed 150x150.
 * The result is a Map sorted by sample size from largest to smallest
 *
 * @see [com.github.panpf.zoomimage.core.test.subsampling.internal.TileManageUtilsTest.testCalculateTileGridMap]
 */
// todo Improve the calculation of the tile grid and no longer rely on containerSize, so that containerSize does not need to be reset when it changes
internal fun calculateTileGridMap(
    imageSize: IntSizeCompat,
    preferredTileSize: IntSizeCompat,
): Map<Int, List<Tile>> {
    val tileMap = HashMap<Int, List<Tile>>()
    val singleDirectionMaxTiles = 150
    val maxGridSize = if (imageSize.width > imageSize.height) {
        IntOffsetCompat(
            x = singleDirectionMaxTiles,
            y = (imageSize.height / imageSize.width.toFloat() * singleDirectionMaxTiles).roundToInt()
        )
    } else {
        IntOffsetCompat(
            x = (imageSize.width / imageSize.height.toFloat() * singleDirectionMaxTiles).roundToInt(),
            y = singleDirectionMaxTiles
        )
    }
    var sampleSize = 1
    do {
        val gridSize = calculateGridSize(
            imageSize = imageSize,
            preferredTileSize = preferredTileSize,
            sampleSize = sampleSize,
            maxGridSize = maxGridSize
        )
        val tileWidth: Int = ceil(imageSize.width / gridSize.x.toFloat()).toInt()
        val tileHeight: Int = ceil(imageSize.height / gridSize.y.toFloat()).toInt()
        val tileList = ArrayList<Tile>(gridSize.x * gridSize.y)
        var xCoordinate = 0
        var yCoordinate = 0
        do {
            val coordinate = IntOffsetCompat(xCoordinate, yCoordinate)
            val left = xCoordinate * tileWidth
            val top = yCoordinate * tileHeight
            val srcRect = IntRectCompat(
                left = left,
                top = top,
                right = (left + tileWidth).coerceAtMost(imageSize.width),
                bottom = (top + tileHeight).coerceAtMost(imageSize.height)
            )
            tileList.add(Tile(coordinate = coordinate, srcRect = srcRect, sampleSize = sampleSize))
            if (xCoordinate < gridSize.x - 1) {
                xCoordinate++
            } else {
                xCoordinate = 0
                yCoordinate++
            }
        } while (yCoordinate <= gridSize.y - 1)
        tileMap[sampleSize] = tileList
        sampleSize *= 2
    } while (gridSize.x * gridSize.y > 1)
    return tileMap.toSortedMap { o1, o2 -> (o1 - o2) * -1 }
}

/**
 * The area that needs to be loaded on the original image is calculated from the area currently visible to the thumbnail, which is usually larger than the visible area, usually half the [preferredTileSize].
 *
 * @see [com.github.panpf.zoomimage.core.test.subsampling.internal.TileManageUtilsTest.testCalculateImageLoadRect]
 */
internal fun calculateImageLoadRect(
    imageSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    preferredTileSize: IntSizeCompat,
    contentVisibleRect: IntRectCompat
): IntRectCompat {
    if (imageSize.isEmpty() || contentSize.isEmpty() || contentVisibleRect.isEmpty) {
        return IntRectCompat.Zero
    }
    val widthScale = imageSize.width / contentSize.width.toFloat()
    val heightScale = imageSize.height / contentSize.height.toFloat()
    val imageVisibleRect = IntRectCompat(
        left = floor(contentVisibleRect.left * widthScale).toInt(),
        top = floor(contentVisibleRect.top * heightScale).toInt(),
        right = ceil(contentVisibleRect.right * widthScale).toInt(),
        bottom = ceil(contentVisibleRect.bottom * heightScale).toInt()
    )
    /*
     * Increase the visible area as the loading area,
     * this preloads tiles around the visible area,
     * the user will no longer feel the loading process while sliding slowly
     */
    val horExtend = preferredTileSize.width / 2f
    val verExtend = preferredTileSize.height / 2f
    val imageLoadRect = IntRectCompat(
        left = floor(imageVisibleRect.left - horExtend).toInt(),
        top = floor(imageVisibleRect.top - verExtend).toInt(),
        right = ceil(imageVisibleRect.right + horExtend).toInt(),
        bottom = ceil(imageVisibleRect.bottom + verExtend).toInt(),
    )
    @Suppress("UnnecessaryVariable") val limitedImageLoadRect = imageLoadRect.limitTo(imageSize)
    return limitedImageLoadRect
}