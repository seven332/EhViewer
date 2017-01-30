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

import java.util.Locale;

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
}
