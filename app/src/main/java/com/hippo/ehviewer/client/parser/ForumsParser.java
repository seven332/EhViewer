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
 * Created by Hippo on 5/17/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.exception.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class ForumsParser {
  private ForumsParser() {}

  /**
   * Same as {@code parseForums(body, Jsoup.parse(body))}.
   */
  @NonNull
  public static String parseForums(@NonNull String body) throws ParseException {
    return parseForums(body, Jsoup.parse(body));
  }

  /**
   * Parses gallery forums page to a profile url.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static String parseForums(@NonNull String body, @NonNull Document document)
      throws ParseException {
    Element userLinks = document.getElementById("userlinks");

    Element child = userLinks.children().first();
    if (child != null) {
      child = child.children().first();
      if (child != null) {
        child = child.children().first();
        if (child != null) {
          String url = child.attr("href");
          url = ParserUtils.completeUrl(EhUrl.URL_FORUMS, url);
          url = ParserUtils.unescape(url);
          return url;
        }
      }
    }

    throw new ParseException("Can't get profile url", body);
  }
}
