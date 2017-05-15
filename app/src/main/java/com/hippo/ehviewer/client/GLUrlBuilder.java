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

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 1/29/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.hippo.ehviewer.client.data.TagSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Gallery list url builder.
 */
public class GLUrlBuilder {

  private int page;
  private int category = EhUtils.CATEGORY_NONE;
  private int language = EhUtils.LANG_UNKNOWN;
  private String keyword;
  private final TagSet tagSet = new TagSet();

  /**
   * Set this GLUrlBuilder the same as that GLUrlBuilder.
   */
  public void set(GLUrlBuilder builder) {
    if (this != builder) {
      this.page = builder.page;
      this.category = builder.category;
      this.language = builder.language;
      this.keyword = builder.keyword;
      this.tagSet.set(builder.tagSet);
    }
  }

  /**
   * Set page of the gallery list.
   * <p>
   * 0-base.
   */
  public void setPage(int page) {
    this.page = page;
  }

  /**
   * Set category of the gallery list.
   *
   * @see #getCategory()
   */
  public void setCategory(int category) {
    this.category = category;
  }

  /**
   * Returns the category.
   *
   * @see #setCategory(int)
   */
  public int getCategory() {
    return category;
  }

  /**
   * Set keyword of the gallery list.
   * <p>
   * {@code null} for no keyword.
   *
   * @see #getKeyword()
   */
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  /**
   * Returns the keyword.
   *
   * @see #setKeyword(String)
   */
  public String getKeyword() {
    return this.keyword;
  }

  /**
   * Sets language for the gallery list.
   *
   * @see #getLanguage()
   */
  public void setLanguage(int language) {
    this.language = language;
  }

  /**
   * Returns the language.
   *
   * @see #setLanguage(int)
   */
  public int getLanguage() {
    return language;
  }

  /**
   * Add a tag.
   *
   * @param tag no quotation marks, no dollar sign
   */
  public void addTag(@NonNull String namespace, @NonNull String tag) {
    tagSet.add(namespace, tag);
  }

  /**
   * Returns {@code true} if it contains a least one tag.
   */
  public int getTagCount() {
    return tagSet.size();
  }

  /**
   * Returns the first tag.
   * Returns {@code null} if no tags.
   */
  @Nullable
  public String getFirstTag() {
    Iterator<Map.Entry<String, Set<String>>> iterator = tagSet.iterator();
    if (iterator.hasNext()) {
      Map.Entry<String, Set<String>> entry = iterator.next();
      return entry.getValue() + ":" + entry.getValue().iterator().next();
    } else {
      return null;
    }
  }

  private void appendTag(StringBuilder sb, String namespace, String tag) {
    if (sb.length() != 0) {
      sb.append(' ');
    }
    sb.append(namespace).append(":\"").append(tag).append("$\"");
  }

  // Combine keyword, language and tags
  @Nullable
  private String resolveKeyword() {
    StringBuilder sb = new StringBuilder();

    if (!TextUtils.isEmpty(keyword)) {
      sb.append(keyword);
    }

    String language = EhUtils.getLangText(this.language);
    if (language != null) {
      appendTag(sb, "language", language);
    }

    for (Map.Entry<String, Set<String>> entry : tagSet) {
      String namespace = entry.getKey();
      for (String tag : entry.getValue()) {
        appendTag(sb, namespace, tag);
      }
    }

    return sb.length() != 0 ? sb.toString() : null;
  }

  /**
   * Build query map
   */
  public Map<String, String> build() {
    Map<String, String> map = new HashMap<>();
    // True if need ("f_apply", "Apply+Filter")
    boolean filter = false;

    // Category
    if (category != EhUtils.CATEGORY_NONE) {
      if ((category & EhUtils.CATEGORY_DOUJINSHI) != 0) map.put("f_doujinshi", "on");
      if ((category & EhUtils.CATEGORY_MANGA) != 0) map.put("f_manga", "on");
      if ((category & EhUtils.CATEGORY_ARTIST_CG) != 0) map.put("f_artistcg", "on");
      if ((category & EhUtils.CATEGORY_GAME_CG) != 0) map.put("f_gamecg", "on");
      if ((category & EhUtils.CATEGORY_WESTERN) != 0) map.put("f_western", "on");
      if ((category & EhUtils.CATEGORY_NON_H) != 0) map.put("f_non-h", "on");
      if ((category & EhUtils.CATEGORY_IMAGE_SET) != 0) map.put("f_imageset", "on");
      if ((category & EhUtils.CATEGORY_COSPLAY) != 0) map.put("f_cosplay", "on");
      if ((category & EhUtils.CATEGORY_ASIAN_PORN) != 0) map.put("f_asianporn", "on");
      if ((category & EhUtils.CATEGORY_MISC) != 0) map.put("f_misc", "on");
      filter = true;
    }

    // Keyword
    String keyword = resolveKeyword();
    if (keyword != null) {
      map.put("f_search", keyword);
      filter = true;
    }

    // Page
    if (page != 0) {
      map.put("page", Integer.toString(page));
    }

    // TODO Advance

    // Add filter foot
    if (filter) {
      map.put("f_apply", "Apply Filter");
    }

    return map;
  }
}
