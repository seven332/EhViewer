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

package com.hippo.ehviewer.client.result;

/*
 * Created by Hippo on 3/4/2017.
 */

import com.hippo.ehviewer.client.EhResult;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.data.FavouritesState;
import java.util.List;

/**
 * Result for {@link com.hippo.ehviewer.client.converter.FavouritesConverter}.
 */
public class FavouritesResult extends EhResult {

  private int pages;
  private List<FavouritesItem> fis;
  private FavouritesState state;

  public FavouritesResult(int pages, List<FavouritesItem> fis, FavouritesState state) {
    super(null);
    this.pages = pages;
    this.fis = fis;
    this.state = state;
  }

  /**
   * Returns favourites pages.
   */
  public int pages() {
    return pages;
  }

  /**
   * Returns FavouritesItem list.
   */
  public List<FavouritesItem> list() {
    return fis;
  }

  /**
   * Returns favourites state.
   * <p>
   * Favourites slot name, favourites count
   */
  public FavouritesState state() {
    return state;
  }


  ////////////////
  // Pain part
  ////////////////

  private FavouritesResult(Throwable t) {
    super(t);
  }

  public static FavouritesResult error(Throwable t) {
    return new FavouritesResult(t);
  }
}
