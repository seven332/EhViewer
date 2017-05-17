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
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.data.FavouritesState;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.FavouritesParser;
import com.hippo.ehviewer.client.result.FavouritesResult;
import java.util.Collections;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * A {@link retrofit2.Converter} to parse the html of favourites.
 */
public class FavouritesConverter extends EhConverter<FavouritesResult> {

  @NonNull
  @Override
  public FavouritesResult convert(String body) throws ParseException {
    Document document = Jsoup.parse(body);

    FavouritesState state = FavouritesParser.parseFavouritesState(body, document);

    int pages = FavouritesParser.parsePages(body, document);

    List<FavouritesItem> favouritesItemList;
    if (pages > 0) {
      favouritesItemList = FavouritesParser.parseFavourites(body, document);
    } else {
      favouritesItemList = Collections.emptyList();
    }

    return new FavouritesResult(pages, favouritesItemList, state);
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public FavouritesResult error(Throwable t) {
    return FavouritesResult.error(t);
  }
}
