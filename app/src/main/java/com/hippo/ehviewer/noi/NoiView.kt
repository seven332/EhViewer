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

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.getDrawable

/*
 * Created by Hippo on 2017/8/7.
 */

open class NoiView : View {

  constructor(context: Context): super(context)

  constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NoiView)
    placeholderDrawable = typedArray.getDrawable(context, R.styleable.NoiView_placeholderDrawable)
    failureDrawable = typedArray.getDrawable(context, R.styleable.NoiView_failureDrawable)
    placeholderScaleType = typedArray.getInt(R.styleable.NoiView_placeholderScaleType, 0).scaleType
    actualScaleType = typedArray.getInt(R.styleable.NoiView_actualScaleType, 0).scaleType
    failureScaleType = typedArray.getInt(R.styleable.NoiView_failureScaleType, 0).scaleType
    placeholderGravity = typedArray.getInt(R.styleable.NoiView_placeholderGravity, Gravity.CENTER)
    actualGravity = typedArray.getInt(R.styleable.NoiView_actualGravity, Gravity.CENTER)
    failureGravity = typedArray.getInt(R.styleable.NoiView_failureGravity, Gravity.CENTER)
    aspectRatio = typedArray.getFloat(R.styleable.NoiView_aspectRatio, 0.0f)
    enableProgress = typedArray.getBoolean(R.styleable.NoiView_enableProgress, false)
    enableFade = typedArray.getBoolean(R.styleable.NoiView_enableFade, true)
    drawable = typedArray.getDrawable(context, R.styleable.NoiView_drawable)
    uri = typedArray.getString(R.styleable.NoiView_uri)
    typedArray.recycle()
  }

  private val task = NoiTask(context, BitmapDrawableResolver(context.resources))
      .also { it.noiDrawable.callback = this }

  var placeholderDrawable: Drawable?
    get() = task.noiDrawable.placeholderDrawable
    set(value) { task.noiDrawable.placeholderDrawable = value }

  val actualDrawable: Drawable? get() = task.noiDrawable.actualDrawable

  var failureDrawable: Drawable?
    get() = task.noiDrawable.failureDrawable
    set(value) { task.noiDrawable.failureDrawable = value }

  var placeholderScaleType: ScaleType
    get() = task.noiDrawable.placeholderScaleType
    set(value) { task.noiDrawable.placeholderScaleType = value }

  var actualScaleType: ScaleType
    get() = task.noiDrawable.actualScaleType
    set(value) { task.noiDrawable.actualScaleType = value }

  var failureScaleType: ScaleType
    get() = task.noiDrawable.failureScaleType
    set(value) { task.noiDrawable.failureScaleType = value }

  var placeholderGravity: Int
    get() = task.noiDrawable.placeholderGravity
    set(value) { task.noiDrawable.placeholderGravity = value }

  var actualGravity: Int
    get() = task.noiDrawable.actualGravity
    set(value) { task.noiDrawable.actualGravity = value }

  var failureGravity: Int
    get() = task.noiDrawable.failureGravity
    set(value) { task.noiDrawable.failureGravity = value }

  var aspectRatio: Float = 0.0f
    set(value) {
      if (field != value) {
        field = value
        requestLayout()
      }
    }

  var enableProgress: Boolean
    get() = task.enableProgress
    set(value) { task.enableProgress = value }

  var enableFade: Boolean
    get() = task.enableFade
    set(value) { task.enableFade = value }

  var uri: String? = null
    set(value) {
      if (field != value) {
        field = value

        if (value != null) {
          drawable = null
        }

        if (ViewCompat.isAttachedToWindow(this)) {
          if (value != null) {
            task.load(value)
          } else {
            task.cancel()
          }
        }
      }
    }

  var drawable: Drawable? = null
    set(value) {
      if (field != value) {
        field = value

        if (value != null) {
          uri = null
        }

        if (ViewCompat.isAttachedToWindow(this)) {
          if (value != null) {
            task.load(value)
          } else {
            task.cancel()
          }
        }
      }
    }

  private val Int.scaleType get() = when (this) {
    1 -> ScaleType.CENTER_INSIDE
    2 -> ScaleType.CENTER_CROP
    else -> ScaleType.NONE
  }

  override fun verifyDrawable(who: Drawable?): Boolean {
    return who == task.noiDrawable || super.verifyDrawable(who)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    val url = this.uri
    val drawable = this.drawable
    if (url != null) {
      task.load(url)
    } else if (drawable != null) {
      task.load(drawable)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    if (uri != null || drawable != null) {
      task.cancel()
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    task.noiDrawable.setBounds(0, 0, w, h)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (aspectRatio > 0.0f) {
      val widthSize = MeasureSpec.getSize(widthMeasureSpec)
      val widthMode = MeasureSpec.getMode(widthMeasureSpec)
      val heightSize = MeasureSpec.getSize(heightMeasureSpec)
      val heightMode = MeasureSpec.getMode(heightMeasureSpec)

      if (widthMode == MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
        setMeasuredDimension(
            widthSize,
            (widthSize / aspectRatio).toInt().let {
              if (heightMode == MeasureSpec.AT_MOST) maxOf(it, heightSize)
              else it
            })
        return
      } else if (heightMode == MeasureSpec.EXACTLY || widthMode != MeasureSpec.EXACTLY) {
        setMeasuredDimension(
            (heightSize * aspectRatio).toInt().let {
              if (widthMode == MeasureSpec.AT_MOST) maxOf(it, widthSize)
              else it
            },
            heightSize)
        return
      }
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  override fun onDraw(canvas: Canvas) {
    task.noiDrawable.draw(canvas)
  }
}
