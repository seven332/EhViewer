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

public class GalleryDetail extends GalleryInfo {

    public int torrentCount;
    public String torrentUrl;
    public String parent;
    public String visible;
    public String language;
    public String size;
    public String resize;
    public int pages;
    public int favoredTimes;
    public boolean isFavored;
    public int ratedTimes;
    public GalleryTagGroup[] tags;
    public GalleryComment[] comments;
    public int previewPages;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.torrentCount);
        dest.writeString(this.torrentUrl);
        dest.writeString(this.parent);
        dest.writeString(this.visible);
        dest.writeString(this.language);
        dest.writeString(this.size);
        dest.writeString(this.resize);
        dest.writeInt(this.pages);
        dest.writeInt(this.favoredTimes);
        dest.writeByte(isFavored ? (byte) 1 : (byte) 0);
        dest.writeInt(this.ratedTimes);
        dest.writeParcelableArray(this.tags, 0);
        dest.writeParcelableArray(this.comments, 0);
        dest.writeInt(this.previewPages);
    }

    public GalleryDetail() {
    }

    protected GalleryDetail(Parcel in) {
        super(in);
        this.torrentCount = in.readInt();
        this.torrentUrl = in.readString();
        this.parent = in.readString();
        this.visible = in.readString();
        this.language = in.readString();
        this.size = in.readString();
        this.resize = in.readString();
        this.pages = in.readInt();
        this.favoredTimes = in.readInt();
        this.isFavored = in.readByte() != 0;
        this.ratedTimes = in.readInt();
        this.tags = in.createTypedArray(GalleryTagGroup.CREATOR);
        this.comments = in.createTypedArray(GalleryComment.CREATOR);
        this.previewPages = in.readInt();
    }

    public static final Creator<GalleryDetail> CREATOR = new Creator<GalleryDetail>() {
        public GalleryDetail createFromParcel(Parcel source) {
            return new GalleryDetail(source);
        }

        public GalleryDetail[] newArray(int size) {
            return new GalleryDetail[size];
        }
    };
}
