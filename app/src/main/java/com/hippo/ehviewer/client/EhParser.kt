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

package com.hippo.ehviewer.client

import com.hippo.ehviewer.exception.GeneralException
import com.hippo.ehviewer.exception.ParseException
import com.hippo.ehviewer.exception.ResponseCodeException
import com.hippo.ehviewer.exception.SadPandaException
import io.reactivex.Single
import io.reactivex.functions.Function
import okhttp3.MediaType
import okhttp3.Response

/*
 * Created by Hippo on 2017/7/25.
 */

private val SAD_PANDA_MEDIA_TYPE = MediaType.parse("image/gif")
private const val SAD_PANDA_CONTENT_LENGTH = 9615L

private const val PLAIN_NOTICE_MAX_LENGTH = 512

private class EhParser<T>(val parser: (String) -> T) : Function<Response, T> {

  override fun apply(response: Response): T {
    // Make sure response is closed after applying
    response.use { return doApply(it) }
  }

  private fun doApply(response: Response): T {
    val body = response.body()!!

    // Check sad panda
    if (response.request().url().host() == HOST_EX &&
        body.contentLength() == SAD_PANDA_CONTENT_LENGTH &&
        body.contentType() == SAD_PANDA_MEDIA_TYPE) {
      throw SadPandaException()
    }

    val string = body.string()
    try {
      // Check response code
      if (!response.isSuccessful) {
        throw ResponseCodeException(response.code())
      }

      return parser(string)
    } catch (e: Throwable) {
      // Check plain text body
      if (!string.contains("<") && string.length <= PLAIN_NOTICE_MAX_LENGTH) {
        throw GeneralException(string)
      } else {
        // Fill body of ParseException
        if (e is ParseException) {
          e.body = string
        }
        throw e
      }
    }
  }
}

fun <T> Single<Response>.parse(parser: (String) -> T): Single<T> = map(EhParser(parser))
