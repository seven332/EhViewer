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
import com.hippo.ehviewer.client.exception.ParseException;
import java.io.IOException;
import java.util.List;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class WhatsHotParserTest {

  @Test
  public void test() throws IOException, ParseException {
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_e-hentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    String body = bufferedSource.readUtf8();

    List<GalleryInfo> list = WhatsHotParser.parseWhatsHot(body);
    assertEquals(15, list.size());

    GalleryInfo info = list.get(0);
    assertEquals("(C91) [Rocket Chousashitsu (Koza)] Momiji-chan to Goshujin-sama (Touhou Project)", info.title);
    assertEquals("a5fcad56f10d1adec7de465df1bc13bd0044d46d-1843459-2112-3000-jpg", info.cover);
    assertEquals("https://ehgt.org/t/a5/fc/a5fcad56f10d1adec7de465df1bc13bd0044d46d-1843459-2112-3000-jpg_l.jpg", info.coverUrl);
    assertEquals(EhUtils.CATEGORY_DOUJINSHI, info.category);
    assertEquals(24, info.pages);
    assertEquals(4.5f, info.rating, 0.0f);
  }
}
