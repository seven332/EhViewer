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
import android.os.Build
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.find
import com.hippo.ehviewer.util.setPaddingTop

/*
 * Created by Hippo on 2017/8/8.
 */

class EhvDetailLayout : FrameLayout, View.OnLayoutChangeListener {

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  override fun onFinishInflate() {
    val header = find<EhvDetailHeader>(R.id.header)
    find<NestedScrollView>(R.id.scroll).setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int -> header.onScrollChange(scrollY) }
    find<EhvDetailPants>(R.id.pants).addOnLayoutChangeListener(this)
  }

  override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
    v.removeOnLayoutChangeListener(this)
    find<View>(R.id.body).setPaddingTop(bottom - top)
  }

  override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      val top = insets.systemWindowInsetTop
      find<EhvDetailPants>(R.id.pants).setAdditionalPaddingTop(top)
      find<View>(R.id.toolbar_status_bar).layoutParams.height = top
      find<View>(R.id.tip_status_bar).layoutParams.height = top
      find<View>(R.id.progress_status_bar).layoutParams.height = top
      insets.consumeSystemWindowInsets()
      return insets
    } else {
      return super.onApplyWindowInsets(insets)
    }
  }
}
