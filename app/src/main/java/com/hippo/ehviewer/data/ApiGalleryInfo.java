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

import com.hippo.ehviewer.util.Utils;


public class ApiGalleryInfo extends GalleryInfo{
    public String archiver_key;
    public String title_jpn;
    public String filecount;
    public long filesize;
    public boolean expunged;
    public String torrentcount;
    public String[] apiTags;

    public static final Parcelable.Creator<ApiGalleryInfo> CREATOR =
            new Parcelable.Creator<ApiGalleryInfo>() {
                @Override
                public ApiGalleryInfo createFromParcel(Parcel source) {
                    ApiGalleryInfo p = new ApiGalleryInfo();
                    p.gid = source.readInt();
                    p.token = source.readString();
                    p.title = source.readString();
                    p.posted = source.readString();
                    p.category = source.readInt();
                    p.thumb = source.readString();
                    p.uploader = source.readString();
                    p.rating = source.readFloat();
                    p.simpleLanguage = source.readString();
                    p.archiver_key = source.readString();
                    p.title_jpn = source.readString();
                    p.filecount = source.readString();
                    p.filesize = source.readLong();
                    p.expunged = Utils.int2boolean(source.readInt());
                    p.torrentcount = source.readString();
                    int apiTagsLength = source.readInt();
                    p.apiTags = new String[apiTagsLength];
                    for (int i = 0; i < apiTagsLength; i++)
                        p.apiTags[i] = source.readString();
                    return p;
                }

                @Override
                public ApiGalleryInfo[] newArray(int size) {
                    return new ApiGalleryInfo[size];
                }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(archiver_key);
        dest.writeString(title_jpn);
        dest.writeString(filecount);
        dest.writeLong(filesize);
        dest.writeInt(Utils.boolean2int(expunged));
        dest.writeString(torrentcount);
        dest.writeInt(apiTags.length);
        for (String str : apiTags)
            dest.writeString(str);
    }

}

