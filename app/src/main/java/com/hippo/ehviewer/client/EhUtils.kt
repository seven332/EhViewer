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

import android.graphics.Color
import com.hippo.ehviewer.R
import java.util.regex.Pattern

/*
 * Created by Hippo on 2017/7/25.
 */

///////////////////////////////////////////////////////////////////////////
// Category
///////////////////////////////////////////////////////////////////////////

const val CATEGORY_MISC = 0x1
const val CATEGORY_DOUJINSHI = 0x2
const val CATEGORY_MANGA = 0x4
const val CATEGORY_ARTIST_CG = 0x8
const val CATEGORY_GAME_CG = 0x10
const val CATEGORY_IMAGE_SET = 0x20
const val CATEGORY_COSPLAY = 0x40
const val CATEGORY_ASIAN_PORN = 0x80
const val CATEGORY_NON_H = 0x100
const val CATEGORY_WESTERN = 0x200
/** Indicate no category specify **/
const val CATEGORY_NONE = 0
/** Unknown category **/
const val CATEGORY_UNKNOWN = -1

private val CATEGORY_VALUES = intArrayOf(
    CATEGORY_MISC,
    CATEGORY_DOUJINSHI,
    CATEGORY_MANGA,
    CATEGORY_ARTIST_CG,
    CATEGORY_GAME_CG,
    CATEGORY_IMAGE_SET,
    CATEGORY_COSPLAY,
    CATEGORY_ASIAN_PORN,
    CATEGORY_NON_H,
    CATEGORY_WESTERN
)

private val CATEGORY_STRINGS = arrayOf(
    arrayOf("misc"),
    arrayOf("doujinshi"),
    arrayOf("manga"),
    arrayOf("artist cg", "artistcg", "artist cg sets"),
    arrayOf("game cg", "gamecg", "game cg sets"),
    arrayOf("image set", "imageset", "image sets"),
    arrayOf("cosplay"),
    arrayOf("asian porn", "asianporn", "asian porn"),
    arrayOf("non-h"),
    arrayOf("western")
)

private val CATEGORY_STRING_UNKNOWN = "unknown"

/**
 * Converts category value to category string.
 * Returns `null` if invalid.
 */
fun Int.categoryString(): String? {
  val index = CATEGORY_VALUES.indexOf(this)
  if (index >= 0) {
    return CATEGORY_STRINGS[index][0]
  } else {
    return null
  }
}

/**
 * Converts category value to category string.
 * Returns `unknown` if invalid.
 */
fun Int.categoryStringNonNull(): String = categoryString() ?: CATEGORY_STRING_UNKNOWN

/**
 * Convert category string to category value.
 * Returns [CATEGORY_UNKNOWN] if invalid.
 */
fun String.categoryValue(): Int {
  val lowerCase = this.toLowerCase()
  val index = CATEGORY_STRINGS.indexOfFirst { it.contains(lowerCase) }
  if (index >= 0) {
    return CATEGORY_VALUES[index]
  } else {
    return CATEGORY_UNKNOWN
  }
}

const val COLOR_DOUJINSHI = 0xfff44336.toInt() // red_500
const val COLOR_MANGA = 0xffff9800.toInt() // orange_500
const val COLOR_ARTIST_CG = 0xffffc107.toInt() // amber_500
const val COLOR_GAME_CG = 0xff4caf50.toInt() // green_500
const val COLOR_WESTERN = 0xff8bc34a.toInt() // light_green_500
const val COLOR_NON_H = 0xff2196f3.toInt() // blue_500
const val COLOR_IMAGE_SET = 0xff3f51b5.toInt() // indigo_500
const val COLOR_COSPLAY = 0xff9c27b0.toInt() // purple_500
const val COLOR_ASIAN_PORN = 0xff9575cd.toInt() // deep_purple_300
const val COLOR_MISC = 0xfff06292.toInt() // pink_300
const val COLOR_UNKNOWN = Color.BLACK

/**
 * Get the color to represent the category.
 * Returns black if can't recognize the category.
 */
fun Int.categoryColor(): Int {
  when (this) {
    CATEGORY_DOUJINSHI -> return COLOR_DOUJINSHI
    CATEGORY_MANGA -> return COLOR_MANGA
    CATEGORY_ARTIST_CG -> return COLOR_ARTIST_CG
    CATEGORY_GAME_CG -> return COLOR_GAME_CG
    CATEGORY_WESTERN -> return COLOR_WESTERN
    CATEGORY_NON_H -> return COLOR_NON_H
    CATEGORY_IMAGE_SET -> return COLOR_IMAGE_SET
    CATEGORY_COSPLAY -> return COLOR_COSPLAY
    CATEGORY_ASIAN_PORN -> return COLOR_ASIAN_PORN
    CATEGORY_MISC -> return COLOR_MISC
    else -> return COLOR_UNKNOWN
  }
}

///////////////////////////////////////////////////////////////////////////
// Language
///////////////////////////////////////////////////////////////////////////

/** Japanese **/
const val LANG_JA = 0
/** English **/
const val LANG_EN = 1
/** Chinese **/
const val LANG_ZH = 2
/** Dutch **/
const val LANG_NL = 3
/** French **/
const val LANG_FR = 4
/** German **/
const val LANG_DE = 5
/** Hungarian **/
const val LANG_HU = 6
/** Italian **/
const val LANG_IT = 7
/** Korean **/
const val LANG_KO = 8
/** Polish **/
const val LANG_PL = 9
/** Portuguese **/
const val LANG_PT = 10
/** Russian **/
const val LANG_RU = 11
/** Spanish **/
const val LANG_ES = 12
/** Thai **/
const val LANG_TH = 13
/** Vietnamese **/
const val LANG_VI = 14
/** N/A **/
const val LANG_N_A = 15
/** Other **/
const val LANG_OTHER = 16
/** Unknown **/
val LANG_UNKNOWN = -1

