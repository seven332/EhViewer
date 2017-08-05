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

package com.hippo.ehviewer.client.parser.url

import java.util.regex.Pattern

/*
 * Created by Hippo on 2017/7/26.
 */

private val URL_PATTERN = Pattern.compile("/g/(\\d+)/([0-9a-f]{10})")

/**
 * Parses a gallery detail url to gid and token.
 *
 * The url looks like:
 * ```
 * https://exhentai.org/g/1060346/03702a68a5/
 * ```
 *
 * Returns `null` if invalid.
 */
fun String.parseGalleryDetailUrl(): Pair<Long, String>? {
  val matcher = URL_PATTERN.matcher(this)
  if (matcher.find()) {
    val gid = matcher.group(1).toLongOrNull()
    val token = matcher.group(2)
    if (gid != null) {
      return Pair(gid, token)
    }
  }
  return null
}
