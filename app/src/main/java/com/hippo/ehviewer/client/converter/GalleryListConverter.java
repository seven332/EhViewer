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
 * Created by Hippo on 1/29/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.InnerParseException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.GalleryListResult;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A {@link retrofit2.Converter} to parse the html of gallery list.
 */
public class GalleryListConverter extends EhConverter<GalleryListResult> {

  private static final String LOG_TAG = GalleryListConverter.class.getSimpleName();
  private static final String EMPTY_KEYWORD = "No hits found</p>";
  private static final Pattern PATTERN_COVER_SIZE = Pattern.compile("height:(\\d+)px; width:(\\d+)px");
  private static final Pattern PATTERN_PX = Pattern.compile("(\\d+)px");
  private static final ItemCreator<GalleryInfo> CREATOR = (info, e) -> info;

  @NonNull
  @Override
  public GalleryListResult convert(String body) throws ParseException {
    int pages;
    List<GalleryInfo> list;

    Document d = Jsoup.parse(body);

    // Parses pages
    try {
      pages = parsePages(d);
      if (pages <= 0) {
        throw new ParseException("Bad pages when parse gallery list: " + pages, body);
      }
    } catch (InnerParseException e) {
      if (body.contains(EMPTY_KEYWORD)) {
        return new GalleryListResult(0, Collections.emptyList());
      } else {
        throw new ParseException("Can't parse pages", body, e);
      }
    }

    // Parses gallery list
    try {
      list = parseItemList(d, CREATOR);
    } catch (InnerParseException e) {
      throw new ParseException("Can't parse gallery list", body, e);
    }

    return new GalleryListResult(pages, list);
  }

  static int parsePages(Document d) throws InnerParseException {
    try {
      Elements es = d.getElementsByClass("ptt").first().child(0).child(0).children();
      Element e = es.get(es.size() - 2);
      return ConverterUtils.parseInt(e.text());
    } catch (Exception e) {
      throw new InnerParseException("Can't parse gallery list pages", e);
    }
  }

