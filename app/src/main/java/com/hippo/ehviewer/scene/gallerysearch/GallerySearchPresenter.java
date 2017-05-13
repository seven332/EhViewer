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

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.GLUrlBuilder;
import com.hippo.ehviewer.presenter.EhvPresenter;
import com.hippo.ehviewer.scene.gallerylist.GalleryListScene;
import com.hippo.stage.Stage;

public class GallerySearchPresenter extends EhvPresenter<GallerySearchContract.View>
    implements GallerySearchContract.Presenter {

  private int galleryListSceneId;

  void setGalleryListSceneId(int galleryListSceneId) {
    this.galleryListSceneId = galleryListSceneId;
  }

  @Override
  public void commitGLUrlBuilder(@NonNull GLUrlBuilder builder) {
    Stage stage = getEhvScene().getStage();
    if (stage != null) {
      GalleryListScene scene = (GalleryListScene) stage.findSceneById(galleryListSceneId);
      if (scene != null) {
        scene.applyGLUrlBuilder(builder);
      }
    }
  }
}
