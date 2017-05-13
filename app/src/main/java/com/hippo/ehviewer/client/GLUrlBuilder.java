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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gallery list url builder.
 */
public class GLUrlBuilder {

  private int page;
  private int category = EhUtils.NONE;
  private int language = EhUtils.LANG_UNKNOWN;
  private String keyword;
  @Nullable
  private List<String> namespaces;
  @Nullable
  private List<String> tags;

  /**
   * Set this GLUrlBuilder the same as that GLUrlBuilder.
   */
  public void set(GLUrlBuilder builder) {
    if (this != builder) {
      this.page = builder.page;
      this.category = builder.category;
      this.language = builder.language;
      this.keyword = builder.keyword;

      if (this.namespaces != null) {
        this.namespaces.clear();
      }
      if (builder.namespaces != null) {
        if (this.namespaces == null) {
          this.namespaces = new ArrayList<>();
        }
        this.namespaces.addAll(builder.namespaces);
      }

      if (this.tags != null) {
        this.tags.clear();
      }
      if (builder.tags != null) {
        if (this.tags == null) {
          this.tags = new ArrayList<>();
        }
        this.tags.addAll(builder.tags);
      }
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
   */
  public void setCategory(int category) {
    this.category = category;
  }

  /**
   * Returns the category.
   */
  public int getCategory() {
    return category;
  }

  /**
   * Set keyword of the gallery list.
   * <p>
   * {@code null} for no keyword.
   */
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  /**
   * Sets language for the gallery list.
   */
  public void setLanguage(int language) {
    this.language = language;
  }

  /**
   * Add a tag.
   *
   * @param tag no quotation marks, no dollar sign
   */
  public void addTag(@NonNull String namespace, @NonNull String tag) {
    if (namespaces == null) {
      namespaces = new ArrayList<>();
    }
    if (tags == null) {
      tags = new ArrayList<>();
    }
    namespaces.add(namespace);
    tags.add(tag);
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

    if (namespaces != null && tags != null) {
      for (int i = 0, n = Math.min(namespaces.size(), tags.size()); i < n; ++i) {
        appendTag(sb, namespaces.get(i), tags.get(i));
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
    if (category != EhUtils.NONE) {
      if ((category & EhUtils.DOUJINSHI) != 0) map.put("f_doujinshi", "on");
      if ((category & EhUtils.MANGA) != 0) map.put("f_manga", "on");
      if ((category & EhUtils.ARTIST_CG) != 0) map.put("f_artistcg", "on");
      if ((category & EhUtils.GAME_CG) != 0) map.put("f_gamecg", "on");
      if ((category & EhUtils.WESTERN) != 0) map.put("f_western", "on");
      if ((category & EhUtils.NON_H) != 0) map.put("f_non-h", "on");
      if ((category & EhUtils.IMAGE_SET) != 0) map.put("f_imageset", "on");
      if ((category & EhUtils.COSPLAY) != 0) map.put("f_cosplay", "on");
      if ((category & EhUtils.ASIAN_PORN) != 0) map.put("f_asianporn", "on");
      if ((category & EhUtils.MISC) != 0) map.put("f_misc", "on");
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
