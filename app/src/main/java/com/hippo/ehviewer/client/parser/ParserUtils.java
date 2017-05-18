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

package com.hippo.ehviewer.client.parser;

/*
 * Created by Hippo on 1/30/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.yorozuya.NumberUtils;
import com.hippo.yorozuya.StringUtils;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public final class ParserUtils {
  private ParserUtils() {}

  private static final String IMPURITIES_INT = ",";

  private final static DateFormat DATE_FORMAT;
  private final static DateFormat DATE_FORMAT_COMMENT;

  static {
    DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    DATE_FORMAT_COMMENT = new SimpleDateFormat("dd MMMMM yyyy, HH:mm z", Locale.ENGLISH);
  }

  /**
   * Remove specified chars in the string.
   */
  private static String cleanString(String str, String impurities, boolean removeWhiteSpace) {
    if (str == null) {
      return null;
    }

    if (impurities == null) {
      impurities = StringUtils.EMPTY;
    }

    StringBuilder sb = new StringBuilder(str.length());
    for (int i = 0, n = str.length(); i < n; ++i) {
      char ch = str.charAt(i);
      if (impurities.indexOf(ch) != -1) {
        continue;
      }
      if (removeWhiteSpace && (Character.isSpaceChar(ch) || Character.isWhitespace(ch))) {
        continue;
      }
      sb.append(ch);
    }
    return sb.toString();
  }

  /**
   * Parses string to int.
   * Example: {@code "   23,532  "}.
   * Return {@code defaultValue} if can't parse the string.
   */
  public static int parseInt(@NonNull String str, int defaultValue) {
    str = unescape(str);
    str = cleanString(str, IMPURITIES_INT, true);
    return NumberUtils.parseInt(str, defaultValue);
  }

  /**
   * Parses string to long.
   * Example: {@code "   23,532  "}.
   * Return {@code defaultValue} if can't parse the string.
   */
  public static long parseLong(@NonNull String str, long defaultValue) {
    str = unescape(str);
    str = cleanString(str, IMPURITIES_INT, true);
    return NumberUtils.parseLong(str, defaultValue);
  }

  /**
   * Parses string to long.
   * Example: {@code "   23,532.67  "}.
   * Return {@code defaultValue} if can't parse the string.
   */
  public static float parseFloat(@NonNull String str, float defaultValue) {
    str = unescape(str);
    str = cleanString(str, IMPURITIES_INT, true);
    return NumberUtils.parseFloat(str, defaultValue);
  }

  /**
   * Parses string to time stamp.
   * Example: {@code "2017-01-30 05:19"}.
   * Return {@code defaultValue} if can't parse the string.
   */
  public static long parseDate(@NonNull String str, long defaultValue) {
    str = unescape(str);
    try {
      return DATE_FORMAT.parse(str).getTime();
    } catch (ParseException e) {
      return defaultValue;
    }
  }

  /**
   * Parses string to time stamp.
   * Example: {@code "16 May 2017, 02:28 UTC"}.
   * Return {@code defaultValue} if can't parse the string.
   */
  public static long parseCommentDate(@NonNull String str, long defaultValue) {
    str = unescape(str);
    try {
      return DATE_FORMAT_COMMENT.parse(str).getTime();
    } catch (ParseException e) {
      return defaultValue;
    }
  }

  /**
   * Converts relative url to absolute url.
   */
  public static String completeUrl(@NonNull String base, @NonNull String url) {
    if (url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    } else {
      return StringUtils.join(base, url, '/');
    }
  }

  /**
   * Remove white characters from both ends of the string.
   * <p>
   * Don't use {@link StringUtils#strip(String)}, it can't remove some characters.
   */
  public static String strip(String str) {
    if (StringUtils.isEmpty(str)) return str;

    int strLen = str.length();

    int start = 0;
    while (start != strLen) {
      char ch = str.charAt(start);
      if (Character.isSpaceChar(ch) || Character.isWhitespace(ch)) {
        start++;
      } else {
        break;
      }
    }

    int end = strLen;
    while (end != start) {
      char ch = str.charAt(end - 1);
      if (Character.isSpaceChar(ch) || Character.isWhitespace(ch)) {
        end--;
      } else {
        break;
      }
    }

    return str.substring(start, end);
  }

  private static final String[] ESCAPE_CHARACTER_LIST = {
      "&amp;",
      "&lt;",
      "&gt;",
      "&quot;",
      "&#039;",
      "&times;",
      "&nbsp;"
  };

  private static final String[] UNESCAPE_CHARACTER_LIST = {
      "&",
      "<",
      ">",
      "\"",
      "'",
      "×",
      "\u00a0"
  };

  /**
   * Unescape xml and strip.
   */
  public static String unescape(String str) {
    str = StringUtils.replaceEach(str, ESCAPE_CHARACTER_LIST, UNESCAPE_CHARACTER_LIST);
    return strip(str);
  }

  /**
   * Returns {@code true} if the string is a url.
   */
  public static boolean isUrl(String str) {
    return str != null && (str.startsWith("http://") || str.startsWith("https://"));
  }
}
