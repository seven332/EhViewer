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
import android.support.annotation.StringDef;
import android.util.Pair;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EhUrl {
  private EhUrl() {}

  private static final Pattern PATTERN_GID_TOKEN = Pattern.compile("/g/(\\d+)/([0-9a-f]{10})");
  // A thumbnail url looks like:
  // https://exhentai.org/t/e8/50/e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg_l.jpg
  private static final Pattern PATTERN_THUMBNAIL =
      Pattern.compile("/([0-9a-f]{2})/([0-9a-f]{2})/([0-9a-f]{40}-\\d+-\\d+-\\d+-[0-9a-z]+)_([0-9a-zA-Z]+)\\.[0-9a-zA-Z]+");
  private static final Pattern PATTERN_IMAGE_FINGERPRINT =
      Pattern.compile("[0-9a-f]{40}-\\d+-\\d+-\\d+-[0-9a-z]+");

  @IntDef({SITE_E, SITE_EX})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Site {}

  public static final int SITE_E = 0;
  public static final int SITE_EX = 1;

  public static final String DOMAIN_E = "e-hentai.org";
  public static final String DOMAIN_EX = "exhentai.org";

  public static final String URL_E = "https://e-hentai.org/";
  public static final String URL_EX = "https://exhentai.org/";

  public static final String URL_THUMBNAIL_E = "https://ul.ehgt.org/";
  public static final String URL_THUMBNAIL_EX = "https://exhentai.org/t/";

  public static final String URL_FORUMS = "https://forums.e-hentai.org/";
  public static final String URL_SIGN_IN = URL_FORUMS + "index.php?act=Login&CODE=01";

  @StringDef({THUMBNAIL_TYPE_L, THUMBNAIL_TYPE_250, THUMBNAIL_TYPE_300})
  @Retention(RetentionPolicy.SOURCE)
  public @interface ThumbnailType {}

  /**
   * Width == 200
   **/
  public static final String THUMBNAIL_TYPE_L = "l";
  /**
   * Width == 250, only available for cover
   */
  public static final String THUMBNAIL_TYPE_250 = "250";
  /**
   * Width == 300, only available for cover
   */
  public static final String THUMBNAIL_TYPE_300 = "300";

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
   * <p>
   * Example:
   * <ul>
   * <li>Url: {@code https://exhentai.org/t/e8/50/e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg_l.jpg}</li>
   * <li>Image fingerprint: {@code e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg}</li>
   * </ul>
   */
  public static String getImageFingerprint(String url) {
    if (url == null) return null;
    Matcher m = PATTERN_THUMBNAIL.matcher(url);
    if (m.find()) {
      String g1 = m.group(1);
      String g2 = m.group(2);
      String g3 = m.group(3);
      if (g1.equals(g3.substring(0, 2)) && g2.equals(g3.substring(2, 4))) {
        return g3;
      }
    }
    return null;
  }

  /**
   * Parse image url to fingerprint.
   * {@code null} if can't parse it.
   * <p>
   * Example:
   * <ul>
   * <li>Url: {@code https://exhentai.org/t/e8/50/e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg_l.jpg}</li>
   * <li>Image fingerprint: {@code e850fc3d8fb7bfea934f66206fb8db8a86f57251-28396-1074-1538-jpg_l}</li>
   * </ul>
   */
  public static String getThumbnailFingerprint(String url) {
    if (url == null) return null;
    Matcher m = PATTERN_THUMBNAIL.matcher(url);
    if (m.find()) {
      String g1 = m.group(1);
      String g2 = m.group(2);
      String g3 = m.group(3);
      String g4 = m.group(4);
      if (g1.equals(g3.substring(0, 2)) && g2.equals(g3.substring(2, 4))) {
        return g3 + "_" + g4;
      }
    }
    return null;
  }

  /**
   * Gets thumbnail url.
   * The {@code fingerprint} is not checked, but
   * {@code null} is returned if the {@code fingerprint}
   * doesn't support the method.
   */
  public static String getThumbnailUrl(String fingerprint,
      @Site int site, @ThumbnailType String type) {
    if (fingerprint == null || fingerprint.length() < 4) return null;
    String url;
    if (site == SITE_EX) {
      url = URL_THUMBNAIL_EX;
    } else {
      url = URL_THUMBNAIL_E;
    }
    return url + fingerprint.substring(0, 2) + "/" + fingerprint.substring(2, 4)
        + "/" + fingerprint + "_" + type + ".jpg";
  }
}
