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

package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.CATEGORY_DOUJINSHI
import com.hippo.ehviewer.client.CATEGORY_IMAGE_SET
import com.hippo.ehviewer.client.CATEGORY_NON_H
import com.hippo.ehviewer.client.FAV_CAT_UNKNOWN
import com.hippo.ehviewer.client.LANG_EN
import com.hippo.ehviewer.client.LANG_UNKNOWN
import okio.Okio
import org.jsoup.Jsoup
import org.junit.Test
import kotlin.test.assertEquals

/*
 * Created by Hippo on 2017/7/27.
 */

class GalleryListParserTest {

  @Test
  fun testCovertEHentai() {
    val source = Okio.source(javaClass.classLoader.getResourceAsStream("gallery_list_exhentai.html"))
    val bufferedSource = Okio.buffer(source)
    val body = bufferedSource.readUtf8()
    val list = parseGalleryList(Jsoup.parse(body))!!

    assertEquals(25, list.size)

    assertEquals(1092457, list[0].gid)
    assertEquals("6898028ae8", list[0].token)
    assertEquals("Artist - Gui Fu Shen Nai", list[0].title)
    assertEquals(LANG_UNKNOWN, list[0].language)
    assertEquals(CATEGORY_IMAGE_SET, list[0].category)
    assertEquals("2017-07-27 14:22".date(), list[0].date)
    assertEquals(200.0f / 290.0f, list[0].coverRatio)
    assertEquals("https://exhentai.org/t/80/4d/804d12f12c1b121679683ea7bff18fc923265d42-1215053-1095-1587-jpg_l.jpg", list[0].coverUrl)
    assertEquals("804d12f12c1b121679683ea7bff18fc923265d42-1215053-1095-1587-jpg", list[0].coverFingerprint)
    assertEquals(FAV_CAT_UNKNOWN, list[0].favouriteSlot)
    assertEquals(false, list[0].invalid)
    assertEquals(4.0f, list[0].rating)
    assertEquals("MSimm1", list[0].uploader)

    assertEquals(1092459, list[1].gid)
    assertEquals("2c468bf350", list[1].token)
    assertEquals("(C84) [Zenoside (Zeno)] Moe Tsukiro!! Yokujou Retsujou Chou Hatsujou (Touhou Project)", list[1].title)
    assertEquals(LANG_UNKNOWN, list[1].language)
    assertEquals(CATEGORY_NON_H, list[1].category)
    assertEquals("2017-07-27 14:13".date(), list[1].date)
    assertEquals(200.0f / 141.0f, list[1].coverRatio)
    assertEquals("https://exhentai.org/t/43/36/4336b944072f2e888a4858902f865af438f3c798-5981539-2847-2000-jpg_l.jpg", list[1].coverUrl)
    assertEquals("4336b944072f2e888a4858902f865af438f3c798-5981539-2847-2000-jpg", list[1].coverFingerprint)
    assertEquals(FAV_CAT_UNKNOWN, list[1].favouriteSlot)
    assertEquals(false, list[1].invalid)
    assertEquals(3.0f, list[1].rating)
    assertEquals("ghap", list[1].uploader)

    assertEquals(1092456, list[2].gid)
    assertEquals("2d6eef4c01", list[2].token)
    assertEquals("[Minazuki Juuzou] umi no toriton (tako ni notta shounen) | Triton of the Sea (The Boy Who Rode An Octopus) [English] [gustmonk] [Decensored]", list[2].title)
    assertEquals(LANG_EN, list[2].language)
    assertEquals(CATEGORY_DOUJINSHI, list[2].category)
    assertEquals("2017-07-27 14:08".date(), list[2].date)
    assertEquals(200.0f / 284.0f, list[2].coverRatio)
    assertEquals("https://exhentai.org/t/4a/2b/4a2bb76b6f2a4cab3cdfa2eb48cb1de3aa5e9fc8-1599453-2120-3000-jpg_l.jpg", list[2].coverUrl)
    assertEquals("4a2bb76b6f2a4cab3cdfa2eb48cb1de3aa5e9fc8-1599453-2120-3000-jpg", list[2].coverFingerprint)
    assertEquals(FAV_CAT_UNKNOWN, list[2].favouriteSlot)
    assertEquals(false, list[2].invalid)
    assertEquals(3.0f, list[2].rating)
    assertEquals("gustmonk", list[2].uploader)

    assertEquals(1092455, list[3].gid)
    assertEquals("8ffd238cc2", list[3].token)
    assertEquals("(Houraigekisen! Yo-i! 29Senme) [Rojiura Manhole (Maki)] Hishokan Fusou o Houchi shi Sugitara Taihen'na Koto ni Natta (Kantai Collection -KanColle-)", list[3].title)
    assertEquals(LANG_UNKNOWN, list[3].language)
    assertEquals(CATEGORY_DOUJINSHI, list[3].category)
    assertEquals("2017-07-27 14:04".date(), list[3].date)
    assertEquals(200.0f / 285.0f, list[3].coverRatio)
    assertEquals("https://exhentai.org/t/fb/53/fb53f99ff87c8246b27c7948af09b5382aa61e48-145092-1050-1492-jpg_l.jpg", list[3].coverUrl)
    assertEquals("fb53f99ff87c8246b27c7948af09b5382aa61e48-145092-1050-1492-jpg", list[3].coverFingerprint)
    assertEquals(0, list[3].favouriteSlot)
    assertEquals(false, list[3].invalid)
    assertEquals(2.5f, list[3].rating)
    assertEquals("Stuckeyj2012", list[3].uploader)
  }
}
