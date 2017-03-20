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
 * Created by Hippo on 3/4/2017.
 */

import android.support.annotation.Nullable;

/**
 * The item of favourites.
 */
public class FavouritesItem implements GalleryInfoContainer {

  /**
   * The {@link GalleryInfo} of this {@code FavouritesItem}.
   * <p>
   * If it's {@code null}, this {@code FavouritesItem} is invalid.
   */
  @Nullable
  public GalleryInfo info;

  /**
   * The note that user input.
   */
  public String note;

  /**
   * Favourited date.
   * <p>
   * 0 if can't get it.
   */
  public long date;

  @Override
  public GalleryInfo getGalleryInfo() {
    return info;
  }

  @Override
  public void setGalleryInfo(GalleryInfo info) {
    this.info = info;
  }
}
