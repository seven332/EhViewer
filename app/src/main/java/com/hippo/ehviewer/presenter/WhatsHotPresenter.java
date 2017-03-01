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

package com.hippo.ehviewer.presenter;

/*
 * Created by Hippo on 2/24/2017.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import com.google.gson.Gson;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhSubscriber;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.component.WhatsHotAdapter;
import com.hippo.ehviewer.component.base.GalleryInfoAdapter;
import com.hippo.ehviewer.contract.WhatsHotContract;
import com.hippo.ehviewer.presenter.base.ControllerPresenter;
import com.hippo.ehviewer.reactivex.Catcher;
import com.hippo.ehviewer.reactivex.Thrower1;
import com.hippo.ehviewer.util.JsonStore;
import com.hippo.ehviewer.widget.ContentData;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.yorozuya.FileUtils;
import java.io.File;
import java.util.List;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Actions;
import rx.schedulers.Schedulers;

public class WhatsHotPresenter extends ControllerPresenter<WhatsHotContract.View>
    implements WhatsHotContract.Presenter {

  private GalleryData data;
  private Gson gson;

  public WhatsHotPresenter(EhvApp app) {
    data = new GalleryData(app);
    gson = app.getGson();

    data.setRemoveDuplicates(true);
    data.restore();
  }

  @NonNull
  @Override
  public GalleryInfoAdapter attachContentLayout(Context context, ContentLayout layout) {
    layout.setPresenter(data);
    data.setView(layout);
    GalleryInfoAdapter adapter = new WhatsHotAdapter(context, data);
    layout.setAdapter(adapter);
    return adapter;
  }

  @Override
  public void detachContentLayout(ContentLayout layout) {
    layout.setPresenter(null);
    data.setView(null);
    RecyclerView.Adapter adapter = layout.getAdapter();
    if (adapter instanceof GalleryInfoAdapter) {
      ((GalleryInfoAdapter) adapter).solidify();
    }
  }

  @Override
  public void restore(WhatsHotContract.View view) {}

  private class GalleryData extends ContentData<GalleryInfo> {

    private static final String BACKUP_FILENAME = "whats_hot_data_backup";

    private EhClient client;
    private EhvPreferences preferences;
    private File backupFile;

    public GalleryData(EhvApp app) {
      client = app.getEhClient();
      preferences = app.getPreferences();

      File dir = app.getCacheDir();
      FileUtils.ensureDir(dir);
      backupFile = new File(dir, BACKUP_FILENAME);
    }

    @Override
    public void onRequireData(final long id, int page) {
      client.getWhatsHot(preferences.getGallerySite())
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(EhSubscriber.from(
              result -> setData(id, result.galleryInfoList(), 1),
              e -> setError(id, e)
          ));
    }

    @Override
    protected void onRestoreData(long id) {
      Observable.just(backupFile)
          .map(Thrower1.from(file -> JsonStore.fetchList(gson, file, GalleryInfo.class)))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(list -> {
            setData(id, list, 1);
          }, Catcher.from(e -> setError(id, e)));
    }

    @Override
    protected void onBackupData(List<GalleryInfo> data) {
      Observable.just(data)
          .map(Thrower1.from(d -> {
            JsonStore.push(gson, backupFile, d, GalleryInfo.class);
            return true;
          }))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(Actions.empty(), Actions.empty());
    }

    @Override
    protected boolean isDuplicate(@Nullable GalleryInfo t1, @Nullable GalleryInfo t2) {
      if (t1 == null && t2 == null) {
        return true;
      }
      if (t1 != null && t2 != null) {
        return t1.gid == t2.gid;
      }
      return false;
    }
  }
}
