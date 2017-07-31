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

package com.hippo.ehviewer.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.hippo.ehviewer.R
import com.hippo.ehviewer.drawable.ProgressDrawable
import com.hippo.ehviewer.util.MAX_LEVEL
import com.hippo.ehviewer.util.getSuitableSize

/*
 * Created by Hippo on 6/7/2017.
 */

/**
 * `ProgressView` shows a circle progress. It could be indeterminate or stable.
 */
class ProgressView : View {

  private val drawable = ProgressDrawable()

  var indeterminate: Boolean
    get() = drawable.indeterminate
    set(value) { drawable.indeterminate = value }

  var progress: Int
    get() = drawable.level
    set(value) {
      check(value in 0..MAX_LEVEL, { "`progress` must in [0, MAX_LEVEL]" })
      // Drawable.onLevelChange() may not called, set indeterminate to false explicitly
      drawable.indeterminate = false
      drawable.level = value
      invalidate()
    }

  var color: Int
    get() = drawable.color
    set(value) { drawable.color = value }

  constructor(context: Context): super(context)

  constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
    val ta = context.obtainStyledAttributes(attrs, R.styleable.ProgressView, 0, 0)
    color = ta.getColor(R.styleable.ProgressView_color, Color.BLACK)
    val indeterminate = ta.getBoolean(R.styleable.ProgressView_indeterminate, true)
    val progress = ta.getInteger(R.styleable.ProgressView_progress, -1)
    if (progress in 0..MAX_LEVEL) {
      this.progress = progress
    } else if (indeterminate) {
      this.indeterminate = indeterminate
    }
    ta.recycle()
  }

  init {
    drawable.callback = this
  }

  override fun verifyDrawable(who: Drawable?): Boolean {
    return super.verifyDrawable(who) || who === drawable
  }

  override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
    super.onSizeChanged(w, h, oldW, oldH)
    drawable.setBounds(paddingLeft, paddingTop, w - paddingRight, h - paddingBottom)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(getSuitableSize(suggestedMinimumWidth, widthMeasureSpec),
        getSuitableSize(suggestedMinimumHeight, heightMeasureSpec))
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)
    drawable.draw(canvas)
  }
}
