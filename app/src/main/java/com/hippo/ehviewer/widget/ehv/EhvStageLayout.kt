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
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import com.hippo.stage.StageLayout

/*
 * Created by Hippo on 2017/8/6.
 */

class EhvStageLayout : StageLayout {

  private var insets: WindowInsets? = null

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      fitsSystemWindows = true
      systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
  }

  override fun onViewAdded(child: View) {
    super.onViewAdded(child)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && insets != null) {
      child.dispatchApplyWindowInsets(WindowInsets(insets))
    }
  }

  override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      // Back up inserts
      this.insets = WindowInsets(insets)

      for (i in 0 until childCount) {
        getChildAt(i).dispatchApplyWindowInsets(WindowInsets(insets))
      }

      return onApplyWindowInsets(insets)
    } else {
      return super.dispatchApplyWindowInsets(insets)
    }
  }

  override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      insets.consumeSystemWindowInsets()
      return insets
    } else {
      return super.onApplyWindowInsets(insets)
    }
  }
}
