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

package com.hippo.ehviewer.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.hippo.drawerlayout.DrawerLayoutChild
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.attrDimensionPixelSize

/*
 * Created by Hippo on 2017/7/16.
 */

class Drawer : FrameLayout, DrawerLayoutChild {

  companion object {
    const val NONE = 0
    const val FIT = 1
    const val ACTION_BAR = 2
  }

  private var windowPaddingTop: Int = 0
  private var windowPaddingBottom: Int = 0

  var mode = NONE
    set(value) {
      if (field != value) {
        field = value
        requestLayout()
      }
    }

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  override fun onGetWindowPadding(windowPaddingTop: Int, windowPaddingBottom: Int) {
    this.windowPaddingTop = windowPaddingTop
    this.windowPaddingBottom = windowPaddingBottom
  }

  override fun getAdditionalBottomMargin() = 0

  override fun getAdditionalTopMargin() = when (mode) {
    FIT -> windowPaddingTop
    ACTION_BAR -> windowPaddingTop + context.attrDimensionPixelSize(R.attr.actionBarSize)
    else -> 0
  }
}
