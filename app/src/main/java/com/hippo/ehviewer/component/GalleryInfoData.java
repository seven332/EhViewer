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

package com.hippo.ehviewer.component;

/*
 * Created by Hippo on 3/2/2017.
 */

import android.support.annotation.Nullable;
import com.google.gson.Gson;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.reactivex.Catcher;
import com.hippo.ehviewer.reactivex.Thrower1;
import com.hippo.ehviewer.util.JsonStore;
import com.hippo.ehviewer.widget.ContentData;
import java.io.File;
import java.util.List;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Actions;
import rx.schedulers.Schedulers;

/**
 * {@code GalleryInfoData} is a {@code ContentData} of {@link GalleryInfo}.
 */
public abstract class GalleryInfoData extends ContentData<GalleryInfo> {

  public GalleryInfoData() {
    setRemoveDuplicates(true);
  }

  /**
   * Returns the file to store backup data.
   */
  protected abstract File getBackupFile();

  /**
   * Returns the {@link Gson} to serialize and deserialize {@link GalleryInfo}.
   */
  protected abstract Gson getGson();

  @Override
  protected void onRestoreData(long id) {
    Observable.just(getBackupFile())
        .map(Thrower1.from(file -> JsonStore.fetchList(getGson(), file, GalleryInfo.class)))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            list -> setData(id, list, 1),
            Catcher.from(e -> setError(id, e))
        );
  }

  @Override
  protected void onBackupData(List<GalleryInfo> data) {
    Observable.just(data)
        .map(Thrower1.from(d -> {
          JsonStore.push(getGson(), getBackupFile(), d, GalleryInfo.class);
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
