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

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhSubscriber;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.result.GalleryDetailResult;
import com.hippo.ehviewer.presenter.task.ComplexTask;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class GalleryDetailPresenter extends GalleryDetailContract.AbsPresenter {

  private GetGalleryDetailTask getGalleryDetailtask;

  @Override
  protected void onCreate() {
    super.onCreate();
    getGalleryDetailtask = new GetGalleryDetailTask();
  }

  @Override
  protected void onRestore(@NonNull GalleryDetailContract.View view) {
    super.onRestore(view);

    switch (getGalleryDetailtask.getState()) {
      case ComplexTask.STATE_NONE:
        view.onGetGalleryDetailNone();
        break;
      case ComplexTask.STATE_RUNNING:
        view.onGetGalleryDetailStart();
        break;
      case ComplexTask.STATE_SUCCESS:
        GalleryDetailResult result = getGalleryDetailtask.getResult();
        view.onGetGalleryDetailSuccess(result.getGalleryInfo(), result.getComments(),
            result.getPreviews());
        break;
      case ComplexTask.STATE_FAILURE:
        view.onGetGalleryDetailFailure(getGalleryDetailtask.getError());
        break;
    }
  }

  @Override
  public void getGalleryDetail(GalleryInfo info) {
    if (getGalleryDetailtask.getState() != ComplexTask.STATE_RUNNING) {
      getGalleryDetailtask.start(info);
    }
  }

  private class GetGalleryDetailTask extends ComplexTask<GalleryInfo, Void, GalleryDetailResult> {

    @Override
    public void onStart(GalleryInfo info) {
      onGetGalleryDetailStart();

      getEhvApp().getEhClient().getGalleryDetail(EhUrl.SITE_E, info.gid, info.token)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(EhSubscriber.from(getSubscriptionSet(), this::success, this::failure));
    }

    @Override
    public void onSuccess(GalleryDetailResult result) {
      // TODO Store api uid key
      onGetGalleryDetailSuccess(result.getGalleryInfo(), result.getComments(), result.getPreviews());
    }

    @Override
    public void onFailure(Throwable e) {
      onGetGalleryDetailFailure(e);
    }
  }
}
