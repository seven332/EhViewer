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

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.hippo.ehviewer.NOI
import java.io.IOException
import java.io.InputStream

/*
 * Created by Hippo on 2017/8/7.
 */

class BitmapDrawableResolver(private val resources: Resources) : NoiTask.DrawableResolver {

  override fun getDrawable(uri: String): Drawable? =
      NOI.bitmapCache[uri]?.let { BitmapDrawable(resources, it) }

  override fun decodeDrawable(uri: String, stream: InputStream, checker: NoiTask.Checker): Drawable {
    val bitmap = BitmapFactory.decodeStream(stream)
    if (bitmap == null) {
      throw IOException("Fail to decode stream")
    } else if (!checker.valid) {
      bitmap.recycle()
      throw IOException("Response body isn't fully read.")
    } else {
      NOI.bitmapCache[uri] = bitmap
      return BitmapDrawable(resources, bitmap)
    }
  }

  override fun recycleDrawable(drawable: Drawable) {}
}
