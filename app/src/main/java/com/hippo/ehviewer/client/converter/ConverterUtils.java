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

package com.hippo.ehviewer.client.converter;

/*
 * Created by Hippo on 1/30/2017.
 */

import com.hippo.ehviewer.client.exception.InnerParseException;
import com.hippo.yorozuya.StringUtils;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public final class ConverterUtils {
  private ConverterUtils() {}

  private static final String IMPURITIES_INT = ",";

  private static DateFormat DATE_FORMAT;

  static {
    DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
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
      if (removeWhiteSpace && Character.isWhitespace(ch)) {
        continue;
      }
      sb.append(ch);
    }
    return sb.toString();
  }

  /**
   * Parses string to int.
   * Example: {@code "   23,532  "}
   *
   * @throws InnerParseException if can't parse the string
   */
  public static int parseInt(String str) throws InnerParseException {
    if (str == null) throw new InnerParseException("Can't parse int from null");
    str = cleanString(str, IMPURITIES_INT, true);
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      throw new InnerParseException("Can't parse int from: " + str, e);
    }
  }

  /**
   * Parses string to int. Return {@code defaultValue} if can't parse the string.
   */
  public static int parseInt(String str, int defaultValue) {
    try {
      return parseInt(str);
    } catch (InnerParseException e) {
      return defaultValue;
    }
  }

  /**
   * Parses string to time stamp.
   * Example: {@code "2017-01-30 05:19"}
   *
   * @throws InnerParseException if can't parse the string
   */
  public static long parseDate(String str) throws InnerParseException {
    if (str == null) throw new InnerParseException("Can't parse date from null");
    str = StringUtils.strip(str);
    try {
      return DATE_FORMAT.parse(str).getTime();
    } catch (ParseException e) {
      throw new InnerParseException("Can't parse int from: " + str, e);
    }
  }

  /**
   * Parses string to time stamp. Return {@code defaultValue} if can't parse the string.
   */
  public static long parseDate(String str, long defaultValue) {
    try {
      return parseDate(str);
    } catch (InnerParseException e) {
      return defaultValue;
    }
  }

  /**
   * Converts relative url to absolute url.
   */
  public static String completeUrl(String base, String url) {
    if (url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    } else {
      return StringUtils.join(base, url, '/');
    }
  }
}
