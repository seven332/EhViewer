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

import com.hippo.ehviewer.client.CATEGORY_NON_H
import com.hippo.ehviewer.client.FAV_CAT_UNKNOWN
import com.hippo.ehviewer.client.LANG_JA
import com.hippo.ehviewer.client.data.TagSet
import okio.Okio
import org.jsoup.Jsoup
import org.junit.Test
import kotlin.test.assertEquals

/*
 * Created by Hippo on 2017/7/31.
 */

class GalleryDetailParserTest {

  fun assertEquals(map : Map<String, List<String>>, tags: TagSet) {
    val iterator1 = map.iterator()
    val iterator2 = tags.iterator()

    while (true) {
      if (iterator1.hasNext()) {
        assertEquals(true, iterator2.hasNext())

        val (namespace1, list1) = iterator1.next()
        val (namespace2, list2) = iterator2.next()

        assertEquals(namespace1, namespace2)
        assertEquals(list1, list2)
      } else {
        assertEquals(false, iterator2.hasNext())
        return
      }
    }
  }

  @Test
  fun testParseExHentai() {
    val source = Okio.source(javaClass.classLoader.getResourceAsStream("gallery_detail_exhentai.html"))
    val bufferedSource = Okio.buffer(source)
    val body = bufferedSource.readUtf8()
    val info = parseGalleryDetail(Jsoup.parse(body))!!

    assertEquals(1094223, info.gid)
    assertEquals("dc6a72a27d", info.token)
    assertEquals("(C89) [EXPOT (Kamiya)] Oyobi desu ka Ojou-sama. (Love Live!)", info.title)
    assertEquals("(C89) [EXPOT (かみや)] お呼びですかお嬢さま。 (ラブライブ!)", info.titleJpn)
    assertEquals("https://exhentai.org/t/bf/93/bf9397cb6d6ee7c98280492cb6afa38e9d5b9a89-2351978-3476-4909-jpg_250.jpg", info.coverUrl)
    assertEquals("bf9397cb6d6ee7c98280492cb6afa38e9d5b9a89-2351978-3476-4909-jpg", info.coverFingerprint)
    assertEquals(250.0f / 354.0f, info.coverRatio)
    assertEquals("417080--3d4f94119742e0a35b457a61c6e3ce944c675f16", info.archiveKey)
    assertEquals(1, info.torrents)
    assertEquals(CATEGORY_NON_H, info.category)
    assertEquals("Miles Edgeworth", info.uploader)
    assertEquals("2017-07-31 07:20".date(), info.date)
    assertEquals(0L, info.parentGid)
    assertEquals(null, info.parentToken)
    assertEquals(false, info.invalid)
    assertEquals(LANG_JA, info.language)
    assertEquals((35.34f * 1024 * 1024).toLong(), info.size)
    assertEquals(22, info.pages)
    assertEquals(0, info.favourited)
    assertEquals(1.50f, info.rating)
    assertEquals(5, info.rated)
    assertEquals(FAV_CAT_UNKNOWN, info.favouriteSlot)
    assertEquals(mapOf(
        "parody" to listOf("love live"),
        "character" to listOf("eri ayase", "nico yazawa", "nozomi toujou"),
        "group" to listOf("expot"),
        "artist" to listOf("kamiya"),
        "female" to listOf("females only")
    ), info.tags)
  }

  @Test
  fun testParseCommentsExHentai() {
    val source = Okio.source(javaClass.classLoader.getResourceAsStream("gallery_comments_exhentai.html"))
    val bufferedSource = Okio.buffer(source)
    val body = bufferedSource.readUtf8()
    val comments = parseComments(Jsoup.parse(body))

    assertEquals(2, comments.size)

    assertEquals(0, comments[0].id)
    assertEquals("11 August 2017, 02:20 UTC".commentDate(), comments[0].date)
    assertEquals("doggerhotter", comments[0].user)
    assertEquals("raw: \n" +
        "<a href=\"https://exhentai.org/g/1042775/eb45af810d/\">https://exhentai.org/g/1042775/eb45af810d/</a>", comments[0].comment)
    assertEquals(0, comments[0].score)
    assertEquals(false, comments[0].votedUp)
    assertEquals(false, comments[0].votedDown)
    assertEquals(null, comments[0].voteState)

    assertEquals(1864159, comments[1].id)
    assertEquals("11 August 2017, 03:39 UTC".commentDate(), comments[1].date)
    assertEquals("Momo_Yuki", comments[1].user)
    assertEquals("Over 300+MB....", comments[1].comment)
    assertEquals(7, comments[1].score)
    assertEquals(false, comments[1].votedUp)
    assertEquals(false, comments[1].votedDown)
    assertEquals("Base +7", comments[1].voteState)
  }
}
