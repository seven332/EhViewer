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

public final class PreviewPage implements Parcelable {

  public final int index;
  public final String url;
  public final String image;
  public final boolean clip;
  public final int clipLeft;
  public final int clipTop;
  public final int clipWidth;
  public final int clipHeight;

  public PreviewPage(int index, String url, String image) {
    this.index = index;
    this.url = url;
    this.image = image;
    this.clip = false;
    this.clipLeft = 0;
    this.clipTop = 0;
    this.clipWidth = 0;
    this.clipHeight = 0;
  }

  public PreviewPage(int index, String url, String image,
      int clipLeft, int clipTop, int clipWidth, int clipHeight) {
    this.index = index;
    this.url = url;
    this.image = image;
    this.clip = true;
    this.clipLeft = clipLeft;
    this.clipTop = clipTop;
    this.clipWidth = clipWidth;
    this.clipHeight = clipHeight;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PreviewPage) {
      PreviewPage page = (PreviewPage) obj;
      return page.index == index &&
          ObjectUtils.equals(page.url, url) &&
          ObjectUtils.equals(page.image, image) &&
          page.clip == clip &&
          page.clipLeft == clipLeft &&
          page.clipTop == clipTop &&
          page.clipWidth == clipWidth &&
          page.clipHeight == clipHeight;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return HashCodeUtils.hashCode(
        index,
        url,
        image,
        clip,
        clipLeft,
        clipTop,
        clipWidth,
        clipHeight
    );
  }

  @Override
  public String toString() {
    return "PreviewPage: {\n"
        + "index: " + index + ",\n"
        + "url: " + url + ",\n"
        + "image: " + image + ",\n"
        + "clip: " + clip + ",\n"
        + "clipLeft: " + clipLeft + ",\n"
        + "clipTop: " + clipTop + ",\n"
        + "clipWidth: " + clipWidth + ",\n"
        + "clipHeight: " + clipHeight + "\n"
        + "}";
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(this.index);
    dest.writeString(this.url);
    dest.writeString(this.image);
    dest.writeByte(this.clip ? (byte) 1 : (byte) 0);
    dest.writeInt(this.clipLeft);
    dest.writeInt(this.clipTop);
    dest.writeInt(this.clipWidth);
    dest.writeInt(this.clipHeight);
  }

  protected PreviewPage(Parcel in) {
    this.index = in.readInt();
    this.url = in.readString();
    this.image = in.readString();
    this.clip = in.readByte() != 0;
    this.clipLeft = in.readInt();
    this.clipTop = in.readInt();
    this.clipWidth = in.readInt();
    this.clipHeight = in.readInt();
  }

  public static final Parcelable.Creator<PreviewPage> CREATOR =
      new Parcelable.Creator<PreviewPage>() {
        @Override
        public PreviewPage createFromParcel(Parcel source) {
          return new PreviewPage(source);
        }

        @Override
        public PreviewPage[] newArray(int size) {
          return new PreviewPage[size];
        }
      };
}
