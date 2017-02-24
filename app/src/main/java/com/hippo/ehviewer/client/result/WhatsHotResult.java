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
 * Created by Hippo on 2/24/2017.
 */

import com.hippo.ehviewer.client.EhResult;
import com.hippo.ehviewer.client.data.GalleryInfo;
import java.util.List;

/**
 * A result of whats hot, contains GalleryInfo list.
 */
public class WhatsHotResult extends EhResult {

  private List<GalleryInfo> gis;

  public WhatsHotResult(List<GalleryInfo> gis) {
    super(null);
    this.gis = gis;
  }

  public List<GalleryInfo> galleryInfoList() {
    return gis;
  }

  ////////////////
  // Pain part
  ////////////////

  private WhatsHotResult(Throwable t) {
    super(t);
  }

  public static WhatsHotResult error(Throwable t) {
    return new WhatsHotResult(t);
  }
}
