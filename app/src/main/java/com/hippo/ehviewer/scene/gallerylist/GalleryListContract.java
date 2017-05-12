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

package com.hippo.ehviewer.scene.gallerylist;

/*
 * Created by Hippo on 2/10/2017.
 */

import com.hippo.ehviewer.client.GLUrlBuilder;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.contract.GalleryInfoContract;
import com.hippo.ehviewer.presenter.EhvPresenter;

public interface GalleryListContract {

  interface Presenter extends GalleryInfoContract.Presenter<View> {

    void applyGLUrlBuilder(GLUrlBuilder builder);

    GalleryInfo getGalleryInfo(int index);
  }

  interface View extends GalleryInfoContract.View {

    void onUpdateGLUrlBuilder(GLUrlBuilder builder);
  }

  abstract class AbsPresenter extends EhvPresenter<View> implements Presenter, View {

    @Override
    public void onUpdateGLUrlBuilder(GLUrlBuilder builder) {
      View view = getView();
      if (view != null) {
        view.onUpdateGLUrlBuilder(builder);
      }
    }
  }
}
