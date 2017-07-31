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

package com.hippo.ehviewer.widget.ehv

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.GenericDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.hippo.ehviewer.EH_URL
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.COVER_SIZE_200
import com.hippo.ehviewer.client.COVER_SIZE_250
import com.hippo.ehviewer.client.COVER_SIZE_300
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.drawable.TextDrawable
import com.hippo.ehviewer.util.attrColor

/*
 * Created by Hippo on 2017/7/29.
 */

class EhvCover : GenericDraweeView {

  private var isStarted = false

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  init {
    val failure = TextDrawable("(;´Д`)", 0.8f)
    failure.backgroundColor = context.attrColor(R.attr.backgroundColorStatusBar)
    failure.textColor = context.attrColor(android.R.attr.textColorTertiary)
    hierarchy.setFailureImage(failure)

    val retry = TextDrawable("(;´Д`)", 0.8f)
    retry.backgroundColor = context.attrColor(R.attr.backgroundColorStatusBar)
    retry.textColor = context.attrColor(android.R.attr.textColorTertiary)
    hierarchy.setRetryImage(retry)
  }

  fun start() {
    isStarted = true
    controller?.animatable?.start()
  }

  fun stop() {
    isStarted = false
    controller?.animatable?.stop()
  }

  private fun PipelineDraweeControllerBuilder.setFirstAvailableUrl(uris: List<String>): PipelineDraweeControllerBuilder {
    val requests = Array<ImageRequest>(uris.size) { ImageRequest.fromUri(uris[it]) }
    return setFirstAvailableImageRequests(requests)
  }

  fun load(info: GalleryInfo) {
    val uris = mutableListOf<String>()
    val coverFingerprint = info.coverFingerprint
    val coverUrl = info.coverUrl
    if (coverFingerprint != null) {
      uris.add(EH_URL.coverUrl(coverFingerprint, COVER_SIZE_300))
      uris.add(EH_URL.coverUrl(coverFingerprint, COVER_SIZE_250))
      uris.add(EH_URL.coverUrl(coverFingerprint, COVER_SIZE_200))
    } else if (coverUrl != null) {
      uris.add(coverUrl)
    }

    val controller = Fresco.newDraweeControllerBuilder()
        .setOldController(controller)
        .setTapToRetryEnabled(true)
        .setFirstAvailableUrl(uris)
        .setControllerListener(object : BaseControllerListener<ImageInfo>() {
          override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
            if (isStarted) animatable?.start()
          }
        })
        .build()
    setController(controller)
  }
}
