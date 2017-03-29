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

package com.hippo.ehviewer.scene.favourites;

/*
 * Created by Hippo on 3/4/2017.
 */

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.widget.ContentData;
import java.util.List;

// TODO
public class FavouritesItemData extends ContentData<GalleryInfo> {

  public FavouritesItemData() {
    setRemoveDuplicates(true);
  }

  @Override
  protected void onRequireData(long id, int page) {

  }

  @Override
  protected void onRestoreData(long id) {

  }

  @Override
  protected void onBackupData(List<GalleryInfo> data) {

  }
}
