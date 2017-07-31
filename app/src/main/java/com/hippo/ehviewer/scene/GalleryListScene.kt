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

package com.hippo.ehviewer.scene

import com.hippo.ehviewer.mvp.EhvScene
import com.hippo.ehviewer.mvp.MvpPaper
import com.hippo.ehviewer.mvp.MvpPen
import com.hippo.ehviewer.slice.GalleryListPen
import com.hippo.ehviewer.slice.ToolbarPaper
import com.hippo.ehviewer.slice.ToolbarPen
import com.hippo.ehviewer.slice.galleryList
import com.hippo.ehviewer.slice.papers
import com.hippo.ehviewer.slice.pens
import com.hippo.ehviewer.slice.toolbar

/*
 * Created by Hippo on 2017/7/24.
 */

/**
 * Shows gallery list. Only this gallery list, can't switch to another list.
 */
class GalleryListScene : EhvScene() {

  private val toolbar: ToolbarPen = object : ToolbarPen() {}

  private val galleryList: GalleryListPen = object : GalleryListPen() {}

  private val pen = pens(toolbar, galleryList) {}

  override fun createPen(): MvpPen<*> = pen

  override fun createPaper(): MvpPaper<*> = papers(pen) {
    toolbar(toolbar, it) {
      galleryList(galleryList, ToolbarPaper.CONTAINER_ID)
    }
  }
}
