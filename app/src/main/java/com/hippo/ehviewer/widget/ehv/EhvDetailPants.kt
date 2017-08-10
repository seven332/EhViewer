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

package com.hippo.ehviewer.widget.ehv

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v4.view.ViewCompat
import android.support.v7.graphics.drawable.DrawableWrapper
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout
import com.hippo.ehviewer.EH_URL
import com.hippo.ehviewer.NOI
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.COVER_SIZE_300
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.noi.NoiTask
import com.hippo.ehviewer.noi.ScaleType
import com.hippo.ehviewer.util.Blur
import com.hippo.ehviewer.util.attrColor
import java.io.IOException
import java.io.InputStream

/*
 * Created by Hippo on 2017/8/7.
 */

class EhvDetailPants : RelativeLayout {

  constructor(context: Context): super(context)
  constructor(context: Context, attrs: AttributeSet?): super(context, attrs)

  private val themeColor: Int = context.attrColor(R.attr.colorPrimary)

  private val originalPaddingTop: Int = paddingTop

  private val task = NoiTask(context, PantsDrawableResolver(context.resources, themeColor))

  private var isTouchEnabled = true

  var info: GalleryInfo? = null
    set(value) {
      if (field != value) {
        field = value
        val fingerprint = value?.coverFingerprint
        uri = if (fingerprint != null) EH_URL.coverUrl(fingerprint, COVER_SIZE_300) else null
      }
    }

  private var uri: String? = null
    set(value) {
      if (field != value) {
        field = value

        if (ViewCompat.isAttachedToWindow(this)) {
          if (value != null) {
            task.load(value)
          } else {
            task.cancel()
          }
        }
      }
    }

  init {
    task.noiDrawable.placeholderDrawable = ColorDrawable(themeColor)
    task.noiDrawable.failureDrawable = ColorDrawable(themeColor)
    task.noiDrawable.actualScaleType = ScaleType.CENTER_CROP
    @Suppress("DEPRECATION")
    setBackgroundDrawable(task.noiDrawable)
  }

  fun setTouchEnabled(isTouchEnabled: Boolean) {
    this.isTouchEnabled = isTouchEnabled
  }

  override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    // FIXME It breaks MotionEvent sequence, it's bad.
    return if (!isTouchEnabled) false else super.dispatchTouchEvent(ev)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    val url = this.uri
    if (url != null) {
      task.load(url)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    if (uri != null) {
      task.cancel()
    }
  }

  fun setAdditionalPaddingTop(additionalPaddingTop: Int) {
    setPadding(paddingTop, originalPaddingTop + additionalPaddingTop, paddingRight, paddingBottom)
  }

  class PantsDrawableResolver(
      private val resources: Resources,
      private val color: Int
  ) : NoiTask.DrawableResolver {
    private fun String.toPantsUri() = this + ":ehv-pants"

    override fun getDrawable(uri: String): Drawable? =
        NOI.bitmapCache[uri.toPantsUri()]?.let { PantsDrawable(BitmapDrawable(resources, it), color) }

    override fun decodeDrawable(uri: String, stream: InputStream, checker: NoiTask.Checker): Drawable {
      val options = BitmapFactory.Options()
      options.inMutable = true

      val bitmap = BitmapFactory.decodeStream(stream, null, options)
      if (bitmap == null) {
        throw IOException("Fail to decode stream")
      } else if (!checker.valid) {
        bitmap.recycle()
        throw IOException("Response body isn't fully read.")
      } else {
        NOI.bitmapCache[uri.toPantsUri()] = Blur.blur(bitmap, 12, true)
        return PantsDrawable(BitmapDrawable(resources, bitmap), color)
      }
    }

    override fun recycleDrawable(drawable: Drawable) {}
  }

  class PantsDrawable(drawable: Drawable, color: Int) : DrawableWrapper(drawable) {
    private val paint = Paint().also { it.color = color and 0xffffff or 0xb3000000.toInt() }

    override fun draw(canvas: Canvas) {
      super.draw(canvas)
      canvas.drawRect(bounds, paint)
    }
  }
}
