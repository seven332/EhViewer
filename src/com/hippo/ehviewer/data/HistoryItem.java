/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.data;

import android.os.Parcel;
import android.os.Parcelable;

public class HistoryItem extends GalleryInfo {
    public int mode;
    public long date;

    public static final Parcelable.Creator<HistoryItem> CREATOR =
            new Parcelable.Creator<HistoryItem>() {
                @Override
                public HistoryItem createFromParcel(Parcel source) {
                    HistoryItem p = new HistoryItem();
                    p.gid = source.readInt();
                    p.token = source.readString();
                    p.title = source.readString();
                    p.posted = source.readString();
                    p.category = source.readInt();
                    p.thumb = source.readString();
                    p.uploader = source.readString();
                    p.rating = source.readFloat();
                    p.simpleLanguage = source.readString();
                    p.mode = source.readInt();
                    p.date = source.readLong();
                    return p;
                }

                @Override
                public HistoryItem[] newArray(int size) {
                    return new HistoryItem[size];
                }
    };

    public HistoryItem() {
        // Empty
    }

    public HistoryItem(GalleryInfo gi, int m, long d) {
        super(gi);
        mode = m;
        date = d;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mode);
        dest.writeLong(date);
    }

}
