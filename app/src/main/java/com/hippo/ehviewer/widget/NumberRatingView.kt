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
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.dimensionPixelSize
import com.hippo.ehviewer.util.drawable

/*
 * Created by Hippo on 2017/7/31.
 */

class NumberRatingView : View {

  private var starSize: Int = context.dimensionPixelSize(R.dimen.number_rating_size)
  private var starInterval: Int = context.dimensionPixelSize(R.dimen.number_rating_interval)
  private var star: Drawable = context.drawable(R.drawable.star_white_x16).apply { setBounds(0, 0, starSize, starSize) }

  private var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = starSize.toFloat(); color = Color.WHITE }
  private var bounds: Rect = Rect()

  private var ratingText: String = "0.0"

  var rating: Float = 0.0f
    set(value) {
      if (field != value) {
        field = value
        val ratingText = value.ratingText()
        if (this.ratingText != ratingText) {
          this.ratingText = ratingText
          requestLayout()
        }
      }
    }

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  private fun Float.ratingText(): String {
    val n = Math.round(this * 10)
    val high = n / 10
    val low = n % 10
    return "$high.$low"
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    paint.getTextBounds(ratingText, 0, ratingText.length, bounds)
    setMeasuredDimension(bounds.width() + starInterval + starSize, Math.max(bounds.height(), starSize))
  }

  override fun onDraw(canvas: Canvas) {
    val height = measuredHeight
    val x = -bounds.left
    val y = -bounds.top + (height - bounds.height()) / 2
    canvas.drawText(ratingText, x.toFloat(), y.toFloat(), paint)

    val saved = canvas.save()
    canvas.translate((bounds.width() + starInterval).toFloat(), ((height - starSize) / 2).toFloat())
    star.draw(canvas)
    canvas.restoreToCount(saved)
  }
}
