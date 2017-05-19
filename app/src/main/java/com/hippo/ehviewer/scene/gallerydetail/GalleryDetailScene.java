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

package com.hippo.ehviewer.scene.gallerydetail;

/*
 * Created by Hippo on 5/14/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.scene.EhvScene;

public class GalleryDetailScene extends EhvScene<GalleryDetailPresenter, GalleryDetailView> {

  private static final String KEY_GALLERY_INFO = "GalleryDetailScene:gallery_info";

  private GalleryInfo info;

  @Override
  protected void onCreate(@Nullable Bundle args) {
    super.onCreate(args);
    //noinspection ConstantConditions
    info = args.getParcelable(KEY_GALLERY_INFO);
  }

  @NonNull
  @Override
  protected GalleryDetailPresenter createPresenter() {
    return new GalleryDetailPresenter();
  }

  @NonNull
  @Override
  protected GalleryDetailView createView() {
    return new GalleryDetailView();
  }

  @Override
  protected void onCreateSceneView(@NonNull GalleryDetailView view) {
    super.onCreateSceneView(view);
    view.setGalleryInfo(info);
  }

  public static GalleryDetailScene create(@NonNull GalleryInfo info) {
    Bundle args = new Bundle();
    args.putParcelable(KEY_GALLERY_INFO, info);
    GalleryDetailScene scene = new GalleryDetailScene();
    scene.setArgs(args);
    return scene;
  }
}
