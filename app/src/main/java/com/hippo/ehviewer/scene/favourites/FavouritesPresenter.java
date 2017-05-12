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

package com.hippo.ehviewer.scene.favourites;

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
import com.hippo.ehviewer.client.FLUrlBuilder;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.component.GalleryInfoAdapter;
import com.hippo.ehviewer.presenter.EhvPresenter;
import com.hippo.ehviewer.widget.ContentData;
import com.hippo.ehviewer.widget.ContentLayout;
import java.util.List;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FavouritesPresenter extends EhvPresenter<FavouritesContract.View>
    implements FavouritesContract.Presenter {

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
    //GalleryInfoAdapter adapter = new FavouritesAdapter(context, data);
    //layout.setAdapter(adapter);
    //return adapter;
    return null;
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

  private class GalleryData extends ContentData<FavouritesItem> {

    private EhClient client;
    private Gson gson;
    private EhvPreferences preferences;

    private FLUrlBuilder builder = new FLUrlBuilder();

    public GalleryData(EhvApp app) {
      client = app.getEhClient();
      gson = app.getGson();
      preferences = app.getPreferences();
    }

    @Override
    protected void onRequireData(long id, int page) {



    }

    @Override
    protected void onRestoreData(long id) {
      Observable.just(null)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(o -> setError(id, new Exception()));
    }

    @Override
    protected void onBackupData(List<FavouritesItem> data) {}
  }
}
