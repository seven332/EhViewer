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
 * Created by Hippo on 5/15/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.client.EhResult;
import com.hippo.ehviewer.client.data.ApiUidKey;
import com.hippo.ehviewer.client.data.CommentEntry;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.PreviewPage;
import java.util.List;

public class GalleryDetailResult extends EhResult {

  private GalleryInfo info;
  private List<CommentEntry> comments;
  private List<PreviewPage> previews;
  private ApiUidKey apiUidKey;

  public GalleryDetailResult(@NonNull GalleryInfo info, List<CommentEntry> comments,
      List<PreviewPage> previews, ApiUidKey apiUidKey) {
    super(null);
    this.info = info;
    this.comments = comments;
    this.previews = previews;
    this.apiUidKey = apiUidKey;
  }

  public GalleryInfo getGalleryInfo() {
    return info;
  }

  @Nullable
  public List<CommentEntry> getComments() {
    return comments;
  }

  @Nullable
  public List<PreviewPage> getPreviews() {
    return previews;
  }

  @Nullable
  public ApiUidKey getApiUidKey() {
    return apiUidKey;
  }


  ////////////////
  // Pain part
  ////////////////

  private GalleryDetailResult(Throwable t) {
    super(t);
  }

  public static GalleryDetailResult error(Throwable t) {
    return new GalleryDetailResult(t);
  }
}
