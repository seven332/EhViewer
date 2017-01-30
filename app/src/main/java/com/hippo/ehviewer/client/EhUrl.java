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
 * Created by Hippo on 1/25/2017.
 */

import android.support.annotation.IntDef;
import android.util.Pair;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EhUrl {
  private EhUrl() {}

  private static final Pattern PATTERN_GID_TOKEN = Pattern.compile("/g/(\\d+)/([0-9a-f]{10})");
  private static final Pattern PATTERN_FINGERPRINT = Pattern.compile("[0-9a-f]{40}-\\d+-\\d+-\\d+-[0-9a-z]+");

  @IntDef({SITE_E, SITE_EX})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Site {}

  public static final int SITE_E = 0;
  public static final int SITE_EX = 1;

  public static final String DOMAIN_E = "e-hentai.org";
  public static final String DOMAIN_EX = "exhentai.org";

  public static final String URL_E = "https://e-hentai.org/";
  public static final String URL_EX = "https://exhentai.org/";


  /**
   * Return site url which ends with "/"
   */
  public static String getSiteUrl(@Site int site) {
    if (site == SITE_EX) {
      return URL_EX;
    } else {
      return URL_E;
    }
  }




  /**
   * Parse gallery url to gid and token.
   * {@code null} if can't parse it.
   */
  public static Pair<Long, String> getGidToken(String url) {
    if (url == null) return null;
    Matcher m = PATTERN_GID_TOKEN.matcher(url);
    if (m.find()) {
      long gid;
      try {
        gid = Long.parseLong(m.group(1));
      } catch (NumberFormatException e) {
        return null;
      }
      String token = m.group(2);
      return new Pair<>(gid, token);
    } else {
      return null;
    }
  }

  /**
   * Parse image url to fingerprint.
   * {@code null} if can't parse it.
   */
  public static String getFingerprint(String url) {
    if (url == null) return null;
    Matcher m = PATTERN_FINGERPRINT.matcher(url);
    if (m.find()) {
      return m.group(0);
    } else {
      return null;
    }
  }
}
