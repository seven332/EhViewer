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

/*
 * Created by Hippo on 2017/7/26.
 */

fun String.replaceEach(searchList: Array<String>, replacementList: Array<String>): String = replaceEach(searchList, replacementList, false, 0)

private fun String.replaceEach(searchList: Array<String>, replacementList: Array<String>, repeat: Boolean, timeToLive: Int): String {

  // mchyzer Performance note: This creates very few new objects (one major goal)
  // let me know if there are performance requests, we can create a harness to measure

  if (searchList.isEmpty() || replacementList.isEmpty()) {
    return this
  }

  // if recursing, this shouldn't be less than 0
  if (timeToLive < 0) {
    throw IllegalStateException("Aborting to protect against StackOverflowError - " + "output of one loop is the input of another")
  }

  val searchLength = searchList.size
  val replacementLength = replacementList.size

  // make sure lengths are ok, these need to be equal
  if (searchLength != replacementLength) {
    throw IllegalArgumentException("Search and Replace array lengths don't match: "
        + searchLength
        + " vs "
        + replacementLength)
  }

  // keep track of which still have matches
  val noMoreMatchesForReplIndex = BooleanArray(searchLength)

  // index on index that the match was found
  var textIndex = -1
  var replaceIndex = -1
  var tempIndex = -1

  // index of replace array that will replace the search string found
  // NOTE: logic duplicated below START
  for (i in 0..searchLength - 1) {
    if (noMoreMatchesForReplIndex[i] || searchList[i].isEmpty()) {
      continue
    }
    tempIndex = indexOf(searchList[i])

    // see if we need to keep searching for this
    if (tempIndex == -1) {
      noMoreMatchesForReplIndex[i] = true
    } else {
      if (textIndex == -1 || tempIndex < textIndex) {
        textIndex = tempIndex
        replaceIndex = i
      }
    }
  }
  // NOTE: logic mostly below END

  // no search strings found, we are done
  if (textIndex == -1) {
    return this
  }

  var start = 0

  // get a good guess on the size of the result buffer so it doesn't have to double if it goes over a bit
  var increase = 0

  // count the replacement text elements that are larger than their corresponding text being replaced
  for (i in searchList.indices) {
    val greater = replacementList[i].length - searchList[i].length
    if (greater > 0) {
      increase += 3 * greater // assume 3 matches
    }
  }
  // have upper-bound at 20% increase, then let Java take over
  increase = Math.min(increase, length / 5)

  val buf = StringBuilder(length + increase)

  while (textIndex != -1) {

    for (i in start..textIndex - 1) {
      buf.append(this[i])
    }
    buf.append(replacementList[replaceIndex])

    start = textIndex + searchList[replaceIndex].length

    textIndex = -1
    replaceIndex = -1
    tempIndex = -1
    // find the next earliest match
    // NOTE: logic mostly duplicated above START
    for (i in 0..searchLength - 1) {
      if (noMoreMatchesForReplIndex[i] || searchList[i].isEmpty()) {
        continue
      }
      tempIndex = indexOf(searchList[i], start)

      // see if we need to keep searching for this
      if (tempIndex == -1) {
        noMoreMatchesForReplIndex[i] = true
      } else {
        if (textIndex == -1 || tempIndex < textIndex) {
          textIndex = tempIndex
          replaceIndex = i
        }
      }
    }
    // NOTE: logic duplicated above END

  }
  val textLength = length
  for (i in start..textLength - 1) {
    buf.append(this[i])
  }
  val result = buf.toString()
  if (!repeat) {
    return result
  }

  return replaceEach(searchList, replacementList, repeat, timeToLive - 1)
}

