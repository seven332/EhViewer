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
 * Created by Hippo on 1/29/2017.
 */

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.util.JsonStore;
import com.hippo.yorozuya.ObjectUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gallery Information.
 * <p>
 * There three ways to obtain a {@code GalleryInfo}.
 * <ul>
 * <li>Parsing gallery list html</li>
 * <li>Parsing gallery detail html</li>
 * <li>Gallery Metadata API</li>
 * </ul>
 * None of these methods can fill all fields.
 */
@JsonStore.Info(
    version = 1,
    name = "com.hippo.ehviewer:GalleryInfo"
)
public class GalleryInfo implements JsonStore.Item, Parcelable {

  private static final String LOG_TAG = GalleryInfo.class.getSimpleName();

  /**
   * An invalid {@code GalleryInfo}.
   */
  public static final GalleryInfo INVALID = new GalleryInfo();

  /**
   * Gallery ID.
   * <p>
   * {@code 0} for invalid.
   * <p>
   * {@code int} should be enough for a long long time.
   * But I like {@code long}.
   */
  public long gid;
  /**
   * Gallery token. Most gallery operations need it.
   * <p>
   * {@code null} for invalid
   * <p>
   * Regex:<pre>{@code
   * [0-9a-f]{10}
   * }</pre>
   * Example:<pre>{@code
   * c219d2cf41
   * }</pre>
   */
  public String token;
  /**
   * Gallery title.
   * <p>
   * May be {@code null} if user enable show jp title.
   * <p>
   * One of {@code title} and {@code titleJpn} must be non-null.
   */
  public String title;
  /**
   * Gallery title.
   * <p>
   * {@code null} if can't get it.
   * <p>
   * One of {@code title} and {@code titleJpn} must be non-null.
   */
  public String titleJpn;
  /**
   * The fingerprint of the first image.
   * <p>
   * Format:
   * {@code [sha1]-[size]-[width]-[height]-[format]}
   * <p>
   * Regex:
   * {@code [0-9a-f]{40}-\d+-\d+-\d+-[0-9a-z]+}
   * <p>
   * Example:
   * {@code 7dd3e4a62807a6938910a14407d9867b18a58a9f-2333088-2831-4015-jpg}
   * <p>
   * {@code null} if can't get it.
   */
  public String cover;
  /**
   * The url of the cover.
   * <p>
   * {@code null} if can't get it.
   */
  public String coverUrl;
  /**
   * Cover width / Cover height.
   * <p>
   * {@code -1.0f} if can't it.
   */
  public float coverRatio = -1.0f;
  /**
   * Gallery category.
   * <p>
   * {@link com.hippo.ehviewer.client.EhUtils#UNKNOWN} if can't get it.
   */
  public int category = EhUtils.UNKNOWN;
  /**
   * Posted time stamp.
   * <p>
   * {@code 0} if can't get it.
   */
  public long date;
  /**
   * Who uploads the gallery.
   * <p>
   * {@code null} if can't get it.
   */
  public String uploader;
  /**
   * Gallery Rating.
   * <p>
   * Range: {@code [0.5, 5]}
   * <p>
   * {@code 0.0f} if can't it, or if no rating temporarily.
   */
  public float rating = 0.0f;

  /**
   * Gallery Language.
   * <p>
   * {@link EhUtils#LANG_UNKNOWN} if can't get it.
   */
  public int language = EhUtils.LANG_UNKNOWN;

  /**
   * Favourite slot.
   * <p>
   * Range: {@code [-3, 9]}
   * <p>
   * {@code -3} for un-favourited.
   */
  public int favouriteSlot = EhUtils.FAV_CAT_UNKNOWN;

  /**
   * Expunged, deleted or replaced.
   */
  public boolean invalid = false;

  /**
   * The key to download archive.
   */
  public String archiverKey;

  /**
   * Gallery Pages.
   * <p>
   * {@code -1} for unknown.
   */
  public int pages = -1;

  /**
   * Gallery size in bytes.
   * <p>
   * {@code -1} for unknown.
   */
  public long size = -1;

  /**
   * Torrent count.
   * <p>
   * {@code 0} for default.
   */
  public int torrentCount = 0;

  /**
   * Gallery tags.
   * <p>
   * Default empty map.
   */
  @Nullable
  public Map<String, List<String>> tags;

