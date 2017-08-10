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

import com.hippo.ehviewer.mvp.EhvPen
import com.hippo.ehviewer.mvp.MvpLogic
import com.hippo.viewstate.ViewState

/*
 * Created by Hippo on 2017/8/5.
 */

interface ScrollLogic : MvpLogic<ScrollUi> {
  fun onScrollChange(scrollX: Int, scrollY: Int)
}

open class ScrollPen private constructor(
    val state: ScrollUiState
) : EhvPen<ScrollUi>(), ScrollLogic, ScrollUi by state {
  constructor() : this(ScrollUiState())

  override fun createViewState(): ViewState<ScrollUi> = state

  override fun onScrollChange(scrollX: Int, scrollY: Int) {}
}
