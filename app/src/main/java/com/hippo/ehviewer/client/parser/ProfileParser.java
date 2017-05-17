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
import android.util.Pair;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.yorozuya.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class ProfileParser {
  private ProfileParser() {}

  /**
   * Same as {@code parseProfile(body, Jsoup.parse(body))}.
   */
  @NonNull
  public static Pair<String, String> parseProfile(@NonNull String body) throws ParseException {
    return parseProfile(body, Jsoup.parse(body));
  }

  /**
   * Parses profile page to a pair of name and avatar.
   * The {@code document} must be from the {@code body}.
   * <p>
   * The name can't be {@code null}, the avatar could be {@code null}.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static Pair<String, String> parseProfile(@NonNull String body,
      @NonNull Document document) throws ParseException {
    String name = null;
    String avatar = null;

    Element profileName = document.getElementById("profilename");
    if (profileName != null) {
      name = ParserUtils.unescape(profileName.text());

      Element element = profileName.nextElementSibling();
      if (element != null) {
        element = element.nextElementSibling();
        if (element != null) {
          element = element.children().first();
          if (element != null) {
            avatar = element.attr("src");
            avatar = ParserUtils.unescape(avatar);
            avatar = ParserUtils.completeUrl(EhUrl.URL_FORUMS, avatar);
          }
        }
      }
    }

    if (StringUtils.isEmpty(name)) {
      throw new ParseException("Can't parse profile page", body);
    }

    return new Pair<>(name, avatar);
  }
}
