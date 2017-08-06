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

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.Gravity

/*
 * Created by Hippo on 2017/8/4.
 */

enum class ScaleType {

  NONE {
    override fun doRefreshMatrix(drawable: Drawable, bounds: Rect, matrix: Matrix, gravity: Int) {
      drawable.bounds.set(bounds)
    }
  },

  CENTER_INSIDE {
    override fun doRefreshMatrix(drawable: Drawable, bounds: Rect, matrix: Matrix, gravity: Int) {
      drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

      val drawableRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
      val boundsRatio = bounds.width().toFloat() / bounds.height().toFloat()

      val scale: Float
      val dx: Float
      val dy: Float
      if (drawableRatio > boundsRatio) {
        scale = bounds.width().toFloat() / drawable.intrinsicWidth.toFloat()
        dx = 0.0f
        dy = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
          Gravity.TOP -> 0.0f
          Gravity.BOTTOM -> bounds.height() - (bounds.width() / drawableRatio)
          else -> (bounds.height() - (bounds.width() / drawableRatio)) / 2
        }
      } else {
        scale = bounds.height().toFloat() / drawable.intrinsicHeight.toFloat()
        dx = when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
          Gravity.LEFT -> 0.0f
          Gravity.RIGHT -> bounds.width() - (bounds.height() * drawableRatio)
          else -> (bounds.width() - (bounds.height() * drawableRatio)) / 2
        }
        dy = 0.0f
      }

      matrix.reset()
      matrix.postScale(scale, scale)
      matrix.postTranslate(dx, dy)
    }
  },

  CENTER_CROP {
    override fun doRefreshMatrix(drawable: Drawable, bounds: Rect, matrix: Matrix, gravity: Int) {
      drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

      val drawableRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
      val boundsRatio = bounds.width().toFloat() / bounds.height().toFloat()

      val scale: Float
      val dx: Float
      val dy: Float
      if (drawableRatio > boundsRatio) {
        scale = bounds.height().toFloat() / drawable.intrinsicHeight.toFloat()
        dx = when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
          Gravity.LEFT -> 0.0f
          Gravity.RIGHT -> bounds.width() - (bounds.height() * drawableRatio)
          else -> (bounds.width() - (bounds.height() * drawableRatio)) / 2
        }
        dy = 0.0f
      } else {
        scale = bounds.width().toFloat() / drawable.intrinsicWidth.toFloat()
        dx = 0.0f
        dy = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
          Gravity.TOP -> 0.0f
          Gravity.BOTTOM -> bounds.height() - (bounds.width() / drawableRatio)
          else -> (bounds.height() - (bounds.width() / drawableRatio)) / 2
        }
      }

      matrix.reset()
      matrix.postScale(scale, scale)
      matrix.postTranslate(dx, dy)
    }
  };

  fun refreshMatrix(drawable: Drawable, bounds: Rect, matrix: Matrix, gravity: Int) {
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight

    if (width == -1 && height == -1) {
      drawable.bounds = bounds
      matrix.reset()

    } else if (width == -1) {
      val ratio = bounds.width().toFloat() / bounds.height().toFloat()
      drawable.bounds.set(0, 0, (height * ratio).toInt(), height)

      val scale = bounds.height().toFloat() / height.toFloat()
      matrix.reset()
      matrix.postScale(scale, scale)

    } else if (height == -1) {
      val ratio = bounds.width().toFloat() / bounds.height().toFloat()
      drawable.bounds.set(0, 0, width, (width / ratio).toInt())

      val scale = bounds.width().toFloat() / width.toFloat()
      matrix.reset()
      matrix.postScale(scale, scale)

    } else {
      doRefreshMatrix(drawable, bounds, matrix, gravity)
    }
  }

  abstract fun doRefreshMatrix(drawable: Drawable, bounds: Rect, matrix: Matrix, gravity: Int)
}
