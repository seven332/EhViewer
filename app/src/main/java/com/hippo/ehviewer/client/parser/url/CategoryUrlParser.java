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
import com.hippo.ehviewer.client.EhUtils;

public class CategoryUrlParser {

  // https://exhentai.org/doujinshi
  /**
   * Parses a category url. The url looks like:
   * {@code https://exhentai.org/doujinshi}
   * Returns {@link EhUtils#CATEGORY_UNKNOWN} if can't parse it.
   */
  public static int parser(@NonNull String str) {
    if (str.charAt(str.length() - 1) == '/') {
      str = str.substring(0, str.length() - 1);
    }
    int index = str.lastIndexOf("/");
    if (index >= 0) {
      return EhUtils.getCategory(str.substring(index + 1));
    } else {
      return EhUtils.CATEGORY_UNKNOWN;
    }
  }
}
