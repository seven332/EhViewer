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

package com.hippo.ehviewer.widget.recyclerview

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

/*
 * Created by Hippo on 2017/7/28.
 */

class AutoGridLayoutManager(context: Context) : GridLayoutManager(context, 1) {

  private var columnSize = -1
  private var columnSizeChanged = true

  init {
    setColumnSize(columnSize)
  }

  /**
   * Set column size to change span count.
   */
  fun setColumnSize(columnSize: Int) {
    if (columnSize == this.columnSize) {
      return
    }
    this.columnSize = columnSize
    this.columnSizeChanged = true
    requestLayout()
  }

  private fun calculateSpanCount(total: Int, single: Int): Int {
    val span = total / single
    return if (span <= 0) 1 else span
  }

  override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
    if (columnSizeChanged && columnSize > 0) {
      val totalSpace: Int
      if (orientation == LinearLayoutManager.VERTICAL) {
        totalSpace = width - paddingRight - paddingLeft
      } else {
        totalSpace = height - paddingTop - paddingBottom
      }

      val spanCount = calculateSpanCount(totalSpace, columnSize)
      setSpanCount(spanCount)
      columnSizeChanged = false
    }
    super.onLayoutChildren(recycler, state)
  }
}
