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
public class GalleryListParserTest {

  @Test
  public void testCovertEHentai() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_e-hentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    List<GalleryInfo> list = GalleryListParser.parseGalleryList(body, Jsoup.parse(body));
    assertEquals(25, list.size());
  }

  @Test
  public void testCovertEXHentai() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_exhentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    List<GalleryInfo> list = GalleryListParser.parseGalleryList(body, Jsoup.parse(body));
    assertEquals(25, list.size());
  }

  @Test
  public void testCovertEHentai2() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_e-hentai_2.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    List<GalleryInfo> list = GalleryListParser.parseGalleryList(body, Jsoup.parse(body));
    assertEquals(25, list.size());
  }

  @Test
  public void testCovertFavouriteEHentai() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_favourite_e-hentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    List<GalleryInfo> list = GalleryListParser.parseGalleryList(body, Jsoup.parse(body));
    assertEquals(6, list.get(0).favouriteSlot);
  }

  @Test
  public void testCovertFavouriteEXHentai() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_favourite_exhentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    List<GalleryInfo> list = GalleryListParser.parseGalleryList(body, Jsoup.parse(body));
    assertEquals(50, list.size());
    assertEquals(true, list.get(6).invalid);
    assertEquals(false, list.get(7).invalid);
    assertEquals(true, list.get(22).invalid);
  }
}
