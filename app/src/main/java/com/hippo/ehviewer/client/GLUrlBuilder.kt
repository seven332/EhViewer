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

import com.hippo.ehviewer.client.data.TagSet

/*
 * Created by Hippo on 2017/7/25.
 */

class GLUrlBuilder {

  var page: Int = 0
  var category: Int = CATEGORY_NONE
  var language: Int = LANG_UNKNOWN
  var keyword: String? = null
  val tags = TagSet()

  private fun appendTag(sb: StringBuilder, namespace: String, tag: String) {
    if (sb.isNotEmpty()) sb.append(' ')
    sb.append(namespace).append(":\"").append(tag).append("$\"")
  }

  // Combine keyword, language and tags
  private fun resolveKeyword(): String? {
    val sb = StringBuilder()

    if (!keyword.isNullOrBlank()) {
      sb.append(keyword)
    }

    val language = this.language.langText()
    if (language != null) {
      appendTag(sb, "language", language)
    }

    for ((namespace, list) in tags) {
      for (tag in list) {
        appendTag(sb, namespace, tag)
      }
    }

    return if (sb.isNotEmpty()) sb.toString() else null
  }

  /**
   * Build query map.
   */
  fun build(): Map<String, String> {
    val query = mutableMapOf<String, String>()
    // True if need ("f_apply", "Apply+Filter")
    var filter = false

    // Category
    if (category != CATEGORY_NONE) {
      if (category and CATEGORY_DOUJINSHI != 0) query.put("f_doujinshi", "on")
      if (category and CATEGORY_MANGA != 0) query.put("f_manga", "on")
      if (category and CATEGORY_ARTIST_CG != 0) query.put("f_artistcg", "on")
      if (category and CATEGORY_GAME_CG != 0) query.put("f_gamecg", "on")
      if (category and CATEGORY_WESTERN != 0) query.put("f_western", "on")
      if (category and CATEGORY_NON_H != 0) query.put("f_non-h", "on")
      if (category and CATEGORY_IMAGE_SET != 0) query.put("f_imageset", "on")
      if (category and CATEGORY_COSPLAY != 0) query.put("f_cosplay", "on")
      if (category and CATEGORY_ASIAN_PORN != 0) query.put("f_asianporn", "on")
      if (category and CATEGORY_MISC != 0) query.put("f_misc", "on")
      filter = true
    }

    // Keyword
    val keyword = resolveKeyword()
    if (keyword != null) {
      query.put("f_search", keyword)
      filter = true
    }

    // Page
    if (page != 0) {
      query.put("page", Integer.toString(page))
    }

    // Add filter foot
    if (filter) {
      query.put("f_apply", "Apply Filter")
    }

    return query
  }
}
