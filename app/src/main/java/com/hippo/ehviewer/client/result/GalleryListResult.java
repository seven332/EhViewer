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
 * Created by Hippo on 1/29/2017.
 */

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.result.base.GalleryInfoListResult;
import java.util.List;

public class GalleryListResult extends GalleryInfoListResult {

  private int pages;

  public GalleryListResult(int pages, List<GalleryInfo> gis) {
    super(gis);
    this.pages = pages;
  }

  public int pages() {
    return pages;
  }


  ////////////////
  // Pain part
  ////////////////

  protected GalleryListResult(Throwable t) {
    super(t);
  }

  public static GalleryListResult error(Throwable t) {
    return new GalleryListResult(t);
  }
}
