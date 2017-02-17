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

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 1/29/2017.
 */

import android.graphics.Color;
import android.support.annotation.Nullable;
import java.util.Locale;
import java.util.regex.Pattern;

public final class EhUtils {
  private EhUtils() {}

  ///////////////////////////////////////////////////////////////////////////
  // Gallery Category
  ///////////////////////////////////////////////////////////////////////////

  public static final int MISC = 0x1;
  public static final int DOUJINSHI = 0x2;
  public static final int MANGA = 0x4;
  public static final int ARTIST_CG = 0x8;
  public static final int GAME_CG = 0x10;
  public static final int IMAGE_SET = 0x20;
  public static final int COSPLAY = 0x40;
  public static final int ASIAN_PORN = 0x80;
  public static final int NON_H = 0x100;
  public static final int WESTERN = 0x200;
  /**
   * No category specify. Represents homepage.
   */
  public static final int NONE = 0;
  /**
   * Unknown category.
   * <p>
   * Used when can't parse the category string.
   */
  public static final int UNKNOWN = -1;

  private static final int[] CATEGORY_VALUES = {
      MISC,
      DOUJINSHI,
      MANGA,
      ARTIST_CG,
      GAME_CG,
      IMAGE_SET,
      COSPLAY,
      ASIAN_PORN,
      NON_H,
      WESTERN,
      UNKNOWN,
  };

  private static final String[][] CATEGORY_STRINGS = {
      new String[] { "misc" },
      new String[] { "doujinshi" },
      new String[] { "manga" },
      new String[] { "artistcg", "artist cg sets" },
      new String[] { "gamecg", "game cg sets" },
      new String[] { "imageset", "image sets" },
      new String[] { "cosplay" },
      new String[] { "asianporn", "asian porn" },
      new String[] { "non-h" },
      new String[] { "western" },
      new String[] { "unknown" },
  };

  public static final int COLOR_DOUJINSHI = 0xfff44336;
  public static final int COLOR_MANGA = 0xffff9800;
  public static final int COLOR_ARTIST_CG = 0xfffbc02d;
  public static final int COLOR_GAME_CG = 0xff4caf50;
  public static final int COLOR_WESTERN = 0xff8bc34a;
  public static final int COLOR_NON_H = 0xff2196f3;
  public static final int COLOR_IMAGE_SET = 0xff3f51b5;
  public static final int COLOR_COSPLAY = 0xff9c27b0;
  public static final int COLOR_ASIAN_PORN = 0xff9575cd;
  public static final int COLOR_MISC = 0xfff06292;
  public static final int COLOR_UNKNOWN = Color.BLACK;

  /**
   * Converts category string to category value.
   */
  public static int getCategory(String type) {
    if (type == null) {
      return CATEGORY_VALUES[CATEGORY_VALUES.length - 1];
    }
    int i, n;
    type = type.toLowerCase(Locale.ENGLISH);
    out:
    for (i = 0, n = CATEGORY_STRINGS.length - 1; i < n; ++i) {
      for (String str : CATEGORY_STRINGS[i]) {
        if (type.equals(str)) break out;
      }
    }
    return CATEGORY_VALUES[i];
  }

  /**
   * Converts category value to category string.
   */
  public static String getCategory(int type) {
    int i, n;
    for (i = 0, n = CATEGORY_VALUES.length - 1; i < n; ++i) {
      if (CATEGORY_VALUES[i] == type) break;
    }
    return CATEGORY_STRINGS[i][0];
  }