  /**
   * Merges data in {@code info} to this {@code GalleryInfo}.
   */
  public void merge(GalleryInfo info) {
    if (info == null) {
      return;
    }
    if (info.gid != gid || !ObjectUtils.equals(info.token, token)) {
      Log.w(LOG_TAG, "Can't merge different GalleryInfo");
      return;
    }

    if (info.title != null) {
      title = info.title;
    }
    if (info.titleJpn != null) {
      titleJpn = info.titleJpn;
    }
    if (info.cover != null) {
      cover = info.cover;
    }
    if (info.coverUrl != null) {
      coverUrl = info.coverUrl;
    }
    if (info.coverRatio != -1.0f) {
      coverRatio = info.coverRatio;
    }
    if (info.category != EhUtils.UNKNOWN) {
      category = info.category;
    }
    if (info.date != 0) {
      date = info.date;
    }
    if (info.uploader != null) {
      uploader = info.uploader;
    }
    if (info.rating != 0.0f) {
      rating = info.rating;
    }
    if (info.language != EhUtils.LANG_UNKNOWN) {
      language = info.language;
    }
    if (info.favouriteSlot != EhUtils.FAV_CAT_UNKNOWN) {
      favouriteSlot = info.favouriteSlot;
    }
    if (info.invalid) {
      invalid = true;
    }
    if (info.archiverKey != null) {
      archiverKey = info.archiverKey;
    }
    if (info.pages != -1) {
      pages = info.pages;
    }
    if (info.size != -1) {
      size = info.size;
    }
    if (info.torrentCount != 0) {
      torrentCount = info.torrentCount;
    }
    if (info.tags != null && !info.tags.isEmpty()) {
      if (tags == null) {
        tags = new HashMap<>(info.tags.size());
      }
      tags.clear();
      tags.putAll(info.tags);
    }
  }

  /**
   * Merges the first same {@code GalleryInfo} in {@code list}.
   */
  public void merge(List<GalleryInfo> list) {
    if (list == null) {
      return;
    }
    for (GalleryInfo info: list) {
      if (info != null && info.gid == gid && ObjectUtils.equals(info.token, token)) {
        merge(info);
        return;
      }
    }
  }

  @Override
  public boolean onFetch(int version) {
    return true;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(this.gid);
    dest.writeString(this.token);
    dest.writeString(this.title);
    dest.writeString(this.titleJpn);
    dest.writeString(this.cover);
    dest.writeString(this.coverUrl);
    dest.writeFloat(this.coverRatio);
    dest.writeInt(this.category);
    dest.writeLong(this.date);
    dest.writeString(this.uploader);
    dest.writeFloat(this.rating);
    dest.writeInt(this.language);
    dest.writeInt(this.favouriteSlot);
    dest.writeByte(this.invalid ? (byte) 1 : (byte) 0);
    dest.writeString(this.archiverKey);
    dest.writeInt(this.pages);
    dest.writeLong(this.size);
    dest.writeInt(this.torrentCount);
    if (this.tags != null) {
      dest.writeInt(this.tags.size());
      for (Map.Entry<String, List<String>> entry : this.tags.entrySet()) {
        dest.writeString(entry.getKey());
        dest.writeStringList(entry.getValue());
      }
    } else {
      dest.writeInt(-1);
    }
  }

  public GalleryInfo() {
  }

  protected GalleryInfo(Parcel in) {
    this.gid = in.readLong();
    this.token = in.readString();
    this.title = in.readString();
    this.titleJpn = in.readString();
    this.cover = in.readString();
    this.coverUrl = in.readString();
    this.coverRatio = in.readFloat();
    this.category = in.readInt();
    this.date = in.readLong();
    this.uploader = in.readString();
    this.rating = in.readFloat();
    this.language = in.readInt();
    this.favouriteSlot = in.readInt();
    this.invalid = in.readByte() != 0;
    this.archiverKey = in.readString();
    this.pages = in.readInt();
    this.size = in.readLong();
    this.torrentCount = in.readInt();
    int tagsSize = in.readInt();
    if (tagsSize >= 0) {
      this.tags = new HashMap<>(tagsSize);
      for (int i = 0; i < tagsSize; i++) {
        String key = in.readString();
        List<String> value = in.createStringArrayList();
        this.tags.put(key, value);
      }
    }
  }

  public static final Parcelable.Creator<GalleryInfo> CREATOR =
      new Parcelable.Creator<GalleryInfo>() {
        @Override
        public GalleryInfo createFromParcel(Parcel source) {
          return new GalleryInfo(source);
        }

        @Override
        public GalleryInfo[] newArray(int size) {
          return new GalleryInfo[size];
        }
      };
}
