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
 * Created by Hippo on 2017/8/11.
 */

class Comment() : Parcelable {

  // 0 for uploader comment. can't vote
  var id: Long = 0
  var date: Long = 0
  var user: String? = null
  var comment: String? = null
  var score: Int = 0
  var votedUp: Boolean = false
  var votedDown: Boolean = false
  var voteState: String? = null

  constructor(parcel: Parcel) : this() {
    id = parcel.readLong()
    date = parcel.readLong()
    user = parcel.readString()
    comment = parcel.readString()
    score = parcel.readInt()
    votedUp = parcel.readByte() != 0.toByte()
    votedDown = parcel.readByte() != 0.toByte()
    voteState = parcel.readString()
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeLong(id)
    parcel.writeLong(date)
    parcel.writeString(user)
    parcel.writeString(comment)
    parcel.writeInt(score)
    parcel.writeByte(if (votedUp) 1 else 0)
    parcel.writeByte(if (votedDown) 1 else 0)
    parcel.writeString(voteState)
  }

  override fun describeContents(): Int = 0

  companion object CREATOR : Parcelable.Creator<Comment> {
    override fun createFromParcel(parcel: Parcel): Comment {
      return Comment(parcel)
    }

    override fun newArray(size: Int): Array<Comment?> {
      return arrayOfNulls(size)
    }
  }
}
