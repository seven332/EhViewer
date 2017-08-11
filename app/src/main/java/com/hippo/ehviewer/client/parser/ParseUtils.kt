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

@file:Suppress("NOTHING_TO_INLINE")

package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.util.replaceEach
import com.hippo.ehviewer.util.strip
import org.jsoup.nodes.Element
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/*
 * Created by Hippo on 2017/7/26.
 */

private val DATE_FORMAT: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }

private val COMMENT_DATE_FORMAT: DateFormat = SimpleDateFormat("dd MMMMM yyyy, HH:mm z", Locale.ENGLISH)

fun String.integer(): Int? = dropWhile { it == ',' || Character.isSpaceChar(it) || Character.isWhitespace(it) }.toIntOrNull()

fun String.float(): Float? = dropWhile { it == ',' || Character.isSpaceChar(it) || Character.isWhitespace(it) }.toFloat()

fun String.unescape(): String? = replaceEach(
    arrayOf("&amp;", "&lt;", "&gt;", "&quot;", "&#039;", "&times;", "&nbsp;"),
    arrayOf("&", "<", ">", "\"", "'", "×", "\u00a0")).strip().let { if (it.isEmpty()) null else it }

fun String.date(): Long = try { DATE_FORMAT.parse(this).time } catch (e: ParseException) { 0 }

fun String.commentDate(): Long = try { COMMENT_DATE_FORMAT.parse(this).time } catch (e: ParseException) { 0 }

inline fun Element.elementById(id: String): Element? = getElementById(id)

inline fun Element.elementByClass(className: String): Element? = getElementsByClass(className).first()

inline fun Element.elementByTag(tagName: String): Element? = getElementsByTag(tagName).first()

inline fun Element.firstChild(): Element? = children().first()

inline fun Element.firstChild(index: Int): Element? = children().let { if (it.size > index) it[index] else null }

inline fun Element.lastChild(): Element? = children().last()

inline fun Element.lastChild(index: Int): Element? = children().let { if (it.size > index) it[it.size - index - 1] else null }

inline fun Element.integer(): Int? = text().integer()

inline fun Element.float(): Float? = text().float()

inline fun Element.unescape(): String? = text().unescape()

inline fun Element.date(): Long = text().date()
