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
import android.util.Pair;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.data.FavouritesState;
import com.hippo.ehviewer.client.exception.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class FavouritesParser {
  private FavouritesParser() {}

  private static final Pattern PATTERN_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}");

  private static final GalleryListParser.ItemCreator<FavouritesItem> FAVOURITES_ITEM_CREATOR =
      (info, e) -> {
        FavouritesItem item = new FavouritesItem();
        item.info = info;

        // Get favorite note
        Element favNote = e.getElementById("favnote_" + info.gid);
        if (favNote != null) {
          String note = favNote.text();
          if (note.startsWith("Note: ")) {
            note = note.substring("Note: ".length());
          }
          item.note = note;
        }

        // Get favorite date
        Matcher matcher = PATTERN_DATE.matcher(e.html());
        if (matcher.find()) {
          item.date = ParserUtils.parseDate(matcher.group(), 0);
        }

        return item;
      };

  /**
   * Parses favourites page to a favourites item list.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static List<FavouritesItem> parseFavourites(@NonNull String body,
      @NonNull Document document) throws ParseException {
    return GalleryListParser.parseGalleryList(body, document, FAVOURITES_ITEM_CREATOR);
  }

  /**
   * Parses favourites page to a favourites state.
   *
   * @throws ParseException if can't parse it
   */
  @NonNull
  public static FavouritesState parseFavouritesState(@NonNull String body,
      @NonNull Document document) throws ParseException {
    Element ido = document.getElementsByClass("ido").first();
    if (ido != null) {
      List<Pair<String, Integer>> list = new ArrayList<>();

      for (Element fp : ido.getElementsByClass("fp")) {
        Elements children = fp.children();
        if (children.size() < 3) continue;

        String name = ParserUtils.unescape(children.get(2).text());
        int count = ParserUtils.parseInt(children.get(0).text(), -1);
        if (count == -1) continue;

        list.add(new Pair<>(name, count));
      }

      if (!list.isEmpty()) {
        return new FavouritesState(list);
      }
    }

    throw new ParseException("Can't get favourites state", body);
  }

  /**
   * Parses favourites page to pages.
   *
   * @throws ParseException if can't parse it
   */
  @IntRange(from = 0)
  public static int parsePages(@NonNull String body,
      @NonNull Document document) throws ParseException {
    return GalleryListParser.parsePages(body, document);
  }
}
