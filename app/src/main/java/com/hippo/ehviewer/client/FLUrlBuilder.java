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

import com.hippo.yorozuya.StringUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * Favourite list url builder.
 */
public class FLUrlBuilder {

  private int page;
  private int favCat = EhUtils.FAV_CAT_ALL;
  private String keyword;

  /**
   * Set page of the favourite list.
   * <p>
   * 0-base.
   */
  public void setPage(int page) {
    this.page = page;
  }

  /**
   * Set category of the favourite list.
   */
  public void setFavCat(int favCat) {
    if (favCat > EhUtils.FAV_CAT_MAX) {
      favCat = EhUtils.FAV_CAT_MAX;
    } else if (favCat < EhUtils.FAV_CAT_ALL) {
      favCat = EhUtils.FAV_CAT_ALL;
    }
    this.favCat = favCat;
  }

  /**
   * Set keyword of the favourite list.
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

    // Page
    if (page != 0) {
      map.put("page", Integer.toString(page));
    }

    // FavCat
    if (favCat == EhUtils.FAV_CAT_ALL) {
      map.put("favcat", "all");
    } else {
      map.put("favcat", Integer.toString(favCat));
    }

    // keyword
    if (!StringUtils.isEmpty(keyword)) {
      map.put("f_search", keyword);
      map.put("f_apply", "Search Favorites");
    }

    return map;
  }
}
