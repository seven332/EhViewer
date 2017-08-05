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

import android.view.MenuItem
import com.hippo.ehviewer.mvp.EhvPen
import com.hippo.ehviewer.mvp.MvpLogic
import com.hippo.viewstate.ViewState

/*
 * Created by Hippo on 2017/7/24.
 */

interface ToolbarLogic : MvpLogic<ToolbarUi> {

  fun onClickNavigationIcon()

  fun onClickMenuItem(item: MenuItem): Boolean

  fun onDoubleClick()
}

open class ToolbarPen(
    val state: ToolbarUiState = ToolbarUiState()
) : EhvPen<ToolbarUi>(), ToolbarLogic, ToolbarUi by state {

  override fun createViewState(): ViewState<ToolbarUi> = state

  override fun onClickNavigationIcon() {}
  override fun onClickMenuItem(item: MenuItem): Boolean { return false }
  override fun onDoubleClick() {}
}
