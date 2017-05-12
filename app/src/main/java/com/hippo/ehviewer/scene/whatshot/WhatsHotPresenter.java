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

package com.hippo.ehviewer.scene.whatshot;

/*
 * Created by Hippo on 2/24/2017.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import com.google.gson.Gson;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhSubscriber;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.component.GalleryInfoAdapter;
import com.hippo.ehviewer.component.GalleryInfoData;
import com.hippo.ehviewer.presenter.EhvPresenter;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.yorozuya.FileUtils;
import java.io.File;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class WhatsHotPresenter extends EhvPresenter<WhatsHotContract.View>
    implements WhatsHotContract.Presenter {

  private GalleryData data;

  @Override
  protected void onCreate() {
    super.onCreate();
    data = new GalleryData(getEhvApp());
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

  public GalleryInfo getGalleryInfo(int index) {
    return data.get(index);
  }

  private static class GalleryData extends GalleryInfoData {

    private static final String BACKUP_FILENAME = "whats_hot_data_backup";

    private EhClient client;
    private Gson gson;
    private EhvPreferences preferences;
    private File backupFile;

    public GalleryData(EhvApp app) {
      client = app.getEhClient();
      gson = app.getGson();
      preferences = app.getPreferences();

      // Get backup file
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
    protected File getBackupFile() {
      return backupFile;
    }

    @Override
    protected Gson getGson() {
      return gson;
    }
  }
}
