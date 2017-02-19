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
 * Created by Hippo on 2/4/2017.
 */

import static org.junit.Assert.assertEquals;

import android.os.Build;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.GalleryListResult;
import java.io.IOException;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class GalleryListConverterTest {

  @Before
  public void setUp() throws Exception {
    ShadowLog.stream = System.out;
  }

  @Test
  public void testCovertEHentai() throws IOException, ParseException {
    GalleryListConverter converter = new GalleryListConverter();
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_e-hentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    GalleryListResult result = converter.convert(bufferedSource.readUtf8());
    assertEquals(25, result.galleryInfoList().size());
  }

  @Test
  public void testCovertEXHentai() throws IOException, ParseException {
    GalleryListConverter converter = new GalleryListConverter();
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_exhentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    GalleryListResult result = converter.convert(bufferedSource.readUtf8());
    assertEquals(25, result.galleryInfoList().size());
  }

  @Test
  public void testCovertEHentai2() throws IOException, ParseException {
    GalleryListConverter converter = new GalleryListConverter();
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_e-hentai_2.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    GalleryListResult result = converter.convert(bufferedSource.readUtf8());
    assertEquals(25, result.galleryInfoList().size());
  }

  @Test
  public void testCovertFavouriteEHentai() throws IOException, ParseException {
    GalleryListConverter converter = new GalleryListConverter();
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("gallery_list_favourite_e-hentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    GalleryListResult result = converter.convert(bufferedSource.readUtf8());
    assertEquals(6, result.galleryInfoList().get(0).favouriteSlot);
  }
}
