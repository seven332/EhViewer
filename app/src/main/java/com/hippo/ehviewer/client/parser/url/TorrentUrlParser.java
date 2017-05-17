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

package com.hippo.ehviewer.client.parser.url;

/*
 * Created by Hippo on 5/16/2017.
 */

import android.net.UrlQuerySanitizer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.yorozuya.NumberUtils;

public class TorrentUrlParser {

  public static class Result {
    public final long gid;
    public final String token;

    public Result(long gid, String token) {
      this.gid = gid;
      this.token = token;
    }
  }

  // https://exhentai.org/gallerytorrents.php?gid=1063451&t=6cc24065cb
  /**
   * Parses a torrent url. The url looks like:
   * {@code https://exhentai.org/gallerytorrents.php?gid=1063451&t=6cc24065cb}
   * Returns {@code null} if can't parse it.
   */
  @Nullable
  public static Result parser(@NonNull String url) {
    UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
    sanitizer.setAllowUnregisteredParamaters(true);
    sanitizer.setUnregisteredParameterValueSanitizer(UrlQuerySanitizer.getAllButNulLegal());
    sanitizer.parseUrl(url);

    long gid = NumberUtils.parseLong(sanitizer.getValue("gid"), 0);
    if (gid == 0) return null;

    String token = sanitizer.getValue("t");
    if (token == null) return null;

    return new Result(gid, token);
  }
}
