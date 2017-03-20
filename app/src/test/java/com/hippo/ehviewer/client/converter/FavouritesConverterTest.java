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
 * Created by Hippo on 3/20/2017.
 */

import static org.junit.Assert.assertEquals;

import android.os.Build;
import android.util.Pair;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.FavouritesResult;
import java.io.IOException;
import java.util.List;
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
public class FavouritesConverterTest {

  @Before
  public void setUp() throws Exception {
    ShadowLog.stream = System.out;
  }

  @Test
  public void testCovertFavouriteEHentai() throws IOException, ParseException {
    FavouritesConverter converter = new FavouritesConverter();
    Source source = Okio.source(getClass().getClassLoader().getResourceAsStream("favourites_exhentai.html"));
    BufferedSource bufferedSource = Okio.buffer(source);
    FavouritesResult result = converter.convert(bufferedSource.readUtf8());

    List<Pair<String, Integer>> state = result.state();
    assertEquals(10, state.size());
    assertEquals("你好", state.get(0).first);
    assertEquals(1, (int) state.get(0).second);
    assertEquals("Favorites 1", state.get(1).first);
    assertEquals(0, (int) state.get(1).second);
    assertEquals("heyhey", state.get(2).first);
    assertEquals(0, (int) state.get(2).second);
    assertEquals("Favorites 3", state.get(3).first);
    assertEquals(0, (int) state.get(3).second);
    assertEquals("Favorites 4", state.get(4).first);
    assertEquals(0, (int) state.get(4).second);
    assertEquals("Favorites 5", state.get(5).first);
    assertEquals(0, (int) state.get(5).second);
    assertEquals("Favorites 6", state.get(6).first);
    assertEquals(0, (int) state.get(6).second);
    assertEquals("Favorites 7", state.get(7).first);
    assertEquals(0, (int) state.get(7).second);
    assertEquals("Favorites 8", state.get(8).first);
    assertEquals(0, (int) state.get(8).second);
    assertEquals("Favorites 9", state.get(9).first);
    assertEquals(0, (int) state.get(9).second);

    assertEquals(1, result.pages());

    List<FavouritesItem> list = result.list();
    assertEquals(1, list.size());
    assertEquals("[AS109] Shoujo and the Back Alley 0.9 – 3.1 [English] {Hennojin} [Uncensored]", list.get(0).note);
    assertEquals(0, list.get(0).date);
  }
}
