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

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 1/29/2017.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EhUtilsTest {

  // category string to category value
  @Test
  public void testGetCategory1() {
    assertEquals(EhUtils.CATEGORY_NON_H, EhUtils.getCategory("Non-H"));
    assertEquals(EhUtils.CATEGORY_ARTIST_CG, EhUtils.getCategory("Artist CG Sets"));
    assertEquals(EhUtils.CATEGORY_DOUJINSHI, EhUtils.getCategory("doujinshi"));
    assertEquals(EhUtils.CATEGORY_UNKNOWN, EhUtils.getCategory("HA HI"));
    assertEquals(EhUtils.CATEGORY_UNKNOWN, EhUtils.getCategory(null));
  }

  // category value to category string
  @Test
  public void testGetCategory2() {
    assertEquals("cosplay", EhUtils.getCategory(EhUtils.CATEGORY_COSPLAY));
    assertEquals("asian porn", EhUtils.getCategory(EhUtils.CATEGORY_ASIAN_PORN));
    assertEquals(null, EhUtils.getCategory(0));
  }

  @Test
  public void testGuessLang() {
    assertEquals(EhUtils.LANG_NL, EhUtils.guessLang("(Colber) Cleo op de Kostschool (dutch)"));
    assertEquals(EhUtils.LANG_NL, EhUtils.guessLang("(COMIC1☆6) [クレスタ (呉マサヒロ)] CL-orz 22 (パパのいうことを聞きなさい!) [オランダ語訳] [無修正]"));
    assertEquals(EhUtils.LANG_ZH, EhUtils.guessLang("T(こみトレ29) [Primal Gym (カワセセイキ)] SAOff AUTUMN (ソードアート·オンライン) [中国翻訳]"));
    assertEquals(EhUtils.LANG_OTHER, EhUtils.guessLang("xixi haha"));
  }
}
