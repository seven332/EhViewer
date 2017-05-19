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
import android.support.annotation.NonNull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A set which stores multiple tags.
 */
public final class TagSet implements Iterable<Map.Entry<String, Set<String>>>, Parcelable {

  private Map<String, Set<String>> groups = new HashMap<>();

  /**
   * Put a tag to this set.
   */
  public void add(@NonNull String namespace, @NonNull String tag) {
    Set<String> set = groups.get(namespace);
    if (set == null) {
      set = new HashSet<>();
      groups.put(namespace, set);
    }
    // Don't add duplicate tag
    if (!set.contains(tag)) {
      set.add(tag);
    }
  }

  /**
   * Remove a tag from this set.
   */
  public void remove(@NonNull String namespace, @NonNull String tag) {
    Set<String> set = groups.get(namespace);
    if (set != null) {
      set.remove(tag);
      // Remove the namespace if it's empty
      if (set.isEmpty()) {
        groups.remove(namespace);
      }
    }
  }

  public void clear() {
    groups.clear();
  }

  /**
   * Make this set the same as another set.
   */
  public void set(@NonNull TagSet tagSet) {
    groups.clear();
    for (Map.Entry<String, Set<String>> entry : tagSet) {
      groups.put(entry.getKey(), new HashSet<>(entry.getValue()));
    }
  }

  /**
   * Returns the size of tags.
   */
  public int size() {
    int sum = 0;
    for (Set<String> set : groups.values()) {
      sum += set.size();
    }
    return sum;
  }

  /**
   * Returns {@code true} if no tags.
   */
  public boolean isEmpty() {
    return groups.isEmpty();
  }

  @Override
  public Iterator<Map.Entry<String, Set<String>>> iterator() {
    return groups.entrySet().iterator();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TagSet && groups.equals(((TagSet) obj).groups);
  }

  @Override
  public int hashCode() {
    return groups.hashCode();
  }

  @Override
  public String toString() {
    return "TagSet: " + groups.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(groups.size());
    for (Map.Entry<String, Set<String>> entry : groups.entrySet()) {
      dest.writeString(entry.getKey());

      Set<String> set = entry.getValue();
      dest.writeInt(set.size());
      for (String tag: set) {
        dest.writeString(tag);
      }
    }
  }

  public TagSet() {}

  protected TagSet(Parcel in) {
    int groupSize = in.readInt();
    groups = new HashMap<>(groupSize);
    for (int i = 0; i < groupSize; ++i) {
      String key = in.readString();

      int tagSize = in.readInt();
      Set<String> set = new HashSet<>(tagSize);
      for (int j = 0; j < tagSize; ++j) {
        set.add(in.readString());
      }
      groups.put(key, set);
    }
  }

  public static final Parcelable.Creator<TagSet> CREATOR = new Parcelable.Creator<TagSet>() {
    @Override
    public TagSet createFromParcel(Parcel source) {
      return new TagSet(source);
    }

    @Override
    public TagSet[] newArray(int size) {
      return new TagSet[size];
    }
  };
}
