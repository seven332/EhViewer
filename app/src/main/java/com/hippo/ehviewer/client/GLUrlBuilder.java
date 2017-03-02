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

import java.util.HashMap;
import java.util.Map;

/**
 * Gallery list url builder.
 */
public class GLUrlBuilder {

  private int page;
  private int category;
  private String keyword;

  /**
   * Set this GLUrlBuilder the same as that GLUrlBuilder.
   */
  public void set(GLUrlBuilder builder) {
    this.page = builder.page;
    this.category = builder.category;
    this.keyword = builder.keyword;
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
