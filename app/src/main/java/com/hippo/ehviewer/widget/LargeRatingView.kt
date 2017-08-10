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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.clamp
import com.hippo.ehviewer.util.dimensionPixelSize
import com.hippo.ehviewer.util.drawable

/*
 * Created by Hippo on 2017/8/9.
 */

class LargeRatingView : View {

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  private val starSize: Int = context.dimensionPixelSize(R.dimen.large_rating_size)
  private var starInterval: Int = context.dimensionPixelSize(R.dimen.large_rating_interval)

  private val starDrawable: Drawable = context.drawable(R.drawable.star_x16).apply { setBounds(0, 0, starSize, starSize) }
  private val starHalfDrawable: Drawable = context.drawable(R.drawable.star_half_x16).apply { setBounds(0, 0, starSize, starSize) }
  private val starOutlineDrawable: Drawable = context.drawable(R.drawable.star_outline_x16).apply { setBounds(0, 0, starSize, starSize) }

  var rating: Int = 0
    set(value) {
      val rating = value.clamp(0, 10)
      if (field != rating) {
        field = rating
        invalidate()
      }
    }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
      return false
    }

    val x = event.x - paddingLeft
    val halfStarSize = starSize / 2

    if (x < 0) {
      rating = 0
      return true
    }

    for (i in 0 until 5) {
      if (x < (starSize + starInterval) * i + halfStarSize) {
        rating = 2 * i + 1
        return true
      } else if (x < (starSize + starInterval) * i + starSize) {
        rating = 2 * i + 2
        return true
      }
    }

    rating = 10
    return true
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(starSize * 5 + starInterval * 4 + paddingLeft + paddingRight, starSize + paddingTop + paddingBottom)
  }

  override fun onDraw(canvas: Canvas) {
    val step = starSize + starInterval
    var numStar = rating / 2
    val numStarHalf = rating % 2
    val saved = canvas.save()
    canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
    while (numStar-- > 0) {
      starDrawable.draw(canvas)
      canvas.translate(step.toFloat(), 0f)
    }
    if (numStarHalf == 1) {
      starHalfDrawable.draw(canvas)
      canvas.translate(step.toFloat(), 0f)
    }
    var numOutline = 5 - numStar - numStarHalf
    while (numOutline-- > 0) {
      starOutlineDrawable.draw(canvas)
      canvas.translate(step.toFloat(), 0f)
    }
    canvas.restoreToCount(saved)
  }
}
