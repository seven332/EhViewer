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

package com.hippo.ehviewer;

/*
 * Created by Hippo on 3/3/2017.
 */

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.dao.FavouritesItemMapping;
import com.hippo.ehviewer.dao.GalleryInfoContainerMapping;
import com.hippo.ehviewer.database.MSQLite;
import com.hippo.ehviewer.database.MSQLiteOpenHelper;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite;
import com.pushtorefresh.storio.sqlite.operations.put.PutResult;
import com.pushtorefresh.storio.sqlite.queries.Query;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import rx.Observable;

public class EhvDB {

  private static final int VERSION = 1;

  @StringDef({FAVOURITES_ITEM_NOTE, FAVOURITES_ITEM_DATE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface FavouritesItemOrder {}

  public static final String FAVOURITES_ITEM_NOTE = FavouritesItemMapping.COLUMN_NOTE;
  public static final String FAVOURITES_ITEM_DATE = FavouritesItemMapping.COLUMN_DATE;

  private StorIOSQLite sql;

  public EhvDB(EhvApp app, String dbName) {
    sql = DefaultStorIOSQLite.builder()
        .sqliteOpenHelper(new EhvDBOpenHelper(app, dbName, VERSION))
        .addTypeMapping(FavouritesItem.class, new FavouritesItemMapping(app.getGson()))
        .build();
  }

  /**
   * Puts a {@link FavouritesItem} to database.
   */
  @NonNull
  public Observable<PutResult> putFavouritesItem(FavouritesItem item) {
    if (item != null) {
      return sql.put()
          .object(item)
          .prepare()
          .asRxObservable();
    } else {
      return Observable.error(new NullPointerException("item == null"));
    }
  }

  /**
   * Gets a {@link FavouritesItem} to database.
   */
  @Nullable
  public FavouritesItem getFavouritesItem(int gid) {
    return sql.get()
        .object(FavouritesItem.class)
        .withQuery(Query.builder()
            .table(FavouritesItemMapping.TABLE_NAME)
            .where(GalleryInfoContainerMapping.COLUMN_GID + " = ?")
            .whereArgs(gid)
            .build()
        )
        .prepare()
        .executeAsBlocking();
  }

  /**
   * Gets favourites.
   */
  public List<FavouritesItem> getFavourites(
      @FavouritesItemOrder String order,
      @IntRange(from = 1) int pageSize,
      @IntRange(from = 0) int page
  ) {
    return sql.get()
        .listOfObjects(FavouritesItem.class)
        .withQuery(Query.builder()
            .table(FavouritesItemMapping.TABLE_NAME)
            .orderBy(order)
            .limit(page * pageSize, pageSize)
            .build()
        )
        .prepare()
        .executeAsBlocking();
  }

  private static class EhvDBOpenHelper extends MSQLiteOpenHelper {

    public EhvDBOpenHelper(Context context, String name, int version) {
      super(context, name, version);
    }

    @Override
    public void onInit(MSQLite ms) {
      ms.version(1);
      GalleryInfoContainerMapping.galleryInfoTableVersion1(ms);
      FavouritesItemMapping.version1(ms);
    }
  }
}
