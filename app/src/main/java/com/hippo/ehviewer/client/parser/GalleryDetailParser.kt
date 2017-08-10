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

package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.CATEGORY_UNKNOWN
import com.hippo.ehviewer.client.FAV_CAT_UNKNOWN
import com.hippo.ehviewer.client.categoryValue
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.lang
import com.hippo.ehviewer.client.parser.url.parseArchiveUrl
import com.hippo.ehviewer.client.parser.url.parseCoverUrl
import com.hippo.ehviewer.client.parser.url.parseGalleryDetailUrl
import com.hippo.ehviewer.exception.ParseException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.regex.Pattern

/*
 * Created by Hippo on 2017/7/31.
 */

private val PATTERN_COVER = Pattern.compile("width:(\\d+)px; height:(\\d+)px.+?url\\((.+?)\\)")
private val PATTERN_ARCHIVE = Pattern.compile("<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Archive Download</a>")
private val PATTERN_TORRENT = Pattern.compile("<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Torrent Download \\( (\\d+) \\)</a>")

fun parseGalleryDetail(document: Document): GalleryInfo? {
  // Get gid and toke from report url
  val (gid, token) = document.elementById("gd5")?.firstChild()?.firstChild(1)?.attr("href")?.parseGalleryDetailUrl() ?: return null
  val info = GalleryInfo()
  info.gid = gid
  info.token = token

  document.elementById("gd1")?.firstChild()?.attr("style")?.also { style ->
    // // width:250px; height:356px; background:transparent url(https://exhentai.org/t/fe/1f/fe1fcfa9bf8fba2f03982eda0aa347cc9d6a6372-145921-1050-1492-jpg_250.jpg) 0 0 no-repeat
    PATTERN_COVER.matcher(style).takeIf { it.find() }?.also { matcher ->
      val width = matcher.group(1).integer()
      val height = matcher.group(2).integer()
      if (width != null && height != null) {
        info.coverRatio = width.toFloat() / height.toFloat()
      }

      matcher.group(3).unescape().also { url ->
        info.coverUrl = url
        info.coverFingerprint = url.parseCoverUrl()
      }
    }
  }

  document.elementById("gd2")?.also { gd2 ->
    info.title = gd2.elementById("gn")?.unescape()
    info.titleJpn = gd2.elementById("gj")?.unescape()
  }

  document.elementById("gd5")?.html()?.also { html ->
    PATTERN_ARCHIVE.matcher(html).takeIf { it.find() }?.also { matcher ->
      matcher.group(1).unescape().parseArchiveUrl()?.also { (gid, token, archiveKey) ->
        if (info.gid == gid && info.token == token) {
          info.archiveKey = archiveKey
        }
      }
    }

    PATTERN_TORRENT.matcher(html).takeIf { it.find() }?.also { matcher ->
      info.torrents = matcher.group(2).integer() ?: 0
    }
  }

  info.category = document.elementById("gdc")?.firstChild()?.firstChild()?.attr("alt")?.categoryValue() ?: CATEGORY_UNKNOWN
  info.uploader = document.elementById("gdn")?.unescape()

  document.elementById("gdd")?.elementByTag("tbody")?.children()?.forEach { element ->
    element.children().takeIf { it.size >= 2 }?.also { children ->
      val key = children[0].unescape()
      val value = children[1].ownText().unescape()

      if (key.startsWith("Posted")) {
        info.date = value.date()
      } else if (key.startsWith("Parent")) {
        children[1].firstChild()?.attr("href")?.parseGalleryDetailUrl()?.also { (gid, token) ->
          info.parentGid = gid
          info.parentToken = token
        }
      } else if (key.startsWith("Visible")) {
        info.invalid = value != "Yes"
      } else if (key.startsWith("Language")) {
        info.language = value.lang()
      } else if (key.startsWith("File Size")) {
        info.size = value.parseSize()
      } else if (key.startsWith("Length")) {
        info.pages = value.substringBefore(' ').integer() ?: 0
      } else if (key.startsWith("Favorited")) {
        info.favourited = when (value) {
          "Never" -> 0
          "Once" -> 1
          else -> {
            value.substringBefore(' ').integer() ?: 0
          }
        }
      }
    }
  }

  info.rating = document.elementById("rating_label")?.unescape()?.takeIf { it != "Not Yet Rated" }?.substringAfter(' ')?.float() ?: 0.0f
  info.rated = document.elementById("rating_count")?.integer() ?: 0
  info.favouriteSlot = document.elementById("fav")?.firstChild()?.attr("style")?.parseFavouriteSlotStyle() ?: FAV_CAT_UNKNOWN

  document.elementById("taglist")?.elementByTag("tbody")?.children()?.forEach { element ->
    element.children().takeIf { it.size >= 2 }?.also { children ->
      children[0].unescape().trim(':').takeIf { it.isNotEmpty() }?.also { namespace ->
        children[1].children().forEach { div ->
          div.unescape().takeIf { it.isNotEmpty() }?.also { info.tags.add(namespace, it) }
        }
      }
    }
  }

  document.elementById("gnd")?.elementByTag("a")?.attr("href")?.parseGalleryDetailUrl()?.also { (gid, token) ->
    info.childGid = gid
    info.childToken = token
  }

  return info
}

private fun String.parseSize(): Long {
  val index = indexOf(' ')
  if (index < 0 || index >= length - 1) return 0

  val num = substring(0, index).float() ?: return 0
  return when (this[index + 1].toLowerCase()) {
    'k' -> (num * 1024L).toLong()
    'm' -> (num * 1024L * 1024L).toLong()
    'g' -> (num * 1024L * 1024L * 1024L).toLong()
    else -> num.toLong()
  }
}

fun parseGalleryDetail(body: String): GalleryInfo {
  val document = Jsoup.parse(body)
  return parseGalleryDetail(document) ?: throw ParseException("Can't parse gallery detail")
}