  static boolean isEmpty(String body) {
    return body.contains(EMPTY_KEYWORD);
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
   * Parses item list which base on {@link GalleryInfo}.
   */
  @NonNull
  static <T> List<T> parseItemList(Document d, ItemCreator<T> creator) throws InnerParseException {
    Element itg = d.getElementsByClass("itg").first();
    if (itg == null) {
      throw new InnerParseException("Can't find itg");
    }
    Elements children = itg.children();
    if (children.size() < 1) {
      throw new InnerParseException("Can't find children of itg");
    }
    Elements es = children.get(0).children();
    if (es.size() < 1) {
      throw new InnerParseException("Can't find children of the first child of itg");
    }
    List<T> list = new ArrayList<>(es.size() - 1);
    // First one is table header, skip it
    for (int i = 1; i < es.size(); i++) {
      T t = parseItem(es.get(i), creator);
      if (t != null) {
        list.add(t);
      } else {
        Log.w(LOG_TAG, "Can't parse item");
      }
    }
    if (list.isEmpty()) {
      throw new InnerParseException("Can't find any gallery");
    }
    return list;
  }

  static <T> T parseItem(Element e, ItemCreator<T> creator) {
    GalleryInfo gi = new GalleryInfo();

    // Category
    Element ic = e.getElementsByClass("ic").first();
    if (ic != null) {
      gi.category = EhUtils.getCategory(StringUtils.strip(ic.attr("alt")));
    } else {
      // It's OK to skip category
      Log.w(LOG_TAG, "Can't parse gallery category");
    }

    // Date
    Element itd = e.getElementsByClass("itd").first();
    if (itd != null) {
      gi.date = ConverterUtils.parseDate(itd.text(), 0);
    } else {
      // It's OK to skip date
      Log.w(LOG_TAG, "Can't parse gallery date");
    }

    // Cover
    Element it2 = e.getElementsByClass("it2").first();
    if (it2 != null) {
      // Cover ratio
      Matcher m = PATTERN_COVER_SIZE.matcher(it2.attr("style"));
      if (m.find()) {
        int width = ConverterUtils.parseInt(m.group(2), 0);
        int height = ConverterUtils.parseInt(m.group(1), 0);
        if (width >= 0 && height >= 0) {
          gi.coverRatio = (float) width / (float) height;
        }
      }
      // Cover url
      Elements es = it2.children();
      if (!es.isEmpty()) {
        gi.coverUrl = ConverterUtils.unescapeXml(es.get(0).attr("src"));
      } else {
        gi.coverUrl = ConverterUtils.unescapeXml(parseCoverUrl(it2.text()));
      }
      // Cover
      if (gi.coverUrl != null) {
        gi.cover = EhUrl.getImageFingerprint(gi.coverUrl);
      }
    }
    if (gi.coverRatio == -1.0f) {
      // It's OK to skip cover ratio
      Log.w(LOG_TAG, "Can't parse gallery cover ratio");
    }
    if (gi.coverUrl == null) {
      // It's OK to skip cover url
      Log.w(LOG_TAG, "Can't parse gallery cover url");
    }
    if (gi.cover == null) {
      // It's OK to skip cover
      Log.w(LOG_TAG, "Can't parse gallery cover");
    }

    // gid, token, title (required)
    Element it5 = e.getElementsByClass("it5").first();
    if (it5 == null) {
      Log.e(LOG_TAG, "Can't parse gallery, no it5. Element: \n" + e.outerHtml());
      return null;
    }
    Elements es = it5.children();
    if (es.size() <= 0) {
      Log.e(LOG_TAG, "Can't parse gallery, it5 no child. Element: \n" + e.outerHtml());
      return null;
    }
    Element a = es.get(0);
    Pair<Long, String> pair = EhUrl.getGidToken(a.attr("href"));
    if (pair == null) {
      Log.e(LOG_TAG, "Can't parse gallery gid and token. Element: \n" + e.outerHtml());
      return null;
    }
    gi.gid = pair.first;
    gi.token = pair.second;
    gi.title = ConverterUtils.unescapeXml(a.text());
    gi.language = EhUtils.guessLang(gi.title);

    Element it3 = e.getElementsByClass("it3").first();
    if (it3 != null) {
      String favId = "favicon_" + gi.gid;
      for (Element child: it3.children()) {
        // Favourite slot
        if (favId.equals(child.id())) {
          gi.favouriteSlot = parseFavouriteSlot(child.attr("style"));
        }

        Elements children = child.children();

        // invalid
        if (children.size() >= 1 && "E".equals(children.get(0).attr("alt"))) {
          gi.invalid = true;
        }
      }
    }

    // TODO Check local favourites
    // TODO Check downloaded

    // Rating
    Element it4r = e.getElementsByClass("it4r").first();
    if (it4r != null) {
      gi.rating = parseRating(it4r.attr("style"));
    }

    // Uploader
    Element itu = e.getElementsByClass("itu").first();
    if (itu != null) {
      gi.uploader = StringUtils.strip(itu.text());
    } else {
      // It's OK to skip uploader
      Log.w(LOG_TAG, "Can't parse gallery uploader");
    }

    return creator.create(gi, e);
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
  private static float parseRating(String style) {
    Matcher m = PATTERN_PX.matcher(style);
    int num1;
    int num2;
    float rate = 5.0f;
    if (m.find()) {
      num1 = ConverterUtils.parseInt(m.group(1), -1);
    } else {
      return 0.0f;
    }
    if (m.find()) {
      num2 = ConverterUtils.parseInt(m.group(1),  -1);
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

  // ehentai website uses a picture to show favourite slot.
  // Looks like:
  // ------------------
  // ◇
  // ◇
  // ◇
  // ◇
  // ◇
  // ◇
  // ◇
  // ◇
  // ◇
  // ◇
  //
  // ------------------
  // The style looks like:
  // background-position:0px -2px
  // The first one is x offset, always 0
  // The second one is y offset, starts from -2, step -19
  private static int parseFavouriteSlot(String style) {
    Matcher m = PATTERN_PX.matcher(style);
    if (!m.find() || !m.find()) {
      return -1;
    }
    int num = ConverterUtils.parseInt(m.group(1), -1);
    if (num == -1) {
      return -1;
    }
    int slot = (num - 2) / 19;
    slot = MathUtils.clamp(slot, 0, 9);
    return slot;
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public GalleryListResult error(Throwable t) {
    return GalleryListResult.error(t);
  }
}
