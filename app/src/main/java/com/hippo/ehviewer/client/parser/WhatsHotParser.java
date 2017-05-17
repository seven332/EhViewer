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
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.url.GalleryDetailUrlParser;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class WhatsHotParser {
  private WhatsHotParser() {}

  /**
   * Same as {@code parseWhatsHot(body, Jsoup.parse(body))}.
   */
  public static List<GalleryInfo> parseWhatsHot(@NonNull String body) throws ParseException {
    return parseWhatsHot(body, Jsoup.parse(body));
  }

  /**
   * Parses whats hot page to a gallery info list.
   *
   * @throws ParseException if can't parse it
   */
  public static List<GalleryInfo> parseWhatsHot(@NonNull String body, @NonNull Document document)
      throws ParseException {
    Element pp = document.getElementById("pp");
    if (pp != null) {
      List<GalleryInfo> list = new ArrayList<>();

      for (Element id1 : pp.getElementsByClass("id1")) {
        GalleryInfo info = parseGalleryInfo(id1);
        if (info != null) {
          list.add(info);
        }
      }

      if (!list.isEmpty()) {
        return list;
      }
    }

    throw new ParseException("Can't parse whats hot", body);
  }

  private static GalleryInfo parseGalleryInfo(Element element) {
    Element id3 = element.getElementsByClass("id3").first();
    if (id3 == null) return null;
    Element a = id3.getElementsByTag("a").first();
    if (a == null) return null;
    String url = a.attr("href");
    GalleryDetailUrlParser.Result result = GalleryDetailUrlParser.parser(url);
    if (result == null) return null;

    GalleryInfo info = new GalleryInfo();
    info.gid = result.gid;
    info.token = result.token;

    Element img = a.getElementsByTag("img").first();
    if (img != null) {
      info.coverUrl = ParserUtils.unescape(img.attr("src"));
      info.cover = EhUrl.getImageFingerprint(info.coverUrl);
      // TODO Might be title jpn
      info.title = ParserUtils.unescape(img.attr("title"));
      info.language = EhUtils.guessLang(info.title);
    }

    Element id41 = element.getElementsByClass("id41").first();
    if (id41 != null) {
      info.category = EhUtils.getCategory(ParserUtils.unescape(id41.attr("title")));
    }

    Element id42 = element.getElementsByClass("id42").first();
    if (id42 != null) {
      String text = id42.text();
      int index = text.indexOf(' ');
      if (index >= 0) {
        info.pages = ParserUtils.parseInt(text.substring(0, index), 0);
      }
    }

    Element id43 = element.getElementsByClass("id43").first();
    if (id43 != null) {
      info.rating = GalleryListParser.parseRating(id43.attr("style"));
    }

    return info;
  }
}
