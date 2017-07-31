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

package com.hippo.ehviewer.network

import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.ResponseBody
import okhttp3.internal.http.HttpHeaders
import okio.BufferedSource

/*
 * Created by Hippo on 2017/7/24.
 */

class SimpleResponseBody(val headers: Headers, val source: BufferedSource) : ResponseBody() {

  override fun contentType(): MediaType? {
    val contentType = headers.get("Content-Type")
    return if (contentType != null) MediaType.parse(contentType) else null
  }

  override fun contentLength(): Long {
    return HttpHeaders.contentLength(headers)
  }

  override fun source(): BufferedSource {
    return source
  }
}
