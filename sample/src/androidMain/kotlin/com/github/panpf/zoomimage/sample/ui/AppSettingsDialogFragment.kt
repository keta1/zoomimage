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

package com.github.panpf.zoomimage.sample.ui

import android.os.Bundle
import android.widget.ImageView.ScaleType
import android.widget.ImageView.ScaleType.MATRIX
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.panpf.assemblyadapter.recycler.AssemblyRecyclerAdapter
import com.github.panpf.tools4a.display.ktx.getDisplayMetrics
import com.github.panpf.zoomimage.sample.R
import com.github.panpf.zoomimage.sample.appSettings
import com.github.panpf.zoomimage.sample.databinding.FragmentRecyclerBinding
import com.github.panpf.zoomimage.sample.ui.base.view.BaseBindingDialogFragment
import com.github.panpf.zoomimage.sample.ui.common.view.list.ListSeparatorItemFactory
import com.github.panpf.zoomimage.sample.ui.common.view.menu.DropdownMenu
import com.github.panpf.zoomimage.sample.ui.common.view.menu.DropdownMenuItemFactory
import com.github.panpf.zoomimage.sample.ui.common.view.menu.MenuDivider
import com.github.panpf.zoomimage.sample.ui.common.view.menu.MenuDividerItemFactory
import com.github.panpf.zoomimage.sample.ui.common.view.menu.MultiChooseMenu
import com.github.panpf.zoomimage.sample.ui.common.view.menu.MultiChooseMenuItemFactory
import com.github.panpf.zoomimage.sample.ui.common.view.menu.SwitchMenuFlow
import com.github.panpf.zoomimage.sample.ui.common.view.menu.SwitchMenuItemFactory
import com.github.panpf.zoomimage.sample.ui.examples.view.ZoomViewType
import com.github.panpf.zoomimage.sample.util.repeatCollectWithLifecycle
import com.github.panpf.zoomimage.sample.viewImageLoaders
import com.github.panpf.zoomimage.util.Logger
import com.github.panpf.zoomimage.zoom.AlignmentCompat
import com.github.panpf.zoomimage.zoom.ContentScaleCompat
import com.github.panpf.zoomimage.zoom.ContinuousTransformType
import com.github.panpf.zoomimage.zoom.GestureType
import com.github.panpf.zoomimage.zoom.name

class AppSettingsDialogFragment : BaseBindingDialogFragment<FragmentRecyclerBinding>() {

    private val args by navArgs<AppSettingsDialogFragmentArgs>()
    private val zoomViewType by lazy { ZoomViewType.valueOf(args.zoomViewType) }
    private val appSettingsViewModel by viewModels<AppSettingsViewModel>()

    override fun onViewCreated(binding: FragmentRecyclerBinding, savedInstanceState: Bundle?) {
        val recyclerAdapter = AssemblyRecyclerAdapter(
            itemFactoryList = listOf(
                SwitchMenuItemFactory(),
                DropdownMenuItemFactory(requireActivity()),
                MultiChooseMenuItemFactory(requireActivity()),
                ListSeparatorItemFactory(),
                MenuDividerItemFactory(),
            ),
            initDataList = emptyList<Any>()
        )

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerAdapter
        }

        appSettingsViewModel.data.repeatCollectWithLifecycle(viewLifecycleOwner, State.CREATED) { dataList ->
            recyclerAdapter.submitList(dataList)

            val screenHeightPixels = requireContext().getDisplayMetrics().heightPixels
            val menuItemHeight = requireContext().resources.getDimension(R.dimen.menu_item_height)
            val dialogMaxHeight = screenHeightPixels * 0.8f
            if (dataList.size * menuItemHeight > dialogMaxHeight) {
                binding.recycler.updateLayoutParams {
                    height = dialogMaxHeight.toInt()
                }
            }
        }
    }
}