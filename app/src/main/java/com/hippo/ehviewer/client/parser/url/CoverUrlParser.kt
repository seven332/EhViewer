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
 * Created by Hippo on 2017/7/27.
 */

// A thumbnail url looks like:
// https://exhentai.org/t/e8/50/e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg_l.jpg
private val PATTERN_THUMBNAIL = Pattern.compile("/([0-9a-f]{2})/([0-9a-f]{2})/([0-9a-f]{40}-\\d+-\\d+-\\d+-[0-9a-z]+)_([0-9a-zA-Z]+)\\.[0-9a-zA-Z]+")

/**
 * Parses a cover url to fingerprint.
 *
 * Url:
 * ```
 * https://exhentai.org/t/e8/50/e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg_l.jpg
 * ```
 * Fingerprint:
 * ```
 * e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg
 * ```
 *
 * Returns `null` if invalid.
 */
fun String.parseCoverUrl(): String? {
  val m = PATTERN_THUMBNAIL.matcher(this)
  if (m.find()) {
    val g1 = m.group(1)
    val g2 = m.group(2)
    val g3 = m.group(3)
    if (g1 == g3.substring(0, 2) && g2 == g3.substring(2, 4)) {
      return g3
    }
  }
  return null
}
