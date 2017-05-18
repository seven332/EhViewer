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

package com.hippo.ehviewer.client.data;

/*
 * Created by Hippo on 5/15/2017.
 */

import android.os.Parcel;
import android.os.Parcelable;
import com.hippo.yorozuya.HashCodeUtils;
import com.hippo.yorozuya.ObjectUtils;

public final class CommentEntry implements Parcelable {

  // 0 for uploader comment. can't vote
  public long id;
  public long date;
  public String user;
  public String comment;
  public int score;
  public boolean votedUp;
  public boolean votedDown;
  public String voteState;

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public String toString() {
    return "CommentEntry: {\n"
        + "id: " + id + ",\n"
        + "date: " + date + ",\n"
        + "user: " + user + ",\n"
        + "comment: " + comment + ",\n"
        + "score: " + score + ",\n"
        + "votedUp: " + votedUp + ",\n"
        + "votedDown: " + votedDown + ",\n"
        + "voteState: " + voteState + "\n"
        + "}";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CommentEntry) {
      CommentEntry entry = (CommentEntry) obj;
      return entry.id == id &&
          entry.date == date &&
          ObjectUtils.equals(entry.user, user) &&
          ObjectUtils.equals(entry.comment, comment) &&
          entry.score == score &&
          entry.votedUp == votedUp &&
          entry.votedDown == votedDown &&
          ObjectUtils.equals(entry.voteState, voteState);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return HashCodeUtils.hashCode(
        id,
        date,
        user,
        comment,
        score,
        votedUp,
        votedDown,
        voteState
    );
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(this.id);
    dest.writeLong(this.date);
    dest.writeString(this.user);
    dest.writeString(this.comment);
    dest.writeInt(this.score);
    dest.writeByte(this.votedUp ? (byte) 1 : (byte) 0);
    dest.writeByte(this.votedDown ? (byte) 1 : (byte) 0);
    dest.writeString(this.voteState);
  }

  public CommentEntry() {}

  protected CommentEntry(Parcel in) {
    this.id = in.readLong();
    this.date = in.readLong();
    this.user = in.readString();
    this.comment = in.readString();
    this.score = in.readInt();
    this.votedUp = in.readByte() != 0;
    this.votedDown = in.readByte() != 0;
    this.voteState = in.readString();
  }

  public static final Parcelable.Creator<CommentEntry> CREATOR =
      new Parcelable.Creator<CommentEntry>() {
        @Override
        public CommentEntry createFromParcel(Parcel source) {
          return new CommentEntry(source);
        }

        @Override
        public CommentEntry[] newArray(int size) {
          return new CommentEntry[size];
        }
      };
}
