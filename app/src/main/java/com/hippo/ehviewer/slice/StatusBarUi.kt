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

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.LinearLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.mvp.GroupPaper
import com.hippo.ehviewer.mvp.MvpUi
import com.hippo.viewstate.GenerateViewState

/*
 * Created by Hippo on 2017/8/6.
 */

@GenerateViewState
interface StatusBarUi : MvpUi

class StatusBarPaper(
    private val logic: StatusBarLogic
) : GroupPaper<StatusBarPaper>(logic), StatusBarUi {

  companion object {
    const val CONTAINER_ID = R.id.status_bar_container
  }

  override fun onCreate(inflater: LayoutInflater, container: ViewGroup) {
    super.onCreate(inflater, container)
    view = inflater.inflate(R.layout.paper_status_bar, container, false)
  }

  class StatusBarLayout : LinearLayout {

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        getChildAt(0)?.layoutParams?.height = insets.systemWindowInsetTop
        insets.consumeSystemWindowInsets()
      }
      return insets
    }
  }
}
