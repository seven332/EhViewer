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
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.MAX_LEVEL
import com.hippo.ehviewer.util.MutableBoolean
import com.hippo.ehviewer.util.check
import com.hippo.ehviewer.util.getDrawable
import com.hippo.ehviewer.util.progress
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.IOException

/*
 * Created by Hippo on 2017/7/28.
 */

open class NoiView : View {

  companion object {
    private val NOI: Noi = com.hippo.ehviewer.NOI

    private val HANDLE: Handler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
        val obj = msg.obj
        if (obj is NoiDrawable && obj.isPlaceholderVisible) {
          obj.placeholderDrawable?.level = msg.what
        }
      }
    }
  }

  private val noiDrawable: NoiDrawable = NoiDrawable().also { it.callback = this }

  private var disposable: Disposable? = null

  var placeholderDrawable: Drawable?
    get() = noiDrawable.placeholderDrawable
    set(value) { noiDrawable.placeholderDrawable = value }

  val actualDrawable: Drawable? get() = noiDrawable.actualDrawable

  var failureDrawable: Drawable?
    get() = noiDrawable.failureDrawable
    set(value) { noiDrawable.failureDrawable = value }

  var placeholderScaleType: ScaleType
    get() = noiDrawable.placeholderScaleType
    set(value) { noiDrawable.placeholderScaleType = value }

  var actualScaleType: ScaleType
    get() = noiDrawable.actualScaleType
    set(value) { noiDrawable.actualScaleType = value }

  var failureScaleType: ScaleType
    get() = noiDrawable.failureScaleType
    set(value) { noiDrawable.failureScaleType = value }

  var aspectRatio: Float = 0.0f
    set(value) {
      if (field != value) {
        field = value
        requestLayout()
      }
    }

  var enableProgress: Boolean = false

  var enableFade: Boolean = true

  var uri: String? = null
    set(value) {
      if (field != value) {
        field = value

        if (value != null) {
          drawable = null
        }

        if (ViewCompat.isAttachedToWindow(this)) {
          if (value != null) {
            load(value)
          } else {
            cancel()
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
            load(value)
          } else {
            cancel()
          }
        }
      }
    }

  constructor(context: Context): super(context)

  constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NoiView)
    placeholderDrawable = typedArray.getDrawable(context, R.styleable.NoiView_placeholderDrawable)
    failureDrawable = typedArray.getDrawable(context, R.styleable.NoiView_failureDrawable)
    placeholderScaleType = typedArray.getInteger(R.styleable.NoiView_placeholderScaleType, 0).scaleType
    actualScaleType = typedArray.getInteger(R.styleable.NoiView_actualScaleType, 0).scaleType
    failureScaleType = typedArray.getInteger(R.styleable.NoiView_failureScaleType, 0).scaleType
    aspectRatio = typedArray.getFloat(R.styleable.NoiView_aspectRatio, 0.0f)
    enableProgress = typedArray.getBoolean(R.styleable.NoiView_enableProgress, false)
    enableFade = typedArray.getBoolean(R.styleable.NoiView_enableFade, false)
    drawable = typedArray.getDrawable(context, R.styleable.NoiView_drawable)
    uri = typedArray.getString(R.styleable.NoiView_uri)
    typedArray.recycle()
  }

  private val Int.scaleType get() = when (this) {
    1 -> ScaleType.FIT_CENTER
    2 -> ScaleType.CENTER_CROP
    else -> ScaleType.NONE
  }

  override fun verifyDrawable(who: Drawable?): Boolean {
    return who == noiDrawable || super.verifyDrawable(who)
  }

  private fun startPlaceholder() {
    val placeholder = noiDrawable.placeholderDrawable
    if (placeholder is Animatable) {
      placeholder.start()
    }
  }

  private fun stopPlaceholder() {
    val placeholder = noiDrawable.placeholderDrawable
    if (placeholder is Animatable) {
      placeholder.stop()
    }
  }

  private fun load(drawable: Drawable) {
    cancel()

    noiDrawable.actualDrawable = drawable
    noiDrawable.showActual(false)
  }

  private fun load(uri: String) {
    cancel()

    val bitmap = NOI.bitmapCache[uri]
    if (bitmap != null) {
      noiDrawable.actualDrawable = BitmapDrawable(resources, bitmap)
      noiDrawable.showActual(false)
    } else {
      startPlaceholder()
      noiDrawable.showPlaceholder(false)
      download(uri)
    }
  }

  private fun download(uri: String) {
    val check = MutableBoolean(false)
    var obj = NOI.http(uri)
        .concurrentDuplicateUrl(false)
        .asResponse()
        .check(check)

    if (enableProgress) {
      obj = obj.progress { _, read, total ->
        val level = (read * MAX_LEVEL / total).toInt()
        HANDLE.sendMessage(HANDLE.obtainMessage(level, noiDrawable))
      }
    }

    obj.map { response ->
          val bitmap = BitmapFactory.decodeStream(response.body()!!.byteStream())
          if (bitmap == null) {
            throw IOException("Fail to decode stream")
          } else if (!check.value) {
            bitmap.recycle()
            throw IOException("Response body isn't fully read.")
          } else {
            NOI.bitmapCache[uri] = bitmap
            bitmap
          }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .register({ bitmap ->
          stopPlaceholder()
          noiDrawable.actualDrawable = BitmapDrawable(resources, bitmap)
          noiDrawable.showActual(enableFade)
        }, {
          stopPlaceholder()
          noiDrawable.showFailure(enableFade)
        })
  }

  private fun cancel() {
    disposable?.run { dispose() }
    disposable = null
    stopPlaceholder()
    noiDrawable.actualDrawable = null
  }

  private fun <T> Single<T>.register(
      onSuccess: (T) -> Unit,
      onError: (Throwable) -> Unit
  ) {
    disposable = subscribe({
      onSuccess(it)
      disposable = null
    }, {
      onError(it)
      disposable = null
    })
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    val url = this.uri
    val drawable = this.drawable
    if (url != null) {
      load(url)
    } else if (drawable != null) {
      load(drawable)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    if (uri != null && drawable != null) {
      cancel()
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    noiDrawable.setBounds(0, 0, w, h)
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
    noiDrawable.draw(canvas)
  }
}
