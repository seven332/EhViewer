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
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextUtils

/*
 * Created by Hippo on 6/10/2017.
 */

class TextDrawable(
    private val text: String,
    private val contentPercent: Float
) : Drawable() {

  companion object {
    private const val STANDARD_TEXT_SIZE = 1000.0f
    private val STANDARD_PAINT: Paint = Paint().apply { textSize = STANDARD_TEXT_SIZE }
  }

  private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

  private var x: Float = 0.0f
  private var y: Float = 0.0f
  private var textSizeDirty = true
  private val textBounds = Rect().apply { STANDARD_PAINT.getTextBounds(text, 0, text.length, this) }

  var textColor: Int = 0
    set(value) {
      if (field != value) {
        field = value
        invalidateSelf()
      }
    }

  var backgroundColor: Int = 0
    set(value) {
      if (field != value) {
        field = value
        invalidateSelf()
      }
    }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    backgroundPaint.colorFilter = colorFilter
    textPaint.colorFilter = colorFilter
    invalidateSelf()
  }

  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  override fun setAlpha(alpha: Int) {}

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    textSizeDirty = true
  }

  private fun updateTextSizeIfDirty() {
    if (!textSizeDirty) return
    textSizeDirty = false

    val contentWidth = (bounds.width() * contentPercent).toInt()
    val contentHeight = (bounds.height() * contentPercent).toInt()
    val widthRatio = contentWidth.toFloat() / textBounds.width()
    val heightRatio = contentHeight.toFloat() / textBounds.height()
    val ratio = Math.min(widthRatio, heightRatio)
    val textSize = STANDARD_TEXT_SIZE * ratio
    textPaint.textSize = textSize
    x = (bounds.width() - textBounds.width() * ratio) / 2 - textBounds.left * ratio
    y = (bounds.height() - textBounds.height() * ratio) / 2 - textBounds.top * ratio
  }

  override fun draw(canvas: Canvas) {
    if (!bounds.isEmpty) {
      // Draw background
      backgroundPaint.color = backgroundColor
      canvas.drawRect(bounds, backgroundPaint)

      if (!TextUtils.isEmpty(text)) {
        // Draw text
        updateTextSizeIfDirty()
        textPaint.color = textColor
        canvas.drawText(text, x, y, textPaint)
      }
    }
  }
}
