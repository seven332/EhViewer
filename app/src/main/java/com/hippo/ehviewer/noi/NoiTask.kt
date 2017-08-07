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
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.hippo.ehviewer.NOI
import com.hippo.ehviewer.util.MAX_LEVEL
import com.hippo.ehviewer.util.MutableAny
import com.hippo.ehviewer.util.MutableBoolean
import com.hippo.ehviewer.util.check
import com.hippo.ehviewer.util.progress
import com.hippo.unifile.UniFile
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.io.InputStream

/*
 * Created by Hippo on 2017/8/7.
 */

class NoiTask(
    private val context: Context,
    private val resolver: DrawableResolver
) {

  companion object {
    private val HANDLE: Handler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
        val obj = msg.obj
        if (obj is NoiDrawable && obj.isPlaceholderVisible) {
          obj.placeholderDrawable?.level = msg.what
        }
      }
    }
  }

  val noiDrawable = NoiDrawable()

  private var disposable: Disposable? = null

  var enableProgress: Boolean = false

  var enableFade: Boolean = true

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

  fun load(drawable: Drawable) {
    cancel()

    noiDrawable.actualDrawable = drawable
    noiDrawable.showActual(false)
  }

  fun load(uri: String) {
    cancel()

    val drawable = resolver.getDrawable(uri)
    if (drawable != null) {
      noiDrawable.actualDrawable = drawable
      noiDrawable.showActual(false)
    } else {
      startPlaceholder()
      noiDrawable.showPlaceholder(false)
      download(uri)
    }
  }

  private fun http(uri: String, checker: RealChecker): Single<InputStream> {
    var obj = NOI.http(uri)
        .concurrentDuplicateUrl(false)
        .asResponse()
        .check(checker.httpChecker)

    if (enableProgress) {
      obj = obj.progress { _, read, total ->
        val level = (read * MAX_LEVEL / total).toInt()
        HANDLE.sendMessage(HANDLE.obtainMessage(level, noiDrawable))
      }
    }

    return obj.map { it.body()!!.byteStream() }
  }

  private fun file(uri: String, checker: RealChecker): Single<InputStream> {
    // It's not http, always set http checker to true
    checker.httpChecker.value = true

    val obj = object : Single<InputStream>() {
      override fun subscribeActual(observer: SingleObserver<in InputStream>) {
        observer.onSubscribe(Disposables.empty())
        observer.onSuccess(UniFile.fromUri(context, Uri.parse(uri)).openInputStream())
      }
    }
    return obj.subscribeOn(Schedulers.io())
  }

  private fun isHttp(uri: String): Boolean = uri.startsWith("http://") || uri.startsWith("https://")

  private fun download(uri: String) {
    val checker = RealChecker()
    val drawableHolder = MutableAny<Drawable>(null)

    (if (isHttp(uri)) http(uri, checker) else file(uri, checker))
        .map { stream ->
          val drawable = stream.use { resolver.decodeDrawable(uri, it, checker) }
          var valid: Boolean = false
          synchronized(checker) {
            valid = checker.valid
            if (valid) {
              drawableHolder.value = drawable
            }
          }
          if (!valid) {
            resolver.recycleDrawable(drawable)
            throw IOException("Disposed")
          } else {
            drawable
          }
        }
        .doOnDispose {
          var drawable: Drawable? = null
          synchronized(checker) {
            checker.failed()
            drawable = drawableHolder.value
            drawableHolder.value = null
          }
          drawable?.let { resolver.recycleDrawable(it) }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .register({ drawable ->
          stopPlaceholder()
          noiDrawable.actualDrawable = drawable
          noiDrawable.showActual(enableFade)
        }, {
          stopPlaceholder()
          noiDrawable.showFailure(enableFade)
        })
  }

  fun cancel() {
    disposable?.run { dispose() }
    disposable = null
    stopPlaceholder()

    val oldDrawable = noiDrawable.actualDrawable
    if (oldDrawable != null) {
      resolver.recycleDrawable(oldDrawable)
      noiDrawable.actualDrawable = null
    }
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

  interface Checker {
    val valid: Boolean
  }

  private class RealChecker : Checker {
    val httpChecker = MutableBoolean(false)

    private var failed = false

    fun failed() {
      failed = true
    }

    override val valid: Boolean get() = !failed && httpChecker.value
  }

  interface DrawableResolver {

    /**
     * Gets drawable for the uri. Returns `null` if can't get the drawable in a short time.
     *
     * It's called in UI thread.
     */
    fun getDrawable(uri: String): Drawable?

    /**
     * Decodes the stream for the uri. Throws a [Throwable] if can't decode the stream.
     *
     * Checks the checker after decoding. The decoded resources should be cached if it's invalid.
     *
     * It's called in IO thread.
     */
    @Throws(Throwable::class)
    fun decodeDrawable(uri: String, stream: InputStream, checker: Checker): Drawable

    /**
     * Recycles the resources associated with the drawable.
     *
     * It's called in UI or IO thread.
     */
    fun recycleDrawable(drawable: Drawable)
  }
}
