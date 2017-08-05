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
import android.util.AttributeSet
import com.hippo.ehviewer.EH_URL
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.COVER_SIZE_300
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.drawable.TextDrawable
import com.hippo.ehviewer.noi.NoiView
import com.hippo.ehviewer.util.attrColor

/*
 * Created by Hippo on 2017/7/29.
 */

class EhvCover : NoiView {

  var info: GalleryInfo? = null
    set(value) {
      if (field != value) {
        field = value
        val fingerprint = value?.coverFingerprint
        uri = if (fingerprint != null) EH_URL.coverUrl(fingerprint, COVER_SIZE_300) else null
      }
    }

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  init {
    val failure = TextDrawable("(;´Д`)", 0.8f)
    failure.backgroundColor = context.attrColor(R.attr.backgroundColorStatusBar)
    failure.textColor = context.attrColor(android.R.attr.textColorTertiary)
    failureDrawable = failure
  }
}
