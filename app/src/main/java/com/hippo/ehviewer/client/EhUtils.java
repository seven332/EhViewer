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

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.hippo.ehviewer.R;
import com.hippo.yorozuya.StringUtils;
import java.util.Locale;
import java.util.regex.Pattern;

public final class EhUtils {
  private EhUtils() {}

  ///////////////////////////////////////////////////////////////////////////
  // Gallery Category
  ///////////////////////////////////////////////////////////////////////////

  public static final int CATEGORY_MISC = 0x1;
  public static final int CATEGORY_DOUJINSHI = 0x2;
  public static final int CATEGORY_MANGA = 0x4;
  public static final int CATEGORY_ARTIST_CG = 0x8;
  public static final int CATEGORY_GAME_CG = 0x10;
  public static final int CATEGORY_IMAGE_SET = 0x20;
  public static final int CATEGORY_COSPLAY = 0x40;
  public static final int CATEGORY_ASIAN_PORN = 0x80;
  public static final int CATEGORY_NON_H = 0x100;
  public static final int CATEGORY_WESTERN = 0x200;
  /**
   * No category specify. Represents homepage.
   */
  public static final int CATEGORY_NONE = 0;
  /**
   * Unknown category.
   * <p>
   * Used when can't parse the category string.
   */
  public static final int CATEGORY_UNKNOWN = -1;

  private static final int[] CATEGORY_VALUES = {
      CATEGORY_MISC,
      CATEGORY_DOUJINSHI,
      CATEGORY_MANGA,
      CATEGORY_ARTIST_CG,
      CATEGORY_GAME_CG,
      CATEGORY_IMAGE_SET,
      CATEGORY_COSPLAY,
      CATEGORY_ASIAN_PORN,
      CATEGORY_NON_H,
      CATEGORY_WESTERN,
  };

  private static final String[][] CATEGORY_STRINGS = {
      new String[] { "misc" },
      new String[] { "doujinshi" },
      new String[] { "manga" },
      new String[] { "artist cg", "artistcg", "artist cg sets" },
      new String[] { "game cg", "gamecg", "game cg sets" },
      new String[] { "image set", "imageset", "image sets" },
      new String[] { "cosplay" },
      new String[] { "asian porn", "asianporn", "asian porn" },
      new String[] { "non-h" },
      new String[] { "western" },
  };

  public static final int COLOR_DOUJINSHI = 0xfff44336; // red_500
  public static final int COLOR_MANGA = 0xffff9800; // orange_500
  public static final int COLOR_ARTIST_CG = 0xffffc107; // amber_500
  public static final int COLOR_GAME_CG = 0xff4caf50; // green_500
  public static final int COLOR_WESTERN = 0xff8bc34a; // light_green_500
  public static final int COLOR_NON_H = 0xff2196f3; // blue_500
  public static final int COLOR_IMAGE_SET = 0xff3f51b5; // indigo_500
  public static final int COLOR_COSPLAY = 0xff9c27b0; // purple_500
  public static final int COLOR_ASIAN_PORN = 0xff9575cd; // deep_purple_300
  public static final int COLOR_MISC = 0xfff06292; // pink_300
  public static final int COLOR_UNKNOWN = Color.BLACK;

  /**
   * Converts category string to category value.
   * Returns {@link #CATEGORY_UNKNOWN} if can't recognize the category.
   */
  public static int getCategory(String type) {
    if (TextUtils.isEmpty(type)) {
      return CATEGORY_UNKNOWN;
    }

    type = type.toLowerCase(Locale.ENGLISH);
    for (int i = 0, n = CATEGORY_STRINGS.length; i < n; ++i) {
      for (String str : CATEGORY_STRINGS[i]) {
        if (type.equals(str)) {
          return CATEGORY_VALUES[i];
        }
      }
    }

    return CATEGORY_UNKNOWN;
  }

  /**
   * Converts category value to category string.
   * Returns {@code null} if can't recognize the category.
   */
  @Nullable
  public static String getCategory(int type) {
    for (int i = 0, n = CATEGORY_VALUES.length; i < n; ++i) {
      if (CATEGORY_VALUES[i] == type) {
        return CATEGORY_STRINGS[i][0];
      }
    }
    return null;
  }

  /**
   * Converts category value to category string.
   * Returns {@code "unknown"} if can't recognize the category.
   */
  @NonNull
  public static String getCategoryNotNull(int type) {
    String category = getCategory(type);
    if (category != null) {
      return category;
    } else {
      return "unknown";
    }
  }

  /**
   * Get the color to represent the category.
   * Returns black if can't recognize the category.
   */
  public static int getCategoryColor(int category) {
    switch (category) {
      case CATEGORY_DOUJINSHI:
        return COLOR_DOUJINSHI;
      case CATEGORY_MANGA:
        return COLOR_MANGA;
      case CATEGORY_ARTIST_CG:
        return COLOR_ARTIST_CG;
      case CATEGORY_GAME_CG:
        return COLOR_GAME_CG;
      case CATEGORY_WESTERN:
        return COLOR_WESTERN;
      case CATEGORY_NON_H:
        return COLOR_NON_H;
      case CATEGORY_IMAGE_SET:
        return COLOR_IMAGE_SET;
      case CATEGORY_COSPLAY:
        return COLOR_COSPLAY;
      case CATEGORY_ASIAN_PORN:
        return COLOR_ASIAN_PORN;
      case CATEGORY_MISC:
        return COLOR_MISC;
      default:
        return COLOR_UNKNOWN;
    }
  }

