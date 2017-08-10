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

import okhttp3.HttpUrl

/*
 * Created by Hippo on 2017/7/31.
 */

/**
 * Parses a archive url to gid, token and archive key.
 *
 * The url looks like:
 * ```
 * https://exhentai.org/archiver.php?gid=1063451&token=6cc24065cb&or=415253--00cbf86c557d53304ba6d23b7faf64f9928156ca
 * ```
 *
 * Returns `null` if invalid.
 */
fun String.parseArchiveUrl(): Triple<Long, String, String>? {
  val url = HttpUrl.parse(this) ?: return null
  val gid = url.queryParameter("gid")?.toLongOrNull() ?: return null
  val token = url.queryParameter("token") ?: return null
  val archiveKey = url.queryParameter("or") ?: return null
  return Triple(gid, token, archiveKey)
}
