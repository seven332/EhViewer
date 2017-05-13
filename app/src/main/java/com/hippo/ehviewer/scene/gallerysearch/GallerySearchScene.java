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

package com.hippo.ehviewer.scene.gallerysearch;

/*
 * Created by Hippo on 5/12/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.scene.EhvScene;
import com.hippo.ehviewer.scene.gallerylist.GalleryListScene;

public class GallerySearchScene extends EhvScene<GallerySearchPresenter, GallerySearchView> {

  public static final String KEY_GALLERY_LIST_SCENE_ID = "GallerySearchScene:gallery_list_scene_id";

  private int galleryListSceneId;

  @Override
  protected void onCreate(@Nullable Bundle args) {
    super.onCreate(args);
    //noinspection ConstantConditions
    galleryListSceneId = args.getInt(KEY_GALLERY_LIST_SCENE_ID);
  }

  @NonNull
  @Override
  protected GallerySearchPresenter createPresenter() {
    return new GallerySearchPresenter();
  }

  @NonNull
  @Override
  protected GallerySearchView createView() {
    return new GallerySearchView();
  }

  @Override
  protected void onCreateScenePresenter(@NonNull GallerySearchPresenter presenter) {
    super.onCreateScenePresenter(presenter);
    presenter.setGalleryListSceneId(galleryListSceneId);
  }

  /**
   * Creates a {@code GallerySearchScene} instance.
   */
  public static GallerySearchScene create(@NonNull GalleryListScene galleryListScene) {
    Bundle args = new Bundle();
    args.putInt(KEY_GALLERY_LIST_SCENE_ID, galleryListScene.getId());
    GallerySearchScene scene = new GallerySearchScene();
    scene.setArgs(args);
    return scene;
  }
}