  /**
   * Gets the theme to represent the category.
   * Returns {@code 0} if can't recognize the category.
   */
  public static int getCategoryTheme(int category, boolean dark) {
    switch (category) {
      case CATEGORY_DOUJINSHI:
        return dark ? R.style.AppTheme_Dark_Ehv_Doujinshi : R.style.AppTheme_Ehv_Doujinshi;
      case CATEGORY_MANGA:
        return dark ? R.style.AppTheme_Dark_Ehv_Manga : R.style.AppTheme_Ehv_Manga;
      case CATEGORY_ARTIST_CG:
        return dark ? R.style.AppTheme_Dark_Ehv_ArtistCG : R.style.AppTheme_Ehv_ArtistCG;
      case CATEGORY_GAME_CG:
        return dark ? R.style.AppTheme_Dark_Ehv_GameCG : R.style.AppTheme_Ehv_GameCG;
      case CATEGORY_WESTERN:
        return dark ? R.style.AppTheme_Dark_Ehv_Western : R.style.AppTheme_Ehv_Western;
      case CATEGORY_NON_H:
        return dark ? R.style.AppTheme_Dark_Ehv_NonH : R.style.AppTheme_Ehv_NonH;
      case CATEGORY_IMAGE_SET:
        return dark ? R.style.AppTheme_Dark_Ehv_ImageSet : R.style.AppTheme_Ehv_ImageSet;
      case CATEGORY_COSPLAY:
        return dark ? R.style.AppTheme_Dark_Ehv_Cosplay : R.style.AppTheme_Ehv_Cosplay;
      case CATEGORY_ASIAN_PORN:
        return dark ? R.style.AppTheme_Dark_Ehv_AsianPorn : R.style.AppTheme_Ehv_AsianPorn;
      case CATEGORY_MISC:
        return dark ? R.style.AppTheme_Dark_Ehv_Misc : R.style.AppTheme_Ehv_Misc;
      default:
        return 0;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Language
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Unknown
   */
  public static final int LANG_UNKNOWN = -1;
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
  public static final int LANG_OTHER = 16;

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

  private static final int[] LANG_ABBR_IDS = {
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
      R.string.language_abbr_vietnamese,
  };

  private static final int[] LANG_IDS = {
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
      R.string.language_other,
  };

  private static final String[] LANG_TEXTS = {
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
      "vietnamese",
  };

  /**
   * Guesses the language of the gallery according to the title.
   * Returns {@link #LANG_UNKNOWN} if can't guess.
   */
  public static int guessLang(String title) {
    if (StringUtils.isEmpty(title)) {
      return LANG_UNKNOWN;
    }
    for (int i = 0, n = GUESS_LANGS.length; i < n; ++i) {
      if (GUESS_LANG_PATTERNS[i].matcher(title).find()) {
        return GUESS_LANGS[i];
      }
    }
    return LANG_UNKNOWN;
  }

  /**
   * Returns abbreviation for the language.
   * Returns {@code null} if no abbreviation for the language.
   */
  @Nullable
  public static String getLangAbbr(Context context, int lang) {
    if (lang >= 0 && lang < LANG_ABBR_IDS.length) {
      return context.getString(LANG_ABBR_IDS[lang]);
    } else {
      return null;
    }
  }

  /**
   * Returns string for the language.
   * Returns {@code null} if no string for the language.
   */
  @Nullable
  public static String getLang(Context context, int lang) {
    if (lang >= 0 && lang < LANG_IDS.length) {
      return context.getString(LANG_IDS[lang]);
    } else {
      return null;
    }
  }

  /**
   * Returns string for the language.
   * Returns a string to represent unknown if can't recognize the category.
   */
  @Nullable
  public static String getLangNotNull(Context context, int lang) {
    String language = getLang(context, lang);
    if (language != null) {
      return language;
    } else {
      return context.getString(R.string.language_unknown);
    }
  }

  /**
   * Returns text for the language. The text can be used for tag.
   * Returns {@code null} if no string for the language.
   */
  @Nullable
  public static String getLangText(int lang) {
    if (lang >= 0 && lang < LANG_TEXTS.length) {
      return LANG_TEXTS[lang];
    } else {
      return null;
    }
  }


  ///////////////////////////////////////////////////////////////////////////
  // Favourite
  ///////////////////////////////////////////////////////////////////////////

  public static final int FAV_CAT_ALL = -1;
  public static final int FAV_CAT_LOCAL = -2;
  public static final int FAV_CAT_UNKNOWN = -3;
  public static final int FAV_CAT_MAX = 9;
}
