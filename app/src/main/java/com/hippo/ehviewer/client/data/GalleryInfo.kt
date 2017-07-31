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

package com.hippo.ehviewer.client.data

import com.hippo.ehviewer.client.CATEGORY_UNKNOWN
import com.hippo.ehviewer.client.FAV_CAT_UNKNOWN
import com.hippo.ehviewer.client.LANG_UNKNOWN

/*
 * Created by Hippo on 2017/7/25.
 */

/**
 * `GalleryInfo` is the information about a gallery from api or html parsing.
 */
class GalleryInfo {

  /**
   * Gallery ID.
   *
   * `0` if invalid.
   */
  var gid: Long = 0

  /**
   * Gallery token. Most gallery operations need it.
   *
   * Empty string if invalid
   *
   * Regex:
   * ```
   * [0-9a-f]{10}
   * ```
   *
   * Example:
   * ```
   * c219d2cf41
   * ```
   */
  var token: String? = null

  /**
   * Gallery title.
   */
  var title: String? = null

  /**
   * Gallery japanese title.
   */
  var titleJpn: String? = null

  /**
   * The fingerprint of the first image.
   *
   * Format:
   * ```
   * [sha1]-[size]-[width]-[height]-[format]
   * ```
   *
   * Regex:
   * ```
   * [0-9a-f]{40}-\d+-\d+-\d+-[0-9a-z]+
   * ```
   *
   * Example:
   * ```
   * 7dd3e4a62807a6938910a14407d9867b18a58a9f-2333088-2831-4015-jpg
   * ```
   */
  var coverFingerprint: String? = null

  /**
   * The url of the first image.
   */
  var coverUrl: String? = null

  /**
   * `width / height` of the first image.
   *
   * `0.0f` if invalid.
   */
  var coverRatio: Float = 0.0f

  /**
   * Gallery category.
   *
   * [CATEGORY_UNKNOWN] if invalid.
   */
  var category: Int = CATEGORY_UNKNOWN

  /**
   * Posted time stamp.
   *
   * `0` if invalid.
   */
  var date: Long = 0

  /**
   * The user that uploads the gallery.
   */
  var uploader: String? = null

  /**
   * Gallery Rating.
   *
   * Range:
   * ```
   * [0.5, 5]
   * ```
   *
   * `0.0f` if invalid.
   */
  var rating: Float = 0.0f

  /**
   * How many users rated the gallery.
   *
   * `0` if invalid.
   */
  var rated: Int = 0

  /**
   * Gallery Language.
   *
   * [LANG_UNKNOWN] if invalid.
   */
  var language: Int = LANG_UNKNOWN

  /**
   * How many users favourited the gallery.
   *
   * `0` if can't get it.
   */
  var favourited: Int = 0

  /**
   * Favourite slot.
   *
   * Range: `[-1, 9]`
   *
   * `-1` if invalid.
   */
  var favouriteSlot: Int = FAV_CAT_UNKNOWN

  /**
   * Expunged, deleted or replaced.
   */
  var invalid: Boolean = false

  /**
   * The key to download archive.
   */
  var archiveKey: String? = null

  /**
   * Gallery Pages.
   *
   * `0` if invalid.
   */
  var pages: Int = 0

  /**
   * Gallery size in bytes.
   *
   * `0` if invalid.
   */
  var size: Long = 0

  /**
   * Torrent count.
   *
   * `0` for default.
   */
  var torrents = 0

  /**
   * Gallery tags.
   */
  val tags: TagSet = TagSet()

  /**
   * The gid of the parent gallery.
   *
   * `0` if no parent.
   *
   * The parent is a gallery which is replaced by this gallery.
   */
  var parentGid: Long = 0

  /**
   * The token of the parent gallery.
   *
   * `null` if no parent.
   *
   * The parent is a gallery which is replaced by this gallery.
   */
  var parentToken: String? = null

  /**
   * The gid of the child gallery.
   *
   * `0` if no child.
   *
   * The child is a gallery which replaces this gallery.
   */
  var childGid: Long = 0

  /**
   * The token of the child gallery.
   *
   * `null` if no child.
   *
   * The child is a gallery which replaces this gallery.
   */
  var childToken: String? = null
}
