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

import android.os.Bundle
import com.hippo.ehviewer.client.GLUrlBuilder
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.mvp.EhvScene
import com.hippo.ehviewer.mvp.MvpPaper
import com.hippo.ehviewer.mvp.MvpPen
import com.hippo.ehviewer.slice.DumpPen
import com.hippo.ehviewer.slice.GalleryDetailPen
import com.hippo.ehviewer.slice.StatusBarPen
import com.hippo.ehviewer.slice.galleryDetail
import com.hippo.ehviewer.slice.papers
import com.hippo.ehviewer.slice.pens

/*
 * Created by Hippo on 2017/7/24.
 */

/**
 * Shows gallery detail.
 */
class GalleryDetailScene : EhvScene() {

  companion object {
    val KEY_GALLERY_INFO = "GalleryDetailScene:gallery_info"
  }

  private val statusBar = object : StatusBarPen() {}

  private val galleryDetail = object : GalleryDetailPen() {
    override fun onClickTag(namespace: String, tag: String) {
      stage?.let { stage ->
        val builder = GLUrlBuilder()
        builder.tags.add(namespace, tag)
        stage.pushScene(galleryList(builder))
      }
    }
  }

  private lateinit var pen: DumpPen

  override fun createPen(args: Bundle): MvpPen<*> = pens(statusBar, galleryDetail) {
    val info = args.getParcelable<GalleryInfo>(KEY_GALLERY_INFO)
    if (info != null) {
      galleryDetail.init(info)
    } else {
      // TODO
    }
  }.apply { pen = this }

  override fun createPaper(): MvpPaper<*> = papers(pen) {
    galleryDetail(galleryDetail, it)
  }
}
