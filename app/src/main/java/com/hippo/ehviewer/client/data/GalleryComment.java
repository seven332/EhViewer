/*
 * Copyright 2016 Hippo Seven
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

import android.os.Parcel;
import android.os.Parcelable;

public class GalleryComment implements Parcelable {

    // 0 for uploader comment. can't vote
    public long id;
    public int score;
    public boolean voteUp;
    public boolean voteDown;
    public String voteState;
    public long time;
    public String user;
    public String comment;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeInt(this.score);
        dest.writeByte(voteUp ? (byte) 1 : (byte) 0);
        dest.writeByte(voteDown ? (byte) 1 : (byte) 0);
        dest.writeString(this.voteState);
        dest.writeLong(this.time);
        dest.writeString(this.user);
        dest.writeString(this.comment);
    }

    public GalleryComment() {
    }

    protected GalleryComment(Parcel in) {
        this.id = in.readLong();
        this.score = in.readInt();
        this.voteUp = in.readByte() != 0;
        this.voteDown = in.readByte() != 0;
        this.voteState = in.readString();
        this.time = in.readLong();
        this.user = in.readString();
        this.comment = in.readString();
    }

    public static final Creator<GalleryComment> CREATOR = new Creator<GalleryComment>() {
        @Override
        public GalleryComment createFromParcel(Parcel source) {
            return new GalleryComment(source);
        }

        @Override
        public GalleryComment[] newArray(int size) {
            return new GalleryComment[size];
        }
    };
}
