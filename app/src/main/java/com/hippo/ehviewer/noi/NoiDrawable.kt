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

package com.hippo.ehviewer.noi

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.Gravity
import com.hippo.ehviewer.util.Animate
import com.hippo.ehviewer.util.DURATION_IN
import com.hippo.ehviewer.util.DURATION_OUT
import com.hippo.ehviewer.util.clamp

/*
 * Created by Hippo on 2017/8/4.
 */

class NoiDrawable : Drawable(), Drawable.Callback {

  private val placeholder = ChildInfo()
  private val actual = ChildInfo()
  private val failure = ChildInfo()

  var placeholderDrawable: Drawable?
    get() = placeholder.drawable
    set(value) = setDrawable(placeholder, value?.mutate())

  var actualDrawable: Drawable?
    get() = actual.drawable
    set(value) = setDrawable(actual, value?.mutate())

  var failureDrawable: Drawable?
    get() = failure.drawable
    set(value) = setDrawable(failure, value?.mutate())

  var placeholderScaleType: ScaleType
    get() = placeholder.scaleType
    set(value) = setScaleType(placeholder, value)

  var actualScaleType: ScaleType
    get() = actual.scaleType
    set(value) = setScaleType(actual, value)

  var failureScaleType: ScaleType
    get() = failure.scaleType
    set(value) = setScaleType(failure, value)

  var placeholderGravity: Int
    get() = placeholder.gravity
    set(value) = setGravity(placeholder, value)

  var actualGravity: Int
    get() = actual.gravity
    set(value) = setGravity(actual, value)

  var failureGravity: Int
    get() = failure.gravity
    set(value) = setGravity(failure, value)

  val isPlaceholderVisible: Boolean get() = placeholder.visible

  val isActualVisible: Boolean get() = actual.visible

  val isFailureVisible: Boolean get() = failure.visible

  private fun setDrawable(info: ChildInfo, drawable: Drawable?) {
    info.drawable?.let { it.callback = null }
    info.drawable = drawable

    if (drawable != null) {
      drawable.callback = this
      val bounds = this.bounds
      if (!bounds.isEmpty) {
        info.refreshMatrix(bounds)
      }
      if (info.visible) {
        invalidateSelf()
      }
    }
  }

  private fun setScaleType(info: ChildInfo, scaleType: ScaleType) {
    if (info.scaleType != scaleType) {
      info.scaleType = scaleType

      val bounds = this.bounds
      if (!bounds.isEmpty) {
        info.refreshMatrix(bounds)
      }
      if (info.visible) {
        invalidateSelf()
      }
    }
  }

  private fun setGravity(info: ChildInfo, gravity: Int) {
    if (info.gravity != gravity) {
      info.gravity = gravity

      val bounds = this.bounds
      if (!bounds.isEmpty) {
        info.refreshMatrix(bounds)
      }
      if (info.visible) {
        invalidateSelf()
      }
    }
  }

  fun showPlaceholder(animation: Boolean) {
    placeholder.show(animation)
    actual.hide(animation)
    failure.hide(animation)
  }

  fun showActual(animation: Boolean) {
    placeholder.hide(animation)
    actual.show(animation)
    failure.hide(animation)
  }

  fun showFailure(animation: Boolean) {
    placeholder.hide(animation)
    actual.hide(animation)
    failure.show(animation)
  }

  override fun onBoundsChange(bounds: Rect) {
    if (bounds.isEmpty) return
    placeholder.refreshMatrix(bounds)
    actual.refreshMatrix(bounds)
    failure.refreshMatrix(bounds)
  }

  override fun draw(canvas: Canvas) {
    if (bounds.isEmpty) return
    val now = SystemClock.elapsedRealtime()
    placeholder.draw(canvas, now)
    actual.draw(canvas, now)
    failure.draw(canvas, now)
  }

  override fun setAlpha(alpha: Int) {}

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  override fun setColorFilter(colorFilter: ColorFilter?) {}

  private fun verifyDrawable(drawable: Drawable?): Boolean = drawable != null &&
      ((placeholder.visible && drawable == placeholder.drawable) ||
          (actual.visible && drawable == actual.drawable) ||
          (failure.visible && drawable == failure.drawable))

  override fun invalidateDrawable(who: Drawable?) {
    if (verifyDrawable(who)) invalidateSelf()
  }

  override fun scheduleDrawable(who: Drawable?, what: Runnable?, `when`: Long) {
    if (verifyDrawable(who)) scheduleSelf(what, `when`)
  }

  override fun unscheduleDrawable(who: Drawable?, what: Runnable?) {
    if (verifyDrawable(who)) unscheduleSelf(what)
  }

  private class ChildInfo {
    var drawable: Drawable? = null
    var visible: Boolean = false
    var scaleType: ScaleType = ScaleType.NONE
    var gravity: Int = Gravity.CENTER
    var matrix: Matrix = Matrix()
    var showing: Boolean = false
    val animate: Animate = object : Animate() {
      override fun onCalculate(progress: Float) {
        drawable?.alpha = ((if (showing) progress else (1.0f - progress)) * 255).toInt().clamp(0, 255)
      }
      override fun onEnd() {
        drawable?.alpha = 255
        visible = showing
      }
    }

    fun show(animation: Boolean) {
      if (!showing) {
        animate.cancel()
        showing = true
        visible = true
        if (animation) {
          animate.duration = DURATION_IN
          animate.start()
        }
        drawable?.invalidateSelf()
      } else if (!animation) {
        animate.cancel()
        drawable?.invalidateSelf()
      }
    }

    fun hide(animation: Boolean) {
      if (showing) {
        animate.cancel()
        showing = false
        if (animation) {
          visible = true
          animate.duration = DURATION_OUT
          animate.start()
        } else {
          visible = false
        }
        drawable?.invalidateSelf()
      } else if (!animation) {
        animate.cancel()
        drawable?.invalidateSelf()
      }
    }

    fun refreshMatrix(bounds: Rect) {
      val drawable = this.drawable
      if (drawable != null) {
        scaleType.refreshMatrix(drawable, bounds, matrix, gravity)
      }
    }

    fun draw(canvas: Canvas, now: Long) {
      val drawable = this.drawable
      if (!visible || drawable == null) return

      if (animate.calculate(now)) {
        drawable.invalidateSelf()
      }

      if (!visible) return

      val translate = scaleType != ScaleType.NONE
      var saved: Int = 0
      if (translate) {
        saved = canvas.save()
        canvas.concat(matrix)
      }

      drawable.draw(canvas)

      if (translate) {
        canvas.restoreToCount(saved)
      }
    }
  }
}
