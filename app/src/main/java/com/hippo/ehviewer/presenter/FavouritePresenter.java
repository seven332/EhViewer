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
 * Created by Hippo on 3/2/2017.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import com.google.gson.Gson;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhSubscriber;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.FLUrlBuilder;
import com.hippo.ehviewer.component.FavouriteAdapter;
import com.hippo.ehviewer.component.GalleryInfoData;
import com.hippo.ehviewer.component.base.GalleryInfoAdapter;
import com.hippo.ehviewer.contract.FavouriteContract;
import com.hippo.ehviewer.presenter.base.ControllerPresenter;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.yorozuya.FileUtils;
import java.io.File;
import java.util.Map;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FavouritePresenter extends ControllerPresenter<FavouriteContract.View>
    implements FavouriteContract.Presenter {

  private GalleryData data;

  public FavouritePresenter(EhvApp app) {
    data = new GalleryData(app);
    data.restore();
  }

  @NonNull
  @Override
  public GalleryInfoAdapter attachContentLayout(Context context, ContentLayout layout) {
    layout.setPresenter(data);
    data.setView(layout);
    GalleryInfoAdapter adapter = new FavouriteAdapter(context, data);
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
  public void restore(FavouriteContract.View view) {}

  private class GalleryData extends GalleryInfoData {

    private static final String BACKUP_FILENAME = "favourites_data_backup";

    private EhClient client;
    private Gson gson;
    private EhvPreferences preferences;
    private File backupFile;

    private FLUrlBuilder builder = new FLUrlBuilder();

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
    protected void onRequireData(long id, int page) {
      builder.setPage(page);
      String url = EhUrl.getFavouritesUrl(preferences.getGallerySite());
      Map<String, String> map = builder.build();
      client.getGalleryList(url, map)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(EhSubscriber.from(
              result -> setData(id, result.galleryInfoList(), result.pages()),
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
