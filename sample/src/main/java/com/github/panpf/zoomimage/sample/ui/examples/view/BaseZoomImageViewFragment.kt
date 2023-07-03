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
package com.github.panpf.zoomimage.sample.ui.examples.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView.ScaleType
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.github.panpf.sketch.decode.internal.exifOrientationName
import com.github.panpf.tools4j.io.ktx.formatFileSize
import com.github.panpf.zoomimage.core.Logger
import com.github.panpf.zoomimage.ZoomImageView
import com.github.panpf.zoomimage.sample.BuildConfig
import com.github.panpf.zoomimage.sample.R
import com.github.panpf.zoomimage.sample.databinding.ZoomImageViewCommonFragmentBinding
import com.github.panpf.zoomimage.sample.prefsService
import com.github.panpf.zoomimage.sample.ui.base.view.BindingFragment
import com.github.panpf.zoomimage.sample.ui.widget.view.ZoomImageMinimapView
import com.github.panpf.zoomimage.sample.util.collectWithLifecycle
import com.github.panpf.zoomimage.sample.util.format
import com.github.panpf.zoomimage.sample.util.toVeryShortString

abstract class BaseZoomImageViewFragment<VIEW_BINDING : ViewBinding> :
    BindingFragment<VIEW_BINDING>() {

    abstract val sketchImageUri: String

    abstract fun getZoomImageView(binding: VIEW_BINDING): ZoomImageView

    abstract fun getCommonBinding(binding: VIEW_BINDING): ZoomImageViewCommonFragmentBinding

    abstract val supportDisabledMemoryCache: Boolean

    abstract val supportIgnoreExifOrientation: Boolean

    abstract val supportDisallowReuseBitmap: Boolean

    override fun onViewCreated(binding: VIEW_BINDING, savedInstanceState: Bundle?) {
        val zoomImageView = getZoomImageView(binding)
        val common = getCommonBinding(binding)
        zoomImageView.apply {
            prefsService.scaleType.stateFlow.collectWithLifecycle(viewLifecycleOwner) {
                scaleType = ScaleType.valueOf(it)
            }
            zoomAbility.apply {
                logger.level = if (BuildConfig.DEBUG)
                    Logger.Level.DEBUG else Logger.Level.INFO
                prefsService.threeStepScaleEnabled.stateFlow.collectWithLifecycle(viewLifecycleOwner) {
                    threeStepScaleEnabled = it
                }
                prefsService.scrollBarEnabled.stateFlow.collectWithLifecycle(viewLifecycleOwner) {
                    scrollBarEnabled = it
                }
                prefsService.readModeEnabled.stateFlow.collectWithLifecycle(viewLifecycleOwner) {
                    readModeEnabled = it
                }
            }
            subsamplingAbility.apply {
                setLifecycle(viewLifecycleOwner.lifecycle)
                prefsService.showTileBounds.stateFlow.collectWithLifecycle(viewLifecycleOwner) {
                    showTileBounds = it
                }
            }
        }

        common.zoomImageViewErrorRetryButton.setOnClickListener {
            loadData(binding, common, sketchImageUri)
        }

        common.zoomImageViewTileMap.setZoomImageView(zoomImageView)

        common.zoomImageViewRotate.setOnClickListener {
            zoomImageView.zoomAbility.rotateBy(90)
        }

        common.zoomImageViewZoom.apply {
            setOnClickListener {
                val nextStepScale = zoomImageView.zoomAbility.getNextStepScale()
                zoomImageView.zoomAbility.scale(nextStepScale, true)
            }
            val resetIcon = {
                val currentScale = zoomImageView.zoomAbility.scale
                val nextStepScale = zoomImageView.zoomAbility.getNextStepScale()
                if (currentScale == nextStepScale || nextStepScale > currentScale) {
                    setImageResource(R.drawable.ic_zoom_in)
                } else {
                    setImageResource(R.drawable.ic_zoom_out)
                }
            }
            zoomImageView.zoomAbility.addOnScaleChangeListener { _, _, _ ->
                resetIcon()
            }
            resetIcon()
        }

        common.zoomImageViewInfo.setOnClickListener {
            ZoomImageViewInfoDialogFragment().apply {
                arguments = buildOtherInfo(zoomImageView, sketchImageUri).toBundle()
            }.show(childFragmentManager, null)
        }

        common.zoomImageViewSettings.setOnClickListener {
            ZoomImageViewOptionsDialogFragment().apply {
                arguments = ZoomImageViewOptionsDialogFragmentArgs(
                    supportMemoryCache = supportDisabledMemoryCache,
                    supportIgnoreExifOrientation = supportIgnoreExifOrientation,
                    supportReuseBitmap = supportDisallowReuseBitmap
                ).toBundle()
            }.show(childFragmentManager, null)
        }

        zoomImageView.zoomAbility.addOnMatrixChangeListener {
            updateInfo(zoomImageView, common)
        }
        zoomImageView.zoomAbility.addOnScaleChangeListener { _, _, _ ->
            updateInfo(zoomImageView, common)
        }
        updateInfo(zoomImageView, common)

        loadData(binding, common, sketchImageUri)
    }

    protected fun loadData(
        binding: VIEW_BINDING,
        common: ZoomImageViewCommonFragmentBinding,
        sketchImageUri: String
    ) {
        loadImage(
            binding = binding,
            onCallStart = {
                common.zoomImageViewProgress.isVisible = true
                common.zoomImageViewErrorLayout.isVisible = false
            },
            onCallSuccess = {
                common.zoomImageViewProgress.isVisible = false
                common.zoomImageViewErrorLayout.isVisible = false
            },
            onCallError = {
                common.zoomImageViewProgress.isVisible = false
                common.zoomImageViewErrorLayout.isVisible = true
            },
        )
        loadMinimap(common.zoomImageViewTileMap, sketchImageUri)
    }

    abstract fun loadImage(
        binding: VIEW_BINDING,
        onCallStart: () -> Unit,
        onCallSuccess: () -> Unit,
        onCallError: () -> Unit
    )

    abstract fun loadMinimap(
        zoomImageMinimapView: ZoomImageMinimapView,
        sketchImageUri: String
    )

    @SuppressLint("SetTextI18n")
    private fun updateInfo(
        zoomImageView: ZoomImageView,
        common: ZoomImageViewCommonFragmentBinding
    ) {
        common.zoomImageViewInfoHeaderText.text = """
                scale: 
                offset: 
                visible: 
            """.trimIndent()
        common.zoomImageViewInfoContentText.text = zoomImageView.zoomAbility.run {
            val scales = floatArrayOf(minScale, mediumScale, maxScale)
                .joinToString(prefix = "[", postfix = "]") { it.format(2).toString() }
            """
                ${scale.format(2)}(${displayScale.scaleX.format(2)}/${baseScale.scaleX.format(2)}) in $scales
                ${offset.run { "(${x.format(1)}, ${y.format(1)})" }}, edge=(${horScrollEdge}, ${verScrollEdge})
                ${getVisibleRect().toVeryShortString()}
            """.trimIndent()
        }
    }

    private fun buildOtherInfo(
        zoomImageView: ZoomImageView,
        sketchImageUri: String
    ): ZoomImageViewInfoDialogFragmentArgs {
        val zoomAbility = zoomImageView.zoomAbility
        val subsamplingAbility = zoomImageView.subsamplingAbility
        val tileList = subsamplingAbility.tileList ?: emptyList()
        val tilesByteCount = tileList.sumOf { it.bitmap?.byteCount ?: 0 }.toLong().formatFileSize()
        return ZoomImageViewInfoDialogFragmentArgs(
            imageUri = sketchImageUri,
            imageInfo = """
                size=${subsamplingAbility.imageSize?.toVeryShortString()}
                mimeType=${subsamplingAbility.imageMimeType}
                exifOrientation=${
                subsamplingAbility.imageExifOrientation?.let {
                    exifOrientationName(
                        it
                    )
                }
            }
            """.trimIndent(),
            sizeInfo = """
                view=${zoomAbility.viewSize.toVeryShortString()}
                drawable=${zoomAbility.drawableSize.toVeryShortString()}
            """.trimIndent(),
            tilesInfo = """
                tileCount=${tileList.size}
                tilesBytes=${tilesByteCount}
                loadedTileCount=${tileList.count { it.bitmap != null }}
            """.trimIndent()
        )
    }
}

