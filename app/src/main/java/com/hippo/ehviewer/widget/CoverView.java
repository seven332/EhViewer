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

package com.hippo.ehviewer.widget;

/*
 * Created by Hippo on 2/3/2017.
 */

import android.content.Context;
import android.util.AttributeSet;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.GenericDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.drawable.TextDrawable;
import com.hippo.yorozuya.android.ResourcesUtils;

/**
 * Cover.
 */
public class CoverView extends GenericDraweeView {

  private EhvPreferences preferences;

  public CoverView(Context context) {
    super(context);
    init(context);
  }

  public CoverView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public CoverView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    preferences = EhvApp.get(context).getPreferences();

    TextDrawable failure = new TextDrawable("(;´Д`)", 0.8f);
    failure.setBackgroundColor(
        ResourcesUtils.getAttrColor(context, R.attr.imageFailureBackgroundColor));
    failure.setTextColor(
        ResourcesUtils.getAttrColor(context, R.attr.imageFailureTextColor));
    getHierarchy().setFailureImage(failure);
  }

  public void load(GalleryInfo info) {
    if (info.cover != null) {
      boolean ex = preferences.getGallerySite() == EhUrl.SITE_EX;
      ImageRequest[] requests;
      int index = 0;
      int count = 3;
      if (ex) {
        count += 3;
      }
      if (info.coverUrl != null) {
        count += 1;
      }

      requests = new ImageRequest[count];
      if (ex) {
        requests[index++] = ImageRequest.fromUri(
            EhUrl.getThumbnailUrl(info.cover, EhUrl.SITE_EX, EhUrl.THUMBNAIL_TYPE_300));
      }
      requests[index++] = ImageRequest.fromUri(
          EhUrl.getThumbnailUrl(info.cover, EhUrl.SITE_E, EhUrl.THUMBNAIL_TYPE_300));
      if (ex) {
        requests[index++] = ImageRequest.fromUri(
            EhUrl.getThumbnailUrl(info.cover, EhUrl.SITE_EX, EhUrl.THUMBNAIL_TYPE_250));
      }
      requests[index++] = ImageRequest.fromUri(
          EhUrl.getThumbnailUrl(info.cover, EhUrl.SITE_E, EhUrl.THUMBNAIL_TYPE_250));
      if (ex) {
        requests[index++] = ImageRequest.fromUri(
            EhUrl.getThumbnailUrl(info.cover, EhUrl.SITE_EX, EhUrl.THUMBNAIL_TYPE_L));
      }
      requests[index++] = ImageRequest.fromUri(
          EhUrl.getThumbnailUrl(info.cover, EhUrl.SITE_E, EhUrl.THUMBNAIL_TYPE_L));
      if (info.coverUrl != null) {
        requests[index] = ImageRequest.fromUri(info.coverUrl);
      }

      DraweeController controller = Fresco.newDraweeControllerBuilder()
          .setFirstAvailableImageRequests(requests)
          .setOldController(getController())
          .build();
      setController(controller);
      return;
    }
    if (info.coverUrl != null) {
      DraweeController controller = Fresco.newDraweeControllerBuilder()
          .setImageRequest(ImageRequest.fromUri(info.coverUrl))
          .setOldController(getController())
          .build();
      setController(controller);
      return;
    }
    // TODO default image
  }
}
