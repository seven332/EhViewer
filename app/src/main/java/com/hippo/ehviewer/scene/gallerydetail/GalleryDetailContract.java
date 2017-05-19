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

import com.hippo.ehviewer.client.data.CommentEntry;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.PreviewPage;
import com.hippo.ehviewer.presenter.EhvPresenter;
import com.hippo.ehviewer.presenter.PresenterInterface;
import com.hippo.ehviewer.scene.signin.SignInScene;
import com.hippo.ehviewer.view.ViewInterface;
import java.util.List;

public interface GalleryDetailContract {

  interface Presenter extends PresenterInterface<View> {

    void getGalleryDetail(GalleryInfo info);
  }

  interface View extends ViewInterface {

    void onGetGalleryDetailNone();

    void onGetGalleryDetailStart();

    void onGetGalleryDetailSuccess(GalleryInfo info, List<CommentEntry> comments,
        List<PreviewPage> previews);

    void onGetGalleryDetailFailure(Throwable e);
  }

  abstract class AbsPresenter extends EhvPresenter<View, SignInScene> implements Presenter, View {

    @Override
    public void onGetGalleryDetailNone() {
      View view = getView();
      if (view != null) {
        view.onGetGalleryDetailNone();
      }
    }

    @Override
    public void onGetGalleryDetailStart() {
      View view = getView();
      if (view != null) {
        view.onGetGalleryDetailStart();
      }
    }

    @Override
    public void onGetGalleryDetailSuccess(GalleryInfo info, List<CommentEntry> comments,
        List<PreviewPage> previews) {
      View view = getView();
      if (view != null) {
        view.onGetGalleryDetailSuccess(info, comments, previews);
      }
    }

    @Override
    public void onGetGalleryDetailFailure(Throwable e) {
      View view = getView();
      if (view != null) {
        view.onGetGalleryDetailFailure(e);
      }
    }
  }
}
