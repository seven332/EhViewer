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

import static org.junit.Assert.assertEquals;

import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.data.FavouritesState;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import java.io.IOException;
import java.util.List;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class FavouritesParserTest {

  @Test
  public void testStateExHentai() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("favourites_exhentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    FavouritesState state = FavouritesParser.parseFavouritesState(body, Jsoup.parse(body));

    assertEquals(10, state.getFavouritesSlotCount());
    assertEquals("你好", state.getFavouritesName(0));
    assertEquals(1, state.getFavouritesCount(0));
    assertEquals("Favorites 1", state.getFavouritesName(1));
    assertEquals(0, state.getFavouritesCount(1));
    assertEquals("heyhey", state.getFavouritesName(2));
    assertEquals(0, state.getFavouritesCount(2));
    assertEquals("Favorites 3", state.getFavouritesName(3));
    assertEquals(0, state.getFavouritesCount(3));
    assertEquals("Favorites 4", state.getFavouritesName(4));
    assertEquals(0, state.getFavouritesCount(4));
    assertEquals("Favorites 5", state.getFavouritesName(5));
    assertEquals(0, state.getFavouritesCount(5));
    assertEquals("Favorites 6", state.getFavouritesName(6));
    assertEquals(0, state.getFavouritesCount(6));
    assertEquals("Favorites 7", state.getFavouritesName(7));
    assertEquals(0, state.getFavouritesCount(7));
    assertEquals("Favorites 8", state.getFavouritesName(8));
    assertEquals(0, state.getFavouritesCount(8));
    assertEquals("Favorites 9", state.getFavouritesName(9));
    assertEquals(0, state.getFavouritesCount(9));
  }

  @Test
  public void testPagesExHentai() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("favourites_exhentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    assertEquals(1, FavouritesParser.parsePages(body, Jsoup.parse(body)));
  }

  @Test
  public void testFavouritesExHentai() throws IOException, ParseException {
    Source source = Okio
        .source(getClass().getClassLoader().getResourceAsStream("favourites_exhentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    List<FavouritesItem> list = FavouritesParser.parseFavourites(body, Jsoup.parse(body));
    assertEquals(1, list.size());

    FavouritesItem item = list.get(0);
    assertEquals("[AS109] Shoujo and the Back Alley 0.9 – 3.1 [English] {Hennojin} [Uncensored]", item.note);
    assertEquals(1488619920000L, item.date);

    GalleryInfo info = item.info;
    assertEquals(EhUtils.CATEGORY_ARTIST_CG, info.category);
    assertEquals(1488619920000L, info.date);
    assertEquals("[As109] 少女と裏路地 [英訳] [無修正]", info.title);
    assertEquals(4.5f, info.rating, 0.0f);
  }
}
