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

package com.hippo.ehviewer.util

import com.hippo.ehviewer.network.SimpleResponseBody
import io.reactivex.Single
import okhttp3.Response
import okio.Buffer
import okio.Okio
import okio.Sink
import okio.Source
import okio.Timeout
import java.io.IOException

/*
 * Created by Hippo on 2017/7/23.
 */

/** Injects checker to check whether response body is fully read. **/
fun Single<Response>.check(result: MutableBoolean): Single<Response> = map { response ->
  val body = response.body()!!
  val contentLength = body.contentLength()

  if (contentLength != -1L) {
    result.value = false
    val source = body.source()
    val checkSource = object : Source {
      private var read: Long = 0L

      override fun read(sink: Buffer, byteCount: Long): Long = source.read(sink, byteCount).also { single ->
        if (single != -1L) {
          read += single
        }
        result.value = read == contentLength
      }

      override fun close() = source.close().also { result.value = read == contentLength }

      override fun timeout(): Timeout = source.timeout()
    }
    response.newBuilder()
        .body(SimpleResponseBody(response.headers(), Okio.buffer(checkSource)))
        .build()
  } else {
    // No content length, can't check it, always set true
    result.value = true
    response
  }
}

interface RequestProgress {

  fun onUpdateProgress(single: Long, read: Long, total: Long)
}

/** Injects a progress indicator to the response body. **/
fun Single<Response>.progress(progress: (single: Long, read: Long, total: Long) -> Unit): Single<Response> = map { response ->
  val body = response.body()!!
  val contentLength = body.contentLength()

  if (contentLength > 0) {
    val source = body.source()
    val progressSource = object : Source {
      private var read: Long = 0L

      override fun read(sink: Buffer, byteCount: Long): Long = source.read(sink, byteCount).also { single ->
        if (single != -1L) {
          read += single
          progress(single, read, contentLength)
        }
      }

      override fun close() = source.close()

      override fun timeout(): Timeout = source.timeout()
    }

    response.newBuilder()
        .body(SimpleResponseBody(response.headers(), Okio.buffer(progressSource)))
        .build()
  } else {
    // No valid content length, just return original response
    response
  }
}

interface ResponseCache {

  fun body(): Sink

  fun abort()
}

/**
 * Injects a cache to save response body.
 *
 * @param cache the cache to save response body
 */
fun Single<Response>.cache(cache: ResponseCache): Single<Response> = map { response ->
  val source = response.body()!!.source()
  val cacheBody = Okio.buffer(cache.body())

  val cacheSource = object : Source {
    private var cacheClosed: Boolean = false

    override fun read(sink: Buffer, byteCount: Long): Long {
      val bytesRead: Long
      try {
        bytesRead = source.read(sink, byteCount)
      } catch (e: IOException) {
        if (!cacheClosed) {
          cacheClosed = true
          cache.abort() // Failed to write a complete cache response.
        }
        throw e
      }

      if (bytesRead == -1L) {
        if (!cacheClosed) {
          cacheClosed = true
          cacheBody.close() // The cache response is complete!
        }
        return -1L
      }

      sink.copyTo(cacheBody.buffer(), sink.size() - bytesRead, bytesRead)
      cacheBody.emitCompleteSegments()
      return bytesRead
    }

    override fun close() = source.close().also {
      if (!cacheClosed) {
        cacheClosed = true
        cache.abort()
      }
    }

    override fun timeout(): Timeout = source.timeout()
  }

  response.newBuilder()
      .body(SimpleResponseBody(response.headers(), Okio.buffer(cacheSource)))
      .build()
}
