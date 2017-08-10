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

package com.hippo.ehviewer.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/*
 * Created by Hippo on 2017/8/9.
 */

class RoundRectDrawable(
    val radius: Float = 0.0f,
    val color: Int = Color.BLACK,
    val edgeWidth: Float = 0.0f,
    val edgeColor: Int = Color.BLACK
) : Drawable() {

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).also {
    it.color = color
    it.style = Paint.Style.FILL
  }

  private val edgePaint = if (edgeWidth <= 0.0f) null else Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).also {
    it.color = edgeColor
    it.style = Paint.Style.FILL
  }

  override fun draw(canvas: Canvas) {
    if (edgePaint != null) {
      canvas.drawRoundRect(RectF(bounds), radius, radius, edgePaint)
      canvas.drawRoundRect(RectF(bounds).apply { inset(edgeWidth, edgeWidth) }, radius, radius, paint)
    } else {
      canvas.drawRoundRect(RectF(bounds), radius, radius, paint)
    }
  }

  override fun setAlpha(alpha: Int) {}

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  override fun setColorFilter(colorFilter: ColorFilter?) {}
}
