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

package com.hippo.ehviewer.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/*
 * Created by Hippo on 2017/7/26.
 */

class StringTest {

  @Test
  fun testReplaceEach() {
    //JAVADOC TESTS START
    assertEquals("".replaceEach(arrayOf("a"), arrayOf("b")), "")
    assertEquals("aba".replaceEach(arrayOf("a"), arrayOf("")), "b")
    assertEquals("abcde".replaceEach(arrayOf("ab", "d"), arrayOf("w", "t")), "wcte")
    assertEquals("abcde".replaceEach(arrayOf("ab", "d"), arrayOf("d", "t")), "dcte")
    //JAVADOC TESTS END

    assertEquals("bcc", "abc".replaceEach(arrayOf("a", "b"), arrayOf("b", "c")))
    assertEquals("q651.506bera", "d216.102oren".replaceEach(
        arrayOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        arrayOf("n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "5", "6", "7", "8", "9", "1", "2", "3", "4")))

    try {
      "abba".replaceEach(arrayOf("a"), arrayOf("b", "a"))
      fail("StringUtils.replaceEach(String, String[], String[]) expecting IllegalArgumentException")
    } catch (ex: IllegalArgumentException) {
      // expected
    }
  }
}
