/*
 * Copyright 2019 Hippo Seven
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.hippo.ehviewer.client.EhUtils;
import edu.emory.mathcs.backport.java.util.Arrays;
import java.io.InputStream;
import java.util.List;
import okio.BufferedSource;
import okio.Okio;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class GalleryListParserTest {

  private static final String MINIMAL = "GalleryListParserTestMinimal.html";
  private static final String COMPAT = "GalleryListParserTestCompat.html";
  private static final String EXTENDED = "GalleryListParserTestExtended.html";
  private static final String THUMBNAIL = "GalleryListParserTestThumbnail.html";

  @ParameterizedRobolectricTestRunner.Parameters(name = "{index}-{0}")
  public static List data() {
    return Arrays.asList(new Object[][] {
        { MINIMAL },
        { COMPAT },
        { EXTENDED },
        { THUMBNAIL },
    });
  }

  private String file;

  public GalleryListParserTest(String file) {
    this.file = file;
  }

  @Test
  public void testParse() throws Exception {
    InputStream resource = GalleryPageApiParserTest.class.getResourceAsStream(file);
    BufferedSource source = Okio.buffer(Okio.source(resource));
    String body = source.readUtf8();

    GalleryListParser.Result result = GalleryListParser.parse(body);

    assertEquals(50, result.galleryInfoList.size());

    result.galleryInfoList.forEach(gi -> {
      assertNotEquals(0, gi.gid);
      assertNotEquals(0, gi.token);
      assertNotNull(gi.title);

      //assertNotNull(gi.simpleTags);

      assertNotEquals(0, gi.category);
      assertNotEquals(EhUtils.UNKNOWN, gi.category);
      assertNotEquals(0, gi.thumbWidth);
      assertNotEquals(0, gi.thumbHeight);
      assertNotNull(gi.thumb);
      assertNotNull(gi.posted);
      assertNotEquals(0.0, gi.rating);
      if (MINIMAL.equals(file) || COMPAT.equals(file) || EXTENDED.equals(file)) {
        assertNotNull(gi.uploader);
      }
      if (COMPAT.equals(file) || EXTENDED.equals(file)) {
        assertNotEquals(0, gi.pages);
      }
    });
  }
}