  /**
   * Get the color to represent the category.
   */
  public static int getColor(int category) {
    switch (category) {
      case DOUJINSHI:
        return COLOR_DOUJINSHI;
      case MANGA:
        return COLOR_MANGA;
      case ARTIST_CG:
        return COLOR_ARTIST_CG;
      case GAME_CG:
        return COLOR_GAME_CG;
      case WESTERN:
        return COLOR_WESTERN;
      case NON_H:
        return COLOR_NON_H;
      case IMAGE_SET:
        return COLOR_IMAGE_SET;
      case COSPLAY:
        return COLOR_COSPLAY;
      case ASIAN_PORN:
        return COLOR_ASIAN_PORN;
      case MISC:
        return COLOR_MISC;
      default:
        return COLOR_UNKNOWN;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Language
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Japanese
   */
  public static final int LANG_JA = 0;
  /**
   * English
   */
  public static final int LANG_EN = 1;
  /**
   * Chinese
   */
  public static final int LANG_ZH = 2;
  /**
   * Dutch
   */
  public static final int LANG_NL = 3;
  /**
   * French
   */
  public static final int LANG_FR = 4;
  /**
   * German
   */
  public static final int LANG_DE = 5;
  /**
   * Hungarian
   */
  public static final int LANG_HU = 6;
  /**
   * Italian
   */
  public static final int LANG_IT = 7;
  /**
   * Korean
   */
  public static final int LANG_KO = 8;
  /**
   * Polish
   */
  public static final int LANG_PL = 9;
  /**
   * Portuguese
   */
  public static final int LANG_PT = 10;
  /**
   * Russian
   */
  public static final int LANG_RU = 11;
  /**
   * Spanish
   */
  public static final int LANG_ES = 12;
  /**
   * Thai
   */
  public static final int LANG_TH = 13;
  /**
   * Vietnamese
   */
  public static final int LANG_VI = 14;
  /**
   * N/A
   */
  public static final int LANG_N_A = 15;
  /**
   * Other
   */
  public static final int LANG_OTHER = 15;

  public static final String LANG_ABBR_JA = "JA";
  public static final String LANG_ABBR_EN = "EN";
  public static final String LANG_ABBR_ZH = "ZH";
  public static final String LANG_ABBR_NL = "NL";
  public static final String LANG_ABBR_FR = "FR";
  public static final String LANG_ABBR_DE = "DE";
  public static final String LANG_ABBR_HU = "HU";
  public static final String LANG_ABBR_IT = "IT";
  public static final String LANG_ABBR_KO = "KO";
  public static final String LANG_ABBR_PL = "PL";
  public static final String LANG_ABBR_PT = "PT";
  public static final String LANG_ABBR_RU = "RU";
  public static final String LANG_ABBR_ES = "ES";
  public static final String LANG_ABBR_TH = "TH";
  public static final String LANG_ABBR_VI = "VI";

  private static final Pattern LANG_PATTERN_EN =
      Pattern.compile("[(\\[]eng(?:lish)?[)\\]]|英訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_ZH =
      Pattern.compile("[(（\\[]ch(?:inese)?[)）\\]]|[汉漢]化|中[国國][语語]|中文|中国翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_NL =
      Pattern.compile("[(\\[]dutch[)\\]]|オランダ語訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_FR =
      Pattern.compile("[(\\[]fr(?:ench)?[)\\]]|フランス翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_DE =
      Pattern.compile("[(\\[]german[)\\]]|ドイツ翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_HU =
      Pattern.compile("[(\\[]hun(?:garian)?[)\\]]|ハンガリー翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_IT =
      Pattern.compile("[(\\[]italiano?[)\\]]|イタリア翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_KO =
      Pattern.compile("[(\\[]korean?[)\\]]|韓国翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_PL =
      Pattern.compile("[(\\[]polish[)\\]]|ポーランド翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_PT =
      Pattern.compile("[(\\[]portuguese|ポルトガル翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_RU =
      Pattern.compile("[(\\[]rus(?:sian)?[)\\]]|ロシア翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_ES =
      Pattern.compile("[(\\[]spanish[)\\]]|[(\\[]Español[)\\]]|スペイン翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_TH =
      Pattern.compile("[(\\[]thai(?: ภาษาไทย)?[)\\]]|แปลไทย|タイ翻訳", Pattern.CASE_INSENSITIVE);
  private static final Pattern LANG_PATTERN_VI =
      Pattern.compile("[(\\[]vietnamese(?: Tiếng Việt)?[)\\]]|ベトナム翻訳", Pattern.CASE_INSENSITIVE);

  private static final int[] GUESS_LANGS = {
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
      LANG_NL,
  };

  private static final Pattern[] GUESS_LANG_PATTERNS = {
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
      LANG_PATTERN_NL,
  };

  private static final String[] LANG_ABBRS = {
      LANG_ABBR_JA,
      LANG_ABBR_EN,
      LANG_ABBR_ZH,
      LANG_ABBR_NL,
      LANG_ABBR_FR,
      LANG_ABBR_DE,
      LANG_ABBR_HU,
      LANG_ABBR_IT,
      LANG_ABBR_KO,
      LANG_ABBR_PL,
      LANG_ABBR_PT,
      LANG_ABBR_RU,
      LANG_ABBR_ES,
      LANG_ABBR_TH,
      LANG_ABBR_VI,
  };

  /**
   * Guesses the language of the gallery according to the title.
   * Returns {@link #LANG_OTHER} if can't guess.
   */
  public static int guessLang(String title) {
    for (int i = 0, n = GUESS_LANGS.length; i < n; ++i) {
      if (GUESS_LANG_PATTERNS[i].matcher(title).find()) {
        return GUESS_LANGS[i];
      }
    }
    return LANG_OTHER;
  }

  /**
   * Returns abbreviation for the language.
   * Returns {@code null} if no abbreviation for the language.
   */
  @Nullable
  public static String getLangAbbr(int lang) {
    if (lang >= 0 && lang < LANG_ABBRS.length) {
      return LANG_ABBRS[lang];
    } else {
      return null;
    }
  }
}
