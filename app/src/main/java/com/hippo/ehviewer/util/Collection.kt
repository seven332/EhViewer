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
 * Created by Hippo on 2017/7/24.
 */

/**
 * Performs the given [action] on each element. ANDs all results and returns it.
 */
inline fun <T> Iterable<T>.forEachAny(action: (T) -> Boolean): Boolean {
  var result = false
  for (element in this) {
    result = action(element) || result
  }
  return result
}

/**
 * Performs the given [action] on each entry. Remove the entry if return `true`.
 */
inline fun <K, V> MutableMap<K, V>.forEachRemove(action: (K, V) -> Boolean ) {
  val iterator = entries.iterator()
  while (iterator.hasNext()) {
    val entry = iterator.next()
    if (action(entry.key, entry.value)) {
      iterator.remove()
    }
  }
}
