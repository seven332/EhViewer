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
import com.hippo.ehviewer.client.guessLang
import com.hippo.ehviewer.client.parser.url.parseCoverUrl
import com.hippo.ehviewer.client.parser.url.parseGalleryDetailUrl
import com.hippo.ehviewer.exception.ParseException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern

/*
 * Created by Hippo on 2017/7/25.
 */

private val EMPTY_KEYWORD = "No hits found</p>"

private val PATTERN_COVER_SIZE = Pattern.compile("height:(\\d+)px; width:(\\d+)px")
private val PATTERN_PX = Pattern.compile("(\\d+)px")

/**
 * Parses gallery list.
 */
fun <T> parseGalleryList(document: Document, creator: (GalleryInfo, Element) -> T): List<T>? {
  val tbody = document.elementByClass("itg")?.firstChild()
  if (tbody != null) {
    val list = mutableListOf<T>()

    var isHeader = true
    for (element in tbody.children()) {
      // First one is table header, skip it
      if (isHeader) {
        isHeader = false
        continue
      }

      val t = parseItem(element, creator)
      if (t != null) {
        list.add(t)
      }
    }

    // Only returns the list if it's not empty
    if (!list.isEmpty()) {
      return list
    }
  }

  return null
}

private fun <T> parseItem(element: Element, creator: (GalleryInfo, Element) -> T): T? {
  val it5 = element.elementByClass("it5")?.firstChild()
  val title = it5?.unescape() ?: return null
  val (gid, token) = it5.attr("href")?.parseGalleryDetailUrl() ?: return null

  val info = GalleryInfo()
  info.gid = gid
  info.token = token
  info.title = title
  info.language = guessLang(title)
  info.category = element.elementByClass("ic")?.attr("alt")?.categoryValue() ?: CATEGORY_UNKNOWN
  info.date = element.elementByClass("itd")?.date() ?: 0

  val it2 = element.elementByClass("it2")
  if (it2 != null) {
    PATTERN_COVER_SIZE.matcher(it2.attr("style")).takeIf { it.find() }?.also { matcher ->
      val width = matcher.group(2).integer() ?: 0
      val height = matcher.group(1).integer() ?: 0
      if (width >= 0 && height >= 0) {
        info.coverRatio = width.toFloat() / height.toFloat()
      }
    }

    val cover = it2.firstChild()
    if (cover != null) {
      info.coverUrl = cover.attr("src").unescape()
    } else {
      info.coverUrl = it2.text().parseCoverUrlText()
    }

    if (info.coverUrl != null) {
      info.coverFingerprint = info.coverUrl?.parseCoverUrl()
    }
  }

  val it3 = element.elementByClass("it3")
  if (it3 != null) {
    val favId = "favicon_" + info.gid
    for (child in it3.children()) {
      if (favId == child.id()) {
        info.favouriteSlot = child.attr("style").parseFavouriteSlotStyle()
      }

      if (child.firstChild()?.attr("alt") == "E") {
        info.invalid = true
      }
    }
  }

  info.rating = element.elementByClass("it4r")?.attr("style")?.parseRatingStyle() ?: 0.0f
  info.uploader = element.elementByClass("itu")?.unescape()

  return creator(info, element)
}

// Parses url from a string like:
// inits~exhentai.org~t/53/a8/53a82a8deec79d3824ab413b4cf784d6df8589b2-1264635-1050-1540-png_l.jpg~(C91) [ガンバリマシン (Shino)] Pさん、今日も頑張ってくれませんか？ (アイドルマスター シンデレラガールズ)
private fun String.parseCoverUrlText(): String? {
  val index1 = indexOf('~')
  if (index1 == -1) return null
  val index2 = indexOf('~', index1 + 1)
  if (index2 == -1) return null
  val index3 = indexOf('~', index2 + 1)
  if (index3 == -1) return null
  return "https://" + substring(index1 + 1, index2) + "/" + substring(index2 + 1, index3)
}

/**
 * Parse the style string to get favourite slot.
 *
 * Ehentai website uses a picture to show favourite slot.
 * Looks like:
 * ```
 * ◇
 * ◇
 * ◇
 * ◇
 * ◇
 * ◇
 * ◇
 * ◇
 * ◇
 * ◇
 * ```
 * The style looks like:
 * `background-position:0px -2px`.
 * The first one is x offset, always 0.
 * The second one is y offset, starts from -2, step -19.
 */
fun String.parseFavouriteSlotStyle(): Int {
  val m = PATTERN_PX.matcher(this)
  // Move to the second one
  if (!m.find() || !m.find()) return FAV_CAT_UNKNOWN

  val num = m.group(1).integer() ?: FAV_CAT_UNKNOWN
  val slot = (num - 2) / 19
  if (slot in 0..9) {
    return slot
  } else {
    return FAV_CAT_UNKNOWN
  }
}

/**
 * ehentai website uses a picture to show rating.
 * Looks like:
 * ```
 * ⛤ ⛤ ⛤ ⛤ ⛤
 * ⛤ ⛤ ⛤ ⛤ half⛤
 * ```
 * The style looks like:
 * ```
 * background-position:-16px -1px
 * ```
 * The first one is x offset, 16px for a star.
 * The second one is y offset, row1 is -1px, row2 is -21px
 */
fun String.parseRatingStyle(): Float {
  val matcher = PATTERN_PX.matcher(this)
  val num1 = matcher.takeIf { matcher.find() }?.group(1)?.integer() ?: return 0.0f
  val num2 = matcher.takeIf { matcher.find() }?.group(1)?.integer() ?: return 0.0f
  var rate = 5.0f - num1 / 16
  if (num2 == 21) {
    rate -= 0.5f
  }
  return rate
}

/**
 * Parses GalleryInfo list.
 */
fun parseGalleryList(document: Document): List<GalleryInfo>? =
    parseGalleryList(document, { info, _ -> info })

/**
 * Parses gallery list pages.
 */
fun parseGalleryListPages(document: Document): Int? =
    document.elementByClass("ptt")?.firstChild()?.firstChild()?.lastChild(1)?.integer() ?:
        if (document.elementById("toppane")?.nextElementSibling()?.firstChild()?.text() == "No hits found") 0
        else null

fun parseGalleryList(body: String): Pair<List<GalleryInfo>, Int> {
  val document = Jsoup.parse(body)
  val pages = parseGalleryListPages(document) ?: throw ParseException("Can't parse gallery list body")
  val infos = parseGalleryList(document) ?: throw ParseException("Can't parse gallery list")
  return Pair(infos, pages)
}
