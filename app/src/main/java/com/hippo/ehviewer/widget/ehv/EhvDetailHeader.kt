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
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.DURATION_IN
import com.hippo.ehviewer.util.DURATION_OUT
import com.hippo.ehviewer.util.find

/*
 * Created by Hippo on 2017/8/8.
 */

class EhvDetailHeader : RelativeLayout, View.OnLayoutChangeListener {

  private lateinit var pants: EhvDetailPants
  private lateinit var mask: View
  private lateinit var statusBar: View
  private lateinit var toolbar: EhvToolbar
  private lateinit var shadow: View

  private var pantsBottom = 0
  private var toolbarBottom = 0
  private var bodyScrollY = 0

  private var headerScrollY = -1
  private var showToolbar = true

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  override fun onFinishInflate() {
    pants = find(R.id.pants)
    mask = find(R.id.mask)
    statusBar = find(R.id.toolbar_status_bar)
    toolbar = find(R.id.toolbar)
    shadow = find(R.id.shadow)
    addOnLayoutChangeListener(this)
  }

  fun onScrollChange(scrollY: Int) {
    bodyScrollY = scrollY
    syncScrollY(true)
  }

  override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
    removeOnLayoutChangeListener(this)
    pantsBottom = pants.bottom
    toolbarBottom = toolbar.bottom

    // Update mask height
    mask.layoutParams.height = pants.height
    post { mask.requestLayout() }

    syncScrollY(false)
  }

  private fun View.updateTopMargin(topMargin: Int) {
    (layoutParams as LayoutParams).topMargin = topMargin
    if (ViewCompat.isLaidOut(this)) {
      offsetTopAndBottom(topMargin - top)
    } else {
      post { requestLayout() }
    }
  }

  private fun syncScrollY(animation: Boolean) {
    if (toolbarBottom == 0 || pantsBottom == 0) return

    // Update position
    val headerScrollY = Math.min(bodyScrollY, pantsBottom - toolbarBottom)
    if (this.headerScrollY != headerScrollY) {
      this.headerScrollY = headerScrollY
      pants.updateTopMargin(-headerScrollY)
      mask.updateTopMargin(-headerScrollY)
      shadow.updateTopMargin(-headerScrollY + pantsBottom)
    }

    // Update visibility
    val showToolbar = pantsBottom - bodyScrollY <= toolbarBottom
    if (this.showToolbar != showToolbar) {
      this.showToolbar = showToolbar
      mask.animate().cancel()
      statusBar.animate().cancel()
      toolbar.animate().cancel()
      val alpha = if (showToolbar) 1.0f else 0.0f
      val duration = if (showToolbar) DURATION_IN else DURATION_OUT
      if (animation) {
        mask.animate().alpha(alpha).setDuration(duration).start()
        statusBar.animate().alpha(alpha).setDuration(duration).start()
        toolbar.animate().alpha(alpha).setDuration(duration).start()
      } else {
        mask.alpha = alpha
        statusBar.alpha = alpha
        toolbar.alpha = alpha
      }

      pants.setTouchEnabled(!showToolbar)
      toolbar.setTouchEnabled(showToolbar)
    }
  }
}
