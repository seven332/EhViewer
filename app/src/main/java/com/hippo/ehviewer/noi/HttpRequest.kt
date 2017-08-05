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

import com.hippo.ehviewer.exception.ResponseCodeException
import com.hippo.ehviewer.util.buffer
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Response
import okio.Okio
import okio.Source
import java.io.InputStream

/*
 * Created by Hippo on 2017/8/1.
 */

class HttpRequest(
    private val call: Call,
    private val dispatcher: HttpDispatcher
) : Request<HttpRequest>(), Runnable {

  var concurrentDuplicateUrl: Boolean = true
    private set

  private lateinit var observer: SingleObserver<in Response>

  // It runs in io thread
  override fun run() {
    var response: Response? = null
    try {
      response = call.execute()
      if (!call.isCanceled) {
        observer.onSuccess(response)
      }
    } catch (e: Throwable) {
      if (!call.isCanceled) {
        observer.onError(e)
      }
    } finally {
      response?.close()
      dispatcher.finished(this)
    }
  }

  /** Whether allow to concurrent duplicate url. `true` in default. **/
  fun concurrentDuplicateUrl(concurrentDuplicateUrl: Boolean): HttpRequest {
    checkSolid()
    this.concurrentDuplicateUrl = concurrentDuplicateUrl
    return this
  }

  internal fun url(): HttpUrl = call.request().url()

  internal fun cancel() = call.cancel()

  fun asResponse(): Single<Response> {
    solidify()
    return object : Single<Response>() {
      override fun subscribeActual(observer: SingleObserver<in Response>) {
        this@HttpRequest.observer = observer
        observer.onSubscribe(object : Disposable {
          override fun dispose() { dispatcher.cancel(this@HttpRequest) }
          override fun isDisposed(): Boolean = call.isCanceled
        })
        dispatcher.enqueue(this@HttpRequest)
      }
    }
  }

  override fun asSource(): Single<Source> = asResponse().map {
    if (it.isSuccessful) {
      it.body()!!.source()
    } else {
      throw ResponseCodeException(it.code())
    }
  }

  override fun asInputStream(): Single<InputStream> = asResponse().map {
    if (it.isSuccessful) {
      it.body()!!.byteStream()
    } else {
      throw ResponseCodeException(it.code())
    }
  }

  /** Fetch the response to OkHttp cache **/
  fun fetch() = asSource().subscribe({ source -> Okio.blackhole().buffer().use { it.writeAll(source) } }, { /* Ignore error */ })
}


