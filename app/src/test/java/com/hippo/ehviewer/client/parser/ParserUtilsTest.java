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

import org.junit.Test;

public class ParserUtilsTest {

  @Test
  public void testParseInt() {
    assertEquals(23532, ParserUtils.parseInt(" &nbsp;  23,532  ", 0));
    assertEquals(0, ParserUtils.parseInt(" &nbsp; sd 23,532  ", 0));
  }

  @Test
  public void testParseLong() {
    assertEquals(23532L, ParserUtils.parseLong(" &nbsp;  23,532  ", 0));
    assertEquals(0L, ParserUtils.parseLong(" &nbsp; sd 23,532  ", 0));
  }

  @Test
  public void testParseFloat() {
    assertEquals(23532.67f, ParserUtils.parseFloat(" &nbsp;  23,532.67  ", 0.0f), 0.0f);
    assertEquals(0.0f, ParserUtils.parseFloat(" &nbsp; sd 23,532.67  ", 0.0f), 0.0f);
  }

  @Test
  public void testParseDate() {
    assertEquals(1485753540000L, ParserUtils.parseDate("  2017-01-30 05:19 &nbsp; ", 0));
    assertEquals(0L, ParserUtils.parseDate("sad-01-30 05:19", 0));
  }

  @Test
  public void testParseCommentDate() {
    assertEquals(1449587220000L, ParserUtils.parseCommentDate("  08 December 2015, 15:07 UTC &nbsp; ", 0));
  }

  @Test
  public void testCompleteUrl() {
    assertEquals("https://xixihaha/xi/ha", ParserUtils.completeUrl("https://xixihaha/", "/xi/ha"));
    assertEquals("https://xixihaha/xi/ha", ParserUtils.completeUrl("https://xixihaha/", "xi/ha"));
    assertEquals("https://xixihaha/xi/ha", ParserUtils.completeUrl("https://xixihaha", "/xi/ha"));
    assertEquals("https://xixihaha/xi/ha", ParserUtils.completeUrl("https://xixihaha", "xi/ha"));

    assertEquals("https://xi/ha", ParserUtils.completeUrl("https://xixihaha/", "https://xi/ha"));
  }

  @Test
  public void testStrip() {
    assertEquals("abc", ParserUtils.strip("    abc    "));
    assertEquals("ab c", ParserUtils.strip("    ab c    "));
    assertEquals("abc", ParserUtils.strip("    abc"));
    assertEquals("abc", ParserUtils.strip("abc   "));
    assertEquals("", ParserUtils.strip("        "));
  }

  @Test
  public void testUnescape() {
    assertEquals("ab c &", ParserUtils.unescape("    ab c &amp;  "));
    assertEquals("ab c", ParserUtils.unescape("    ab c &nbsp;  "));
  }

  @Test
  public void testIsUrl() {
    assertEquals(false, ParserUtils.isUrl(null));
    assertEquals(false, ParserUtils.isUrl(""));
    assertEquals(false, ParserUtils.isUrl("sdsd"));
    assertEquals(true, ParserUtils.isUrl("https://"));
    assertEquals(true, ParserUtils.isUrl("https://ss"));
    assertEquals(true, ParserUtils.isUrl("http://"));
    assertEquals(true, ParserUtils.isUrl("http://ss"));
    assertEquals(false, ParserUtils.isUrl("http:"));
  }
}
