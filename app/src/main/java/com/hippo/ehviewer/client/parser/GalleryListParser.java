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

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.url.GalleryDetailUrlParser;
import com.hippo.yorozuya.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class GalleryListParser {
  private GalleryListParser() {}

  private static final String EMPTY_KEYWORD = "No hits found</p>";

  private static final Pattern PATTERN_COVER_SIZE = Pattern.compile("height:(\\d+)px; width:(\\d+)px");
  private static final Pattern PATTERN_PX = Pattern.compile("(\\d+)px");

  private static final ItemCreator<GalleryInfo> GALLERY_INFO_CREATOR = (info, e) -> info;

  /**
   * Parses gallery list page to a gallery info list.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static List<GalleryInfo> parseGalleryList(@NonNull String body,
      @NonNull Document document) throws ParseException {
    return parseGalleryList(body, document, GALLERY_INFO_CREATOR);
  }

  /**
   * Creates the actual item.
   */
  interface ItemCreator<T> {

    /**
     * Creates a item from the {@link GalleryInfo} and the {@link Element}.
     */
    @Nullable
    T create(@NonNull GalleryInfo info, @NonNull Element e);
  }

  /**
   * Parses gallery list page to a list.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  static <T> List<T> parseGalleryList(@NonNull String body, @NonNull Document document,
      @NonNull ItemCreator<T> creator) throws ParseException {
    Element itg = document.getElementsByClass("itg").first();
    if (itg != null) {
      Element tbody = itg.children().first();
      if (tbody != null) {
        List<T> list = new ArrayList<>();

        boolean isHeader = true;
        for (Element e : tbody.children()) {
          // First one is table header, skip it
          if (isHeader) {
            isHeader = false;
            continue;
          }

          T t = parseItem(e, creator);
          if (t != null) {
            list.add(t);
          }
        }

        if (!list.isEmpty()) {
          return list;
        }
      }
    }

    throw new ParseException("Can't parse gallery list", body);
  }

  @Nullable
  private static <T> T parseItem(Element e, ItemCreator<T> creator) {
    // gid, token, title (required)
    Element it5 = e.getElementsByClass("it5").first();
    if (it5 == null) return null;
    Element a = it5.children().first();
    if (a == null) return null;
    GalleryDetailUrlParser.Result result = GalleryDetailUrlParser.parser(a.attr("href"));
    if (result == null) return null;
    String title = ParserUtils.unescape(a.text());
    if (StringUtils.isEmpty(title)) return null;

    GalleryInfo info = new GalleryInfo();
    info.gid = result.gid;
    info.token = result.token;
    // TODO It might be jpn title
    info.title = title;
    info.language = EhUtils.guessLang(title);

    // Category
    Element ic = e.getElementsByClass("ic").first();
    if (ic != null) {
      info.category = EhUtils.getCategory(ParserUtils.unescape(ic.attr("alt")));
    }

    // Date
    Element itd = e.getElementsByClass("itd").first();
    if (itd != null) {
      info.date = ParserUtils.parseDate(itd.text(), 0);
    }

    // Cover
    Element it2 = e.getElementsByClass("it2").first();
    if (it2 != null) {
      // Cover ratio
      Matcher m = PATTERN_COVER_SIZE.matcher(it2.attr("style"));
      if (m.find()) {
        int width = ParserUtils.parseInt(m.group(2), 0);
        int height = ParserUtils.parseInt(m.group(1), 0);
        if (width >= 0 && height >= 0) {
          info.coverRatio = (float) width / (float) height;
        }
      }
      // Cover url
      Elements es = it2.children();
      if (!es.isEmpty()) {
        info.coverUrl = ParserUtils.unescape(es.get(0).attr("src"));
      } else {
        info.coverUrl = ParserUtils.unescape(parseCoverUrl(it2.text()));
      }
      // Cover
      if (info.coverUrl != null) {
        info.cover = EhUrl.getImageFingerprint(info.coverUrl);
      }
    }

    Element it3 = e.getElementsByClass("it3").first();
    if (it3 != null) {
      String favId = "favicon_" + info.gid;
      for (Element child: it3.children()) {
        // Favourite slot
        if (favId.equals(child.id())) {
          info.favouriteSlot = parseFavouriteSlot(child.attr("style"));
        }

        Elements children = child.children();

        // invalid
        if (children.size() >= 1 && "E".equals(children.get(0).attr("alt"))) {
          info.invalid = true;
        }
      }
    }

    // Rating
    Element it4r = e.getElementsByClass("it4r").first();
    if (it4r != null) {
      info.rating = parseRating(it4r.attr("style"));
    }

    // Uploader
    Element itu = e.getElementsByClass("itu").first();
    if (itu != null) {
      info.uploader = ParserUtils.unescape(itu.text());
    }

    return creator.create(info, e);
  }

  // Parses url from a string like:
  // inits~exhentai.org~t/53/a8/53a82a8deec79d3824ab413b4cf784d6df8589b2-1264635-1050-1540-png_l.jpg~(C91) [ガンバリマシン (Shino)] Pさん、今日も頑張ってくれませんか？ (アイドルマスター シンデレラガールズ)
  private static String parseCoverUrl(String str) {
    int index1 = str.indexOf('~');
    if (index1 == -1) return null;
    int index2 = str.indexOf('~', index1 + 1);
    if (index2 == -1) return null;
    int index3 = str.indexOf('~', index2 + 1);
    if (index3 == -1) return null;
    return "https://" + str.substring(index1 + 1, index2) + "/" + str.substring(index2 + 1, index3);
  }

  /**
   * Parse the style string to get favourite slot.
   * <p>
   * Ehentai website uses a picture to show favourite slot.
   * Looks like: <pre>{@code
   * ------------------
   * ◇
   * ◇
   * ◇
   * ◇
   * ◇
   * ◇
   * ◇
   * ◇
   * ◇
   * ◇
   *
   * ------------------
   * }</pre>
   * The style looks like:
   * {@code background-position:0px -2px}.
   * The first one is x offset, always 0.
   * The second one is y offset, starts from -2, step -19.
   */
  public static int parseFavouriteSlot(String style) {
    Matcher m = PATTERN_PX.matcher(style);
    // Move to the second one
    if (!m.find() || !m.find()) return EhUtils.FAV_CAT_UNKNOWN;

    int num = ParserUtils.parseInt(m.group(1), 0);
    if (num == 0) return EhUtils.FAV_CAT_UNKNOWN;

    int slot = (num - 2) / 19;
    if (slot >= 0 && slot <= 9) {
      return slot;
    } else {
      return EhUtils.FAV_CAT_UNKNOWN;
    }
  }

  // ehentai website uses a picture to show rating.
  // Looks like:
  // ------------------
  // ⛤ ⛤ ⛤ ⛤ ⛤
  // ⛤ ⛤ ⛤ ⛤ half⛤
  // ------------------
  // The style looks like:
  // background-position:-16px -1px
  // The first one is x offset, 16px for a star.
  // The second one is y offset, row1 is -1px, row2 is -21px
  public static float parseRating(String style) {
    Matcher m = PATTERN_PX.matcher(style);
    int num1;
    int num2;
    float rate = 5.0f;
    if (m.find()) {
      num1 = ParserUtils.parseInt(m.group(1), -1);
    } else {
      return 0.0f;
    }
    if (m.find()) {
      num2 = ParserUtils.parseInt(m.group(1),  -1);
    } else {
      return 0.0f;
    }
    if (num1 == -1 || num2 == -1) {
      return 0.0f;
    }
    rate = rate - (num1 / 16);
    if (num2 == 21) {
      rate -= 0.5f;
    }
    return rate;
  }

  /**
   * Parses gallery list page to pages.
   *
   * @throws ParseException if can't parse it
   */
  @IntRange(from = 0)
  public static int parsePages(@NonNull String body,
      @NonNull Document document) throws ParseException {
    int pages = 0;
    Element ptt = document.getElementsByClass("ptt").first();
    if (ptt != null) {
      Element tbody = ptt.children().first();
      if (tbody != null) {
        Element tr = tbody.children().first();
        if (tr != null) {
          Elements tds = tr.children();
          if (tds.size() >= 2) {
            pages = ParserUtils.parseInt(tds.get(tds.size() - 2).text(), 0);
          }
        }
      }
    }

    if (pages == 0 && !body.contains(EMPTY_KEYWORD)) {
      throw new ParseException("Can't get pages", body);
    }

    return pages;
  }
}
