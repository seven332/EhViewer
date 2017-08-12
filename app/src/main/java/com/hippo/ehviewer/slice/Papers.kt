/*
 * Copyright 2017 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.slice

import android.view.LayoutInflater
import android.view.ViewGroup
import com.hippo.ehviewer.mvp.GroupPaper
import com.hippo.ehviewer.mvp.MvpPaper

/*
 * Created by Hippo on 2017/7/24.
 */

fun papers(
    logic: DumpLogic,
    onCreate: GroupPaper<DumpUi>.(ViewGroup) -> MvpPaper<*>
) = object : DumpPaper(logic) {
  override fun onCreate(inflater: LayoutInflater, container: ViewGroup) {
    super.onCreate(inflater, container)
    val paper = onCreate(container)
    addChild(paper)
    view = paper.view
  }
}

fun GroupPaper<*>.drawer(
    logic: DrawerLogic,
    container: ViewGroup,
    init: DrawerPaper.() -> Unit
) = DrawerPaper(logic).also { it.create(inflater, container); it.init() }

fun GroupPaper<*>.drawer(
    logic: DrawerLogic,
    containerId: Int,
    init: DrawerPaper.() -> Unit
) = inflateChild(containerId) { container -> DrawerPaper(logic).also { it.create(inflater, container) } }.apply { init() }

fun GroupPaper<*>.statusBar(
    logic: StatusBarLogic,
    container: ViewGroup,
    init: StatusBarPaper.() -> Unit
) = StatusBarPaper(logic).also { it.create(inflater, container); it.init() }

fun GroupPaper<*>.statusBar(
    logic: StatusBarLogic,
    containerId: Int,
    init: StatusBarPaper.() -> Unit
) = inflateChild(containerId) { container -> StatusBarPaper(logic).also { it.create(inflater, container) } }.apply { init() }

fun GroupPaper<*>.toolbar(
    logic: ToolbarLogic,
    container: ViewGroup,
    init: ToolbarPaper.() -> Unit
) = ToolbarPaper(logic).also { it.create(inflater, container); it.init() }

fun GroupPaper<*>.toolbar(
    logic: ToolbarLogic,
    containerId: Int,
    init: ToolbarPaper.() -> Unit
) = inflateChild(containerId) { container -> ToolbarPaper(logic).also { it.create(inflater, container) } }.apply { init() }

fun GroupPaper<*>.galleryList(
    logic: GalleryListLogic,
    containerId: Int
) = inflateChild(containerId) { container -> GalleryListPaper(logic).also { it.create(inflater, container) } }

fun GroupPaper<*>.galleryDetail(
    logic: GalleryDetailLogic,
    container: ViewGroup
) = GalleryDetailPaper(logic).also { it.create(inflater, container) }

fun GroupPaper<*>.galleryDetail(
    logic: GalleryDetailLogic,
    containerId: Int
) = inflateChild(containerId) { container -> GalleryDetailPaper(logic).also { it.create(inflater, container) } }
