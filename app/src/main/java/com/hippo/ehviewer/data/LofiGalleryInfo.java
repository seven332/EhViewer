/*
 * Copyright (C) 2014-2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.data;

import android.os.Parcel;

public class LofiGalleryInfo extends GalleryInfo {

    public String[] lofiTags;

    public static final Creator<LofiGalleryInfo> CREATOR =
            new Creator<LofiGalleryInfo>() {
                @Override
                public LofiGalleryInfo createFromParcel(Parcel source) {
                    int length;

                    LofiGalleryInfo p = new LofiGalleryInfo();
                    p.gid = source.readInt();
                    p.token = source.readString();
                    p.title = source.readString();
                    p.posted = source.readString();
                    p.category = source.readInt();
                    p.thumb = source.readString();
                    p.uploader = source.readString();
                    p.rating = source.readFloat();
                    p.simpleLanguage = source.readString();

                    length = source.readInt();
                    p.lofiTags = new String[length];
                    for (int i = 0; i < length; i++)
                        p.lofiTags[i] = source.readString();

                    return p;
                }

                @Override
                public LofiGalleryInfo[] newArray(int size) {
                    return new LofiGalleryInfo[size];
                }
    };

    public LofiGalleryInfo() {}

    public LofiGalleryInfo(GalleryInfo galleryInfo) {
        gid = galleryInfo.gid;
        token = galleryInfo.token;
        title = galleryInfo.title;
        posted = galleryInfo.posted;
        category = galleryInfo.category;
        thumb = galleryInfo.thumb;
        uploader = galleryInfo.uploader;
        rating = galleryInfo.rating;
        simpleLanguage = galleryInfo.simpleLanguage;
        lofiTags = new String[0];
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        int length = lofiTags == null ? 0 : lofiTags.length;
        dest.writeInt(length);
        for (int i = 0; i < length; i++)
            dest.writeString(lofiTags[i]);
    }
}
