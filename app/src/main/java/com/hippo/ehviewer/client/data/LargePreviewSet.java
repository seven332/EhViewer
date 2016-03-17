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

import com.hippo.yorozuya.IntList;

import java.util.ArrayList;

public class LargePreviewSet implements Parcelable {

    private final IntList mIndexList;
    private final ArrayList<String> mImageUrlList;
    private final ArrayList<String> mPageUrlList;

    public int size() {
        return mImageUrlList.size();
    }

    public void addItem(int index, String imageUrl, String pageUrl) {
        mIndexList.add(index);
        mImageUrlList.add(imageUrl);
        mPageUrlList.add(pageUrl);
    }

    public int getIndexAt(int index) {
        return mIndexList.get(index);
    }

    public String getImageUrlAt(int index) {
        return mImageUrlList.get(index);
    }

    public String getPageUrlAt(int index) {
        return mPageUrlList.get(index);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mIndexList, flags);
        dest.writeStringList(this.mImageUrlList);
        dest.writeStringList(this.mPageUrlList);
    }

    public LargePreviewSet() {
        mIndexList = new IntList();
        mImageUrlList = new ArrayList<>();
        mPageUrlList = new ArrayList<>();
    }

    protected LargePreviewSet(Parcel in) {
        this.mIndexList = in.readParcelable(IntList.class.getClassLoader());
        this.mImageUrlList = in.createStringArrayList();
        this.mPageUrlList = in.createStringArrayList();
    }

    public static final Parcelable.Creator<LargePreviewSet> CREATOR = new Parcelable.Creator<LargePreviewSet>() {
        @Override
        public LargePreviewSet createFromParcel(Parcel source) {
            return new LargePreviewSet(source);
        }

        @Override
        public LargePreviewSet[] newArray(int size) {
            return new LargePreviewSet[size];
        }
    };
}
