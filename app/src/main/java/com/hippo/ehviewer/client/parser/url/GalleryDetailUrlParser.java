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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryDetailUrlParser {

  public static final Pattern URL_PATTERN = Pattern.compile("/g/(\\d+)/(\\w+)");

  public static class Result {
    public final long gid;
    public final String token;

    public Result(long gid, String token) {
      this.gid = gid;
      this.token = token;
    }
  }

  // https://exhentai.org/g/1060346/03702a68a5/
  /**
   * Parses a gallery detail url. The url looks like:
   * {@code https://exhentai.org/g/1060346/03702a68a5/}
   * Returns {@code null} if can't parse it.
   */
  @Nullable
  public static Result parser(@NonNull String url) {
    Matcher matcher = URL_PATTERN.matcher(url);
    if (matcher.find()) {
      long gid = Long.parseLong(matcher.group(1));
      String token = matcher.group(2);
      return new Result(gid, token);
    }
    return null;
  }
}
