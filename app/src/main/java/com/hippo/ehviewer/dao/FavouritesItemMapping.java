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

package com.hippo.ehviewer.dao;

/*
 * Created by Hippo on 3/5/2017.
 */

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import com.google.gson.Gson;
import com.hippo.ehviewer.client.data.FavouritesItem;
import com.hippo.ehviewer.database.MSQLite;
import com.hippo.ehviewer.util.DBUtils;
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping;

public class FavouritesItemMapping extends SQLiteTypeMapping<FavouritesItem> {

  public static final String TABLE_NAME = "favourites";
  public static final String COLUMN_NOTE = "note";
  public static final String COLUMN_DATE = "date";

  public FavouritesItemMapping(Gson gson) {
    super(
        new FavouritesItemPutResolver(gson, TABLE_NAME),
        new FavouritesItemGetResolver(gson),
        new FavouritesItemDeleteResolver(gson, TABLE_NAME)
    );
  }

  /**
   * Table for FavouritesItem version 1.
   */
  public static void version1(MSQLite ms) {
    ms.createTable(TABLE_NAME, GalleryInfoContainerMapping.COLUMN_GID, long.class)
        .insertColumn(TABLE_NAME, COLUMN_NOTE, String.class)
        .insertColumn(TABLE_NAME, COLUMN_DATE, long.class);
  }

  private static class FavouritesItemPutResolver
      extends GalleryInfoContainerMapping.UniquePutResolver<FavouritesItem> {

    protected FavouritesItemPutResolver(@NonNull Gson gson, @NonNull String tableName) {
      super(gson, tableName);
    }

    @NonNull
    @Override
    protected ContentValues mapToContentValues(@NonNull FavouritesItem item) {
      ContentValues contentValues = new ContentValues(3);
      contentValues.put(COLUMN_NOTE, item.note);
      contentValues.put(COLUMN_DATE, item.date);
      return contentValues;
    }
  }

  private static class FavouritesItemGetResolver
      extends GalleryInfoContainerMapping.GetResolver<FavouritesItem> {

    protected FavouritesItemGetResolver(@NonNull Gson gson) {
      super(gson);
    }

    @NonNull
    @Override
    protected FavouritesItem mapFromCursor2(@NonNull Cursor cursor) {
      FavouritesItem item = new FavouritesItem();
      item.note = DBUtils.getString(cursor, COLUMN_NOTE, null);
      item.date = DBUtils.getLong(cursor, COLUMN_DATE, 0);
      return item;
    }
  }

  private static class FavouritesItemDeleteResolver
      extends GalleryInfoContainerMapping.UniqueDeleteResolver<FavouritesItem> {

    protected FavouritesItemDeleteResolver(@NonNull Gson gson, @NonNull String tableName) {
      super(gson, tableName);
    }
  }
}
