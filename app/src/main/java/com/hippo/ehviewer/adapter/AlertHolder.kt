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

package com.hippo.ehviewer.adapter

import android.support.annotation.CallSuper
import android.support.v7.widget.RecyclerView
import android.view.View

/*
 * Created by Hippo on 6/13/2017.
 */

abstract class AlertHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

  private var hasResumed = false

  internal var isAttached = false
    set(value) {
      if (field == value) return
      field = value
      if (value && isResumed) resume() else pause()
    }

  internal var isResumed = false
    set(value) {
      if (field == value) return
      field = value
      if (value && isAttached) resume() else pause()
    }


  private fun resume() {
    if (!hasResumed) {
      hasResumed = true
      onResume()
    }
  }

  private fun pause() {
    if (hasResumed) {
      hasResumed = false
      onPause()
    }
  }

  @CallSuper
  open fun onResume() {}

  @CallSuper
  open fun onPause() {}
}
