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
import com.hippo.ehviewer.mvp.MvpPaper
import com.hippo.ehviewer.widget.content.ContentDataAdapter
import io.reactivex.Observable

/*
 * Created by Hippo on 6/13/2017.
 */

abstract class AlertAdapter<T : Any, VH: AlertHolder>(
    lifecycle: Observable<Int>
) : ContentDataAdapter<T, VH>() {

  private val holderList = mutableListOf<VH>()

  private var isResumed = false
    set(value) {
      field = value
      holderList.forEach { it.isResumed = value }
    }

  init {
    lifecycle.subscribe({
      when (it) {
        MvpPaper.RESUME -> isResumed = true
        MvpPaper.PAUSE -> isResumed = false
        MvpPaper.DESTROY -> {
          holderList.clear()
        }
      }
    }, { /* Ignore error */ })
  }

  @CallSuper
  override fun onBindViewHolder(holder: VH, position: Int) {
    if (isResumed) {
      holder.isResumed = true
    }
    holderList.add(holder)
  }

  override fun onViewAttachedToWindow(holder: VH) {
    super.onViewAttachedToWindow(holder)
    holder.isAttached = true
  }

  override fun onViewDetachedFromWindow(holder: VH) {
    super.onViewDetachedFromWindow(holder)
    holder.isAttached = false
  }

  @CallSuper
  override fun onViewRecycled(holder: VH) {
    super.onViewRecycled(holder)
    holderList.remove(holder)
    holder.isAttached = false
    holder.isResumed = false
  }
}
