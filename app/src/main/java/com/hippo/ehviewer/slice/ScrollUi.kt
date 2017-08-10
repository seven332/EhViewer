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

import android.support.v4.widget.NestedScrollView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.hippo.ehviewer.R
import com.hippo.ehviewer.mvp.GroupPaper
import com.hippo.ehviewer.mvp.MvpUi
import com.hippo.ehviewer.util.find
import com.hippo.viewstate.GenerateViewState
import com.hippo.viewstate.strategy.SingleByMethod
import com.hippo.viewstate.strategy.StrategyType

/*
 * Created by Hippo on 2017/8/5.
 */

@GenerateViewState
interface ScrollUi : MvpUi {

  @StrategyType(value = SingleByMethod::class)
  fun setScrollChangeEnabled()
}

class ScrollPaper(
    private val logic: ScrollLogic
) : GroupPaper<ScrollPaper>(logic), ScrollUi {

  companion object {
    const val CONTAINER_ID = R.id.scroll_container
  }

  private lateinit var scroll: NestedScrollView

  override fun onCreate(inflater: LayoutInflater, container: ViewGroup) {
    super.onCreate(inflater, container)

    view = inflater.inflate(R.layout.paper_scroll, container, false)
    scroll = view.find(R.id.scroll)
  }

  override fun setScrollChangeEnabled() {
    scroll.setOnScrollChangeListener { _: NestedScrollView?, scrollX: Int, scrollY: Int, _: Int, _: Int ->
      logic.onScrollChange(scrollX, scrollY)
    }
  }
}
