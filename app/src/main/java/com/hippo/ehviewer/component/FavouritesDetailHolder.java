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

package com.hippo.ehviewer.component;

/*
 * Created by Hippo on 3/4/2017.
 */

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.component.base.GalleryInfoDetailHolder;

public class FavouritesDetailHolder extends GalleryInfoDetailHolder<FavouritesItem> {

  private FavouritesDetailHolder(View view) {
    super(view);
  }

  @Override
  public void bind(FavouritesItem item) {
    bindGalleryInfo(item.info != null ? item.info : GalleryInfo.INVALID);
    bindNote(item.note);
  }

  public static FavouritesDetailHolder create(LayoutInflater inflater, ViewGroup container) {
    return new FavouritesDetailHolder(createView(inflater, container));
  }
}