private val LANG_PATTERN_EN = Pattern.compile("[(\\[]eng(?:lish)?[)\\]]|英訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_ZH = Pattern.compile("[(（\\[]ch(?:inese)?[)）\\]]|[汉漢]化|中[国國][语語]|中文|中国翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_NL = Pattern.compile("[(\\[]dutch[)\\]]|オランダ語訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_FR = Pattern.compile("[(\\[]fr(?:ench)?[)\\]]|フランス翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_DE = Pattern.compile("[(\\[]german[)\\]]|ドイツ翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_HU = Pattern.compile("[(\\[]hun(?:garian)?[)\\]]|ハンガリー翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_IT = Pattern.compile("[(\\[]italiano?[)\\]]|イタリア翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_KO = Pattern.compile("[(\\[]korean?[)\\]]|韓国翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_PL = Pattern.compile("[(\\[]polish[)\\]]|ポーランド翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_PT = Pattern.compile("[(\\[]portuguese|ポルトガル翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_RU = Pattern.compile("[(\\[]rus(?:sian)?[)\\]]|ロシア翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_ES = Pattern.compile("[(\\[]spanish[)\\]]|[(\\[]Español[)\\]]|スペイン翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_TH = Pattern.compile("[(\\[]thai(?: ภาษาไทย)?[)\\]]|แปลไทย|タイ翻訳", Pattern.CASE_INSENSITIVE)
private val LANG_PATTERN_VI = Pattern.compile("[(\\[]vietnamese(?: Tiếng Việt)?[)\\]]|ベトナム翻訳", Pattern.CASE_INSENSITIVE)

private val GUESS_LANGS = intArrayOf(
    LANG_EN,
    LANG_ZH,
    LANG_ES,
    LANG_KO,
    LANG_RU,
    LANG_FR,
    LANG_PT,
    LANG_TH,
    LANG_DE,
    LANG_IT,
    LANG_VI,
    LANG_PL,
    LANG_HU,
    LANG_NL
)

private val GUESS_LANG_PATTERNS = arrayOf(
    LANG_PATTERN_EN,
    LANG_PATTERN_ZH,
    LANG_PATTERN_ES,
    LANG_PATTERN_KO,
    LANG_PATTERN_RU,
    LANG_PATTERN_FR,
    LANG_PATTERN_PT,
    LANG_PATTERN_TH,
    LANG_PATTERN_DE,
    LANG_PATTERN_IT,
    LANG_PATTERN_VI,
    LANG_PATTERN_PL,
    LANG_PATTERN_HU,
    LANG_PATTERN_NL
)

fun guessLang(title: String): Int {
  val index = GUESS_LANG_PATTERNS.indexOfFirst { it.matcher(title).find() }
  if (index >= 0) {
    return GUESS_LANGS[index]
  } else {
    return LANG_UNKNOWN
  }
}

private val LANG_ABBR_IDS = intArrayOf(
    R.string.language_abbr_japanese,
    R.string.language_abbr_english,
    R.string.language_abbr_chinese,
    R.string.language_abbr_dutch,
    R.string.language_abbr_french,
    R.string.language_abbr_german,
    R.string.language_abbr_hungarian,
    R.string.language_abbr_italian,
    R.string.language_abbr_korean,
    R.string.language_abbr_polish,
    R.string.language_abbr_portuguese,
    R.string.language_abbr_russian,
    R.string.language_abbr_spanish,
    R.string.language_abbr_thai,
    R.string.language_abbr_vietnamese
)

private val LANG_IDS = intArrayOf(
    R.string.language_japanese,
    R.string.language_english,
    R.string.language_chinese,
    R.string.language_dutch,
    R.string.language_french,
    R.string.language_german,
    R.string.language_hungarian,
    R.string.language_italian,
    R.string.language_korean,
    R.string.language_polish,
    R.string.language_portuguese,
    R.string.language_russian,
    R.string.language_spanish,
    R.string.language_thai,
    R.string.language_vietnamese,
    R.string.language_n_a,
    R.string.language_other
)

private val LANG_TEXTS = arrayOf(
    "japanese",
    "english",
    "chinese",
    "dutch",
    "french",
    "german",
    "hungarian",
    "italian",
    "korean",
    "polish",
    "portuguese",
    "russian",
    "spanish",
    "thai",
    "vietnamese"
)

/**
 * Returns abbreviation resource id for the language.
 * Returns `0` if invalid.
 */
fun Int.langAbbr(): Int {
  if (this in 0 until LANG_ABBR_IDS.size) {
    return LANG_ABBR_IDS[this]
  } else {
    return 0
  }
}

/**
 * Returns language resource id for the language.
 * Returns `0` if invalid.
 */
fun Int.lang(): Int {
  if (this in 0 until LANG_IDS.size) {
    return LANG_IDS[this]
  } else {
    return 0
  }
}

/**
 * Returns text for the language.
 * Returns `null` if invalid.
 */
fun Int.langText(): String? {
  if (this in 0 until LANG_TEXTS.size) {
    return LANG_TEXTS[this]
  } else {
    return null
  }
}

/**
 * Returns value for the language.
 * Returns [LANG_UNKNOWN] if invalid.
 */
fun String.lang(): Int {
  val lowerCase = this.toLowerCase()
  val index = LANG_TEXTS.indexOfFirst { it == lowerCase }
  return if (index >= 0) index else LANG_UNKNOWN
}

///////////////////////////////////////////////////////////////////////////
// Favourites
///////////////////////////////////////////////////////////////////////////

const val FAV_CAT_UNKNOWN = -1
