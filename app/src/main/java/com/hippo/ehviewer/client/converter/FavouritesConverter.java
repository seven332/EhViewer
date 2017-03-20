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
 * Created by Hippo on 3/4/2017.
 */

import android.support.annotation.NonNull;
import android.util.Pair;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.exception.InnerParseException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.FavouritesResult;
import com.hippo.yorozuya.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A {@link retrofit2.Converter} to parse the html of favourites.
 */
public class FavouritesConverter extends EhConverter<FavouritesResult> {

  private static final int FAVOURITES_SLOT_COUNT = 10;
  private static final GalleryListConverter.ItemCreator<FavouritesItem> CREATOR = (info, e) -> {
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
    return item;
  };

  @NonNull
  @Override
  public FavouritesResult convert(String body) throws ParseException {
    List<Pair<String, Integer>> state;
    int pages;
    List<FavouritesItem> list;

    Document d = Jsoup.parse(body);

    // Parse favourites state
    try {
      state = parseState(d);
    } catch (InnerParseException e) {
      throw new ParseException("Can't parse favourites state", body, e);
    }

    // Parse pages
    try {
      pages = GalleryListConverter.parsePages(d);
      if (pages <= 0) {
        throw new ParseException("Bad pages when parse gallery list: " + pages, body);
      }
    } catch (InnerParseException e) {
      if (GalleryListConverter.isEmpty(body)) {
        return new FavouritesResult(0, Collections.emptyList(), state);
      } else {
        throw new ParseException("Can't parse gallery list", body, e);
      }
    }

    // Parses gallery list
    try {
      list = GalleryListConverter.parseItemList(d, CREATOR);
    } catch (InnerParseException e) {
      throw new ParseException("Can't parse gallery list", body, e);
    }

    return new FavouritesResult(pages, list, state);
  }

  @NonNull
  static List<Pair<String, Integer>> parseState(Document d) throws InnerParseException {
    Element ido = d.getElementsByClass("ido").first();
    if (ido == null) {
      throw new InnerParseException("Can't find ido");
    }
    Elements fps = ido.getElementsByClass("fp");

    List<Pair<String, Integer>> state = new ArrayList<>(FAVOURITES_SLOT_COUNT);
    for (Element fp: fps) {
      Elements children = fp.children();
      if (children.size() < 3) {
        continue;
      }
      String name = StringUtils.strip(children.get(2).text());
      int count = ConverterUtils.parseInt(children.get(0).text(), -1);
      if (count == -1) {
        continue;
      }
      state.add(new Pair<>(name, count));
    }

    // Must get FAVOURITES_SLOT_COUNT
    if (state.size() != FAVOURITES_SLOT_COUNT) {
      throw new InnerParseException("Found " + state.size() + " favourites slot, it should be " +
          FAVOURITES_SLOT_COUNT);
    }

    return state;
  }


  @NonNull
  @Override
  public FavouritesResult error(Throwable t) {
    return FavouritesResult.error(t);
  }
}
