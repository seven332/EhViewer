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
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.TagSet;
import com.hippo.ehviewer.client.exception.ParseException;
import java.io.IOException;
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
public class GalleryDetailParserTest {

  @Test
  public void testEHentai() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_detail_exhentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    GalleryInfo info = GalleryDetailParser.parseGalleryDetail(body, Jsoup.parse(body));

    assertEquals(1063607L, info.gid);
    assertEquals("523cc07306", info.token);
    assertEquals("[Kura-Shiki (Wotoe)] Long Way Home, Take Me Home (Rance IX) [Digital]", info.title);
    assertEquals("[蔵式 (をとゑ)] Long Way Home, Take Me Home (Rance IX) [DL版]", info.titleJpn);
    assertEquals("14c0b978b63e9cc77eefa9edcf043202dd5f8b54-1277374-1062-1500-jpg", info.cover);
    assertEquals("https://exhentai.org/t/14/c0/14c0b978b63e9cc77eefa9edcf043202dd5f8b54-1277374-1062-1500-jpg_250.jpg", info.coverUrl);
    assertEquals(250.0f / 354.0f, info.coverRatio, 0.0f);
    assertEquals(EhUtils.CATEGORY_NON_H, info.category);
    assertEquals(1494901680000L, info.date);
    assertEquals("Kyou_kun", info.uploader);
    assertEquals(4.37f, info.rating, 0.0f);
    assertEquals(17, info.rated);
    assertEquals(EhUtils.LANG_JA, info.language);
    assertEquals(12, info.favourited);
    assertEquals(EhUtils.FAV_CAT_UNKNOWN, info.favouriteSlot);
    assertEquals(false, info.invalid);
    assertEquals("415262--d0e650c90104a3f10ea4f6d83a57a0d49fd99d0d", info.archiverKey);
    assertEquals(46, info.pages);
    assertEquals(33418118, info.size);
    assertEquals(1, info.torrentCount);
    TagSet tagSet = new TagSet();
    tagSet.add("parody", "rance");
    tagSet.add("artist", "wotoe");
    tagSet.add("misc", "kura-shiki");
    assertEquals(tagSet, info.tagSet);
    assertEquals(0, info.parentGid);
    assertEquals(null, info.parentToken);
    assertEquals(0, info.childGid);
    assertEquals(null, info.childToken);
  }
}
