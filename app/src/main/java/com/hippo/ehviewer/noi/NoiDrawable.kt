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

/*
 * Created by Hippo on 2017/8/4.
 */

class NoiDrawable : Drawable(), Drawable.Callback {

  private val placeholder = ChildInfo()
  private val actual = ChildInfo()
  private val failure = ChildInfo()

  var placeholderDrawable: Drawable?
    get() = placeholder.drawable
    set(value) = setDrawable(placeholder, value)

  var actualDrawable: Drawable?
    get() = actual.drawable
    set(value) = setDrawable(actual, value)

  var failureDrawable: Drawable?
    get() = failure.drawable
    set(value) = setDrawable(failure, value)

  var placeholderScaleType: ScaleType
    get() = placeholder.scaleType
    set(value) = setScaleType(placeholder, value)

  var actualScaleType: ScaleType
    get() = actual.scaleType
    set(value) = setScaleType(actual, value)

  var failureScaleType: ScaleType
    get() = failure.scaleType
    set(value) = setScaleType(failure, value)

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

  fun showPlaceholder() {
    placeholder.visible = true
    actual.visible = false
    failure.visible = false
    invalidateSelf()
  }

  fun showActual() {
    placeholder.visible = false
    actual.visible = true
    failure.visible = false
    invalidateSelf()
  }

  fun showFailure() {
    placeholder.visible = false
    actual.visible = false
    failure.visible = true
    invalidateSelf()
  }

  override fun onBoundsChange(bounds: Rect) {
    if (bounds.isEmpty) return
    placeholder.refreshMatrix(bounds)
    actual.refreshMatrix(bounds)
    failure.refreshMatrix(bounds)
  }

  override fun draw(canvas: Canvas) {
    if (bounds.isEmpty) return
    placeholder.draw(canvas)
    actual.draw(canvas)
    failure.draw(canvas)
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
    var matrix: Matrix = Matrix()

    fun refreshMatrix(bounds: Rect) {
      val drawable = this.drawable
      if (drawable != null) {
        scaleType.refreshMatrix(drawable, bounds, matrix)
      }
    }

    fun draw(canvas: Canvas) {
      val drawable = this.drawable
      if (!visible || drawable == null) return

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
