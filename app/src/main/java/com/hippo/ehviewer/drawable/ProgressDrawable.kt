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
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.support.v4.view.animation.PathInterpolatorCompat
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import com.hippo.ehviewer.util.Animate
import com.hippo.ehviewer.util.MAX_LEVEL
import com.hippo.ehviewer.util.centerTo
import com.hippo.ehviewer.util.forEachAny
import com.hippo.ehviewer.util.lerp

/*
 * Created by Hippo on 6/6/2017.
 */

class ProgressDrawable : Drawable() {

  companion object {
    private val TRIM_START_INTERPOLATOR: Interpolator
    private val TRIM_END_INTERPOLATOR: Interpolator
    private val LINEAR_INTERPOLATOR = LinearInterpolator()

    init {
      val trimStartPath = Path()
      trimStartPath.moveTo(0.0f, 0.0f)
      trimStartPath.lineTo(0.5f, 0.0f)
      trimStartPath.cubicTo(0.7f, 0.0f, 0.6f, 1f, 1f, 1f)
      TRIM_START_INTERPOLATOR = PathInterpolatorCompat.create(trimStartPath)

      val trimEndPath = Path()
      trimEndPath.moveTo(0.0f, 0.0f)
      trimEndPath.cubicTo(0.2f, 0.0f, 0.1f, 1f, 0.5f, 1f)
      trimEndPath.lineTo(1f, 1f)
      TRIM_END_INTERPOLATOR = PathInterpolatorCompat.create(trimEndPath)
    }
  }

  private val animates: List<Animate>
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val rect = RectF()

  private var trimStart = 0.0f
  private var trimEnd = 0.0f
  private var trimOffset = 0.0f
  private var trimRotation = 0.0f

  var indeterminate: Boolean = false
    set(value) {
      if (field != value) {
        field = value
        if (value) {
          animates.forEach { it.start() }
        } else {
          animates.forEach { it.cancel() }
        }
        invalidateSelf()
      }
    }

  var color: Int
    get() = paint.color
    set(value) {
      paint.color = value
      invalidateSelf()
    }

  var size: Int = -1
    set(value) {
      if (field != value) {
        field = value
        invalidateSelf()
      }
    }

  init {
    paint.strokeCap = Paint.Cap.SQUARE
    paint.strokeJoin = Paint.Join.MITER
    paint.style = Paint.Style.STROKE
    val trimStart = ProgressAnimate(0.0f, 0.75f, 1333L, TRIM_START_INTERPOLATOR, { trimStart = it })
    val trimEnd = ProgressAnimate(0.0f, 0.75f, 1333L, TRIM_END_INTERPOLATOR, { trimEnd = it })
    val trimOffset = ProgressAnimate(0.0f, 0.25f, 1333L, LINEAR_INTERPOLATOR, { trimOffset = it })
    val trimRotation = ProgressAnimate(0.0f, 720.0f, 6665L, LINEAR_INTERPOLATOR, { trimRotation = it })
    animates = listOf(trimStart, trimEnd, trimOffset, trimRotation)
  }

  override fun onLevelChange(level: Int): Boolean {
    indeterminate = false
    trimStart = 0f
    trimEnd = (level / MAX_LEVEL).toFloat()
    trimOffset = 0f
    trimRotation = 0f
    invalidateSelf()
    return true
  }

  override fun onBoundsChange(bounds: Rect) {
    if (bounds.isEmpty) { rect.setEmpty(); return }

    val size = this.size.let { if (it < 0) Math.min(bounds.width(), bounds.height()).toFloat() else it.toFloat() }
    val stroke = size / 12.0f
    paint.strokeWidth = stroke
    rect.set(0.0f, 0.0f, size, size)
    rect.centerTo(bounds.centerX().toFloat(), bounds.centerY().toFloat())
    val halfStroke = stroke * 0.5f
    rect.inset(halfStroke, halfStroke)
  }

  override fun draw(canvas: Canvas) {
    if (rect.isEmpty) return

    val now = System.currentTimeMillis()
    val invalidate = animates.forEachAny { it.calculate(now) }

    val saved = canvas.save()
    canvas.rotate(trimRotation, rect.centerX(), rect.centerY())

    val startAngle = (trimStart + trimOffset) * 360.0f - 90.0f
    val sweepAngle = (trimEnd - trimStart) * 360.0f

    canvas.drawArc(rect, startAngle, sweepAngle, false, paint)

    canvas.restoreToCount(saved)

    if (invalidate) invalidateSelf()
  }

  override fun getIntrinsicWidth() = size

  override fun getIntrinsicHeight() = size

  override fun setAlpha(alpha: Int) { paint.alpha = alpha }

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }

  private class ProgressAnimate(
      val from: Float,
      val to: Float,
      duration: Long,
      interpolator: Interpolator,
      val block: (Float) -> Unit
  ) : Animate() {

    init {
      this.duration = duration
      this.interpolator = interpolator
      this.repeat = INFINITE
    }

    override fun onCalculate(progress: Float) { block(progress.lerp(from, to)) }
  }
}
