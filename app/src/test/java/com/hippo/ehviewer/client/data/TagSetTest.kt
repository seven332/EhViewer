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

package com.hippo.ehviewer.client.data

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/*
 * Created by Hippo on 2017/8/10.
 */

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class TagSetTest {

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
  fun testAdd() {
    val tags = TagSet()

    tags.add("AAA", "111")
    tags.add("AAA", "222")
    tags.add("AAA", "333")
    tags.add("BBB", "111")

    assertEquals(mapOf(
        "AAA" to listOf("111", "222", "333"),
        "BBB" to listOf("111")
    ), tags)
  }

  @Test
  fun testRemove() {
    val tags = TagSet()

    tags.add("AAA", "111")
    tags.add("AAA", "222")
    tags.add("AAA", "333")
    tags.add("BBB", "111")

    tags.remove("BBB", "111")

    assertEquals(mapOf(
        "AAA" to listOf("111", "222", "333")
    ), tags)

    tags.remove("AAA", "111")

    assertEquals(mapOf(
        "AAA" to listOf("222", "333")
    ), tags)
  }

  @Test
  fun testSizeIsEmpty() {
    val tags = TagSet()
    assertEquals(0, tags.size())
    assertEquals(true, tags.isEmpty())

    tags.add("AAA", "111")
    assertEquals(1, tags.size())
    assertEquals(false, tags.isEmpty())

    tags.add("AAA", "222")
    assertEquals(2, tags.size())
    assertEquals(false, tags.isEmpty())

    tags.add("BBB", "111")
    assertEquals(3, tags.size())
    assertEquals(false, tags.isEmpty())
  }

  @Test
  fun testFirstTag() {
    val tags = TagSet()
    assertEquals(null, tags.firstTag())

    tags.add("AAA", "111")
    assertEquals(Pair("AAA", "111"), tags.firstTag())

    tags.add("AAA", "222")
    assertEquals(Pair("AAA", "111"), tags.firstTag())

    tags.add("BBB", "111")
    assertEquals(Pair("AAA", "111"), tags.firstTag())
  }
}
