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

import android.os.Parcel
import android.os.Parcelable

/*
 * Created by Hippo on 2017/7/25.
 */

class TagSet() : Iterable<Map.Entry<String, List<String>>>, Parcelable {

  private val map = LinkedHashMap<String, MutableList<String>>()

  fun add(namespace: String, tag: String) {
    var list = map[namespace]
    if (list == null) {
      list = arrayListOf()
      map[namespace] = list
    }
    list.add(tag)
  }

  fun remove(namespace: String, tag: String) {
    val list = map[namespace]
    if (list != null) {
      list.remove(tag)
      if (list.isEmpty()) {
        map.remove(namespace)
      }
    }
  }

  override fun iterator(): Iterator<Map.Entry<String, List<String>>> = map.iterator()

  fun size(): Int = map.values.sumBy { it.size }

  fun isEmpty(): Boolean = map.isEmpty()

  fun firstTag(): Pair<String, String>? {
    val iterator = map.iterator()
    if (iterator.hasNext()) {
      val (namespace, tags) = iterator.next()
      if (tags.isNotEmpty()) {
        return Pair(namespace, tags[0])
      } else {
        return null
      }
    } else {
      return null
    }
  }

  fun set(tags: TagSet) {
    map.clear()
    for ((namespace, list) in tags) {
      map[namespace] = ArrayList(list)
    }
  }

  constructor(parcel: Parcel) : this() {
    for (i in 0 until parcel.readInt()) {
      val namespace = parcel.readString()
      val list = arrayListOf<String>()
      map[namespace] = list

      for (j in 0 until parcel.readInt()) {
        list.add(parcel.readString())
      }
    }
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(map.size)

    for ((namespace, list) in map) {
      parcel.writeString(namespace)

      parcel.writeInt(list.size)
      for (tag in list) {
        parcel.writeString(tag)
      }
    }
  }

  override fun describeContents(): Int = 0

  companion object CREATOR : Parcelable.Creator<TagSet> {
    override fun createFromParcel(parcel: Parcel): TagSet {
      return TagSet(parcel)
    }

    override fun newArray(size: Int): Array<TagSet?> {
      return arrayOfNulls(size)
    }
  }
}
