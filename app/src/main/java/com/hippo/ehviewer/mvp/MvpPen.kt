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

package com.hippo.ehviewer.mvp

import android.support.annotation.CallSuper
import com.hippo.viewstate.ViewState

/*
 * Created by Hippo on 2017/7/14.
 */

abstract class MvpPen<Ui : Any> : MvpLogic<Ui> {

  lateinit var view: Ui
    private set
  private lateinit var state: ViewState<Ui>

  override fun attach(ui: Ui) {
    state.attach(ui)
  }

  /**
   * Detaches the paper of the pen.
   */
  override fun detach() {
    state.detach()
  }

  override fun isRestoring(): Boolean = state.isRestoring

  /**
   * Creates view state for this pen.
   */
  abstract fun createViewState(): ViewState<Ui>

  /**
   * Creates the pen.
   */
  internal fun create() {
    state = createViewState()
    @Suppress("UNCHECKED_CAST")
    view = state as Ui
    onCreate()
  }

  /**
   * Destroys the pen.
   */
  internal fun destroy() {
    onDestroy()
  }

  /**
   * Called when the pen created.
   */
  @CallSuper
  protected open fun onCreate() {}

  /**
   * Called when the pen destroyed.
   */
  @CallSuper
  protected open fun onDestroy() {}
}
