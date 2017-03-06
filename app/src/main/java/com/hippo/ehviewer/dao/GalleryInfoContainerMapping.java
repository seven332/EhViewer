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
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryInfoContainer;
import com.hippo.ehviewer.database.MSQLite;
import com.hippo.ehviewer.util.DBUtils;
import com.pushtorefresh.storio.internal.InternalQueries;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.pushtorefresh.storio.sqlite.operations.delete.DeleteResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResult;
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery;
import com.pushtorefresh.storio.sqlite.queries.InsertQuery;
import com.pushtorefresh.storio.sqlite.queries.Query;
import com.pushtorefresh.storio.sqlite.queries.RawQuery;
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery;
import java.util.List;
import java.util.Map;

/**
 * {@code GalleryInfoContainerMapping} can map {@link GalleryInfoContainer}.
 * Please extend {@link PutResolver}, {@link GetResolver} and {@link DeleteResolver}.
 * <p>
 * Must contain a {@link #COLUMN_GID} column in item table to store {@link GalleryInfo#gid}.
 * <p>
 * If use {@link GalleryInfo} as primary key, please extend {@link UniquePutResolver}
 * and {@link UniqueDeleteResolver}.
 */
@SuppressWarnings("TryFinallyCanBeTryWithResources")
public final class GalleryInfoContainerMapping {
  private GalleryInfoContainerMapping() {}

  private static final String LOG_TAG = GalleryInfoContainerMapping.class.getSimpleName();

  @VisibleForTesting
  static final String TABLE_NAME = "gallery_info";
  public static final String COLUMN_GID = "gid";
  private static final String COLUMN_TOKEN = "token";
  private static final String COLUMN_TITLE = "title";
  private static final String COLUMN_TITLE_JPN = "title_jpn";
  private static final String COLUMN_COVER = "cover";
  private static final String COLUMN_COVER_URL = "cover_url";
  private static final String COLUMN_COVER_RATIO = "cover_ratio";
  private static final String COLUMN_CATEGORY = "category";
  private static final String COLUMN_DATE = "date";
  private static final String COLUMN_UPLOADER = "uploader";
  private static final String COLUMN_RATING = "rating";
  private static final String COLUMN_LANGUAGE = "language";
  private static final String COLUMN_FAVOURITE_SLOT = "favourite_slot";
  private static final String COLUMN_INVALID = "invalid";
  private static final String COLUMN_PAGES = "pages";
  private static final String COLUMN_SIZE = "size";
  private static final String COLUMN_TORRENT_COUNT = "torrent_count";
  private static final String COLUMN_TAGS = "tags";
  @VisibleForTesting
  static final String COLUMN_REFERENCE = "reference";

  /**
   * GalleryInfo table version 1.
   */
  public static void galleryInfoTableVersion1(MSQLite ms) {
    ms.createTable(TABLE_NAME, COLUMN_GID, long.class)
        .insertColumn(TABLE_NAME, COLUMN_TOKEN, String.class)
        .insertColumn(TABLE_NAME, COLUMN_TITLE, String.class)
        .insertColumn(TABLE_NAME, COLUMN_TITLE_JPN, String.class)
        .insertColumn(TABLE_NAME, COLUMN_COVER, String.class)
        .insertColumn(TABLE_NAME, COLUMN_COVER_URL, String.class)
        .insertColumn(TABLE_NAME, COLUMN_COVER_RATIO, float.class)
        .insertColumn(TABLE_NAME, COLUMN_CATEGORY, int.class)
        .insertColumn(TABLE_NAME, COLUMN_DATE, long.class)
        .insertColumn(TABLE_NAME, COLUMN_UPLOADER, String.class)
        .insertColumn(TABLE_NAME, COLUMN_RATING, float.class)
        .insertColumn(TABLE_NAME, COLUMN_LANGUAGE, int.class)
        .insertColumn(TABLE_NAME, COLUMN_FAVOURITE_SLOT, int.class)
        .insertColumn(TABLE_NAME, COLUMN_INVALID, boolean.class)
        .insertColumn(TABLE_NAME, COLUMN_PAGES, int.class)
        .insertColumn(TABLE_NAME, COLUMN_SIZE, long.class)
        .insertColumn(TABLE_NAME, COLUMN_TORRENT_COUNT, int.class)
        .insertColumn(TABLE_NAME, COLUMN_TAGS, String.class)
        .insertColumn(TABLE_NAME, COLUMN_REFERENCE, int.class);
  }

  private static ContentValues mapGalleryInfoToContentValues(@NonNull Gson gson,
      @NonNull GalleryInfo info, int reference) {
    ContentValues contentValues = new ContentValues(19);
    contentValues.put(COLUMN_GID, info.gid);
    contentValues.put(COLUMN_TOKEN, info.token);
    contentValues.put(COLUMN_TITLE, info.title);
    contentValues.put(COLUMN_TITLE_JPN, info.titleJpn);
    contentValues.put(COLUMN_COVER, info.cover);
    contentValues.put(COLUMN_COVER_URL, info.coverUrl);
    contentValues.put(COLUMN_COVER_RATIO, info.coverRatio);
    contentValues.put(COLUMN_CATEGORY, info.category);
    contentValues.put(COLUMN_DATE, info.date);
    contentValues.put(COLUMN_UPLOADER, info.uploader);
    contentValues.put(COLUMN_RATING, info.rating);
    contentValues.put(COLUMN_LANGUAGE, info.language);
    contentValues.put(COLUMN_FAVOURITE_SLOT, info.favouriteSlot);
    contentValues.put(COLUMN_INVALID, info.invalid);
    contentValues.put(COLUMN_PAGES, info.pages);
    contentValues.put(COLUMN_SIZE, info.size);
    contentValues.put(COLUMN_TORRENT_COUNT, info.torrentCount);
    contentValues.put(COLUMN_TAGS, gson.toJson(info.tags));
    contentValues.put(COLUMN_REFERENCE, reference);
    return contentValues;
  }

  @NonNull
  private static GalleryInfo mapGalleryInfoFromCursor(@NonNull Cursor cursor, @NonNull Gson gson) {
    GalleryInfo info = new GalleryInfo();
    info.gid = DBUtils.getLong(cursor, COLUMN_GID, 0);
    info.token = DBUtils.getString(cursor, COLUMN_TOKEN, null);
    info.title = DBUtils.getString(cursor, COLUMN_TITLE, null);
    info.titleJpn = DBUtils.getString(cursor, COLUMN_TITLE_JPN, null);
    info.cover = DBUtils.getString(cursor, COLUMN_COVER, null);
    info.coverUrl = DBUtils.getString(cursor, COLUMN_COVER_URL, null);
    info.coverRatio = DBUtils.getFloat(cursor, COLUMN_COVER_RATIO, -1.0f);
    info.category = DBUtils.getInt(cursor, COLUMN_CATEGORY, EhUtils.UNKNOWN);
    info.date = DBUtils.getLong(cursor, COLUMN_DATE, 0);
    info.uploader = DBUtils.getString(cursor, COLUMN_UPLOADER, null);
    info.rating = DBUtils.getFloat(cursor, COLUMN_RATING, 0.0f);
    info.language = DBUtils.getInt(cursor, COLUMN_LANGUAGE, EhUtils.LANG_UNKNOWN);
    info.favouriteSlot = DBUtils.getInt(cursor, COLUMN_FAVOURITE_SLOT, EhUtils.FAV_CAT_UNKNOWN);
    info.invalid = DBUtils.getBoolean(cursor, COLUMN_INVALID, false);
    info.pages = DBUtils.getInt(cursor, COLUMN_PAGES, -1);
    info.size = DBUtils.getLong(cursor, COLUMN_SIZE, -1);
    info.torrentCount = DBUtils.getInt(cursor, COLUMN_TORRENT_COUNT, 0);
    // Tags
    String tags = DBUtils.getString(cursor, COLUMN_TAGS, null);
    if (tags != null) {
      info.tags = gson.fromJson(tags, new TypeToken<Map<String, List<String>>>(){}.getType());
    }
    return info;
  }

  // Insert GalleryInfo, or update GalleryInfo and increment reference
  private static void putGalleryInfo(@NonNull StorIOSQLite.LowLevel lowLevel,
      @NonNull Gson gson, @NonNull GalleryInfo info) {
    final Cursor cursor = lowLevel.query(Query.builder()
        .table(TABLE_NAME)
        .where(COLUMN_GID + " = ?")
        .whereArgs(info.gid)
        .build());

    try {
      if (cursor.moveToNext()) {
        // Found old GalleryInfo, merge, increment reference, put back
        final GalleryInfo oldInfo = mapGalleryInfoFromCursor(cursor, gson);
        final int reference = DBUtils.getInt(cursor, COLUMN_REFERENCE, 1);
        oldInfo.merge(info);
        final UpdateQuery updateQuery = UpdateQuery.builder()
            .table(TABLE_NAME)
            .where(COLUMN_GID + " = ?")
            .whereArgs(info.gid)
            .build();
        lowLevel.update(updateQuery, mapGalleryInfoToContentValues(gson, info, reference + 1));
      } else {
        // Can't find old GalleryInfo, insert new one
        final InsertQuery insertQuery = InsertQuery.builder()
            .table(TABLE_NAME)
            .build();
        lowLevel.insert(insertQuery, mapGalleryInfoToContentValues(gson, info, 1));
      }
    } finally {
      cursor.close();
    }
  }

  // Update GalleryInfo keeping reference
  private static void updateGalleryInfo(@NonNull StorIOSQLite.LowLevel lowLevel,
      @NonNull Gson gson, @NonNull GalleryInfo info) {
    final Cursor cursor = lowLevel.query(Query.builder()
        .table(TABLE_NAME)
        .where(COLUMN_GID + " = ?")
        .whereArgs(info.gid)
        .build());

    try {
      if (cursor.moveToNext()) {
        final GalleryInfo oldInfo = mapGalleryInfoFromCursor(cursor, gson);
        final int reference = DBUtils.getInt(cursor, COLUMN_REFERENCE, 1);
        oldInfo.merge(info);
        final UpdateQuery updateQuery = UpdateQuery.builder()
            .table(TABLE_NAME)
            .where(COLUMN_GID + " = ?")
            .whereArgs(info.gid)
            .build();
        lowLevel.update(updateQuery, mapGalleryInfoToContentValues(gson, info, reference));
      } else {
        Log.e(LOG_TAG, "Can't get GalleryInfo in db: " + info.gid);
      }
    } finally {
      cursor.close();
    }
  }

  private static GalleryInfo getGalleryInfo(@NonNull StorIOSQLite.LowLevel lowLevel,
      @NonNull Gson gson, long gid) {
    final Cursor cursor = lowLevel.query(Query.builder()
        .table(TABLE_NAME)
        .where(COLUMN_GID + " = ?")
        .whereArgs(gid)
        .build());

    try {
      if (cursor.moveToNext()) {
        return mapGalleryInfoFromCursor(cursor, gson);
      } else {
        Log.e(LOG_TAG, "Can't get GalleryInfo in db: " + gid);
      }
    } finally {
      cursor.close();
    }

    return null;
  }

  // Decrement GalleryInfo reference, or delete it from database
  private static void deleteGalleryInfo(@NonNull StorIOSQLite.LowLevel lowLevel,
      @NonNull Gson gson, @NonNull GalleryInfo info) {
    final Cursor cursor = lowLevel.query(Query.builder()
        .table(TABLE_NAME)
        .where(COLUMN_GID + " = ?")
        .whereArgs(info.gid)
        .build());

    try {
      if (cursor.moveToNext()) {
        int reference = DBUtils.getInt(cursor, COLUMN_REFERENCE, 1);
        if (reference <= 1) {
          // Delete the GalleryInfo
          final DeleteQuery deleteQuery = DeleteQuery.builder()
              .table(TABLE_NAME)
              .where(COLUMN_GID + " = ?")
              .whereArgs(info.gid)
              .build();
          lowLevel.delete(deleteQuery);
        } else {
          // Merge, decrement reference, put back
          final GalleryInfo oldInfo = mapGalleryInfoFromCursor(cursor, gson);
          oldInfo.merge(info);
          final UpdateQuery updateQuery = UpdateQuery.builder()
              .table(TABLE_NAME)
              .where(COLUMN_GID + " = ?")
              .whereArgs(info.gid)
              .build();
          lowLevel.update(updateQuery, mapGalleryInfoToContentValues(gson, info, reference - 1));
        }
      } else {
        Log.e(LOG_TAG, "Can't get GalleryInfo in db: " + info.gid);
      }
    } finally {
      cursor.close();
    }
  }

  /**
   * Puts {@link GalleryInfoContainer} to database.
   * The table must contains a column named {@link #COLUMN_GID}
   * to store {@link GalleryInfo#gid}.
   */
  public static abstract class PutResolver<T extends GalleryInfoContainer> extends
      com.pushtorefresh.storio.sqlite.operations.put.PutResolver<T> {

    private Gson gson;

    protected PutResolver(@NonNull Gson gson) {
      this.gson = gson;
    }

    /**
     * Converts object of required type to {@link InsertQuery}.
     *
     * @param object non-null object that should be converted to {@link InsertQuery}.
     * @return non-null {@link InsertQuery}.
     */
    @NonNull
    protected abstract InsertQuery mapToInsertQuery(@NonNull T object);

    /**
     * Converts object of required type to {@link UpdateQuery}.
     *
     * @param object non-null object that should be converted to {@link UpdateQuery}.
     * @return non-null {@link UpdateQuery}.
     */
    @NonNull
    protected abstract UpdateQuery mapToUpdateQuery(@NonNull T object);

    /**
     * Converts object of required type to {@link ContentValues}.
     * <p>
     * {@link GalleryInfo#gid} will be put to the {@link ContentValues}.
     *
     * @param object non-null object that should be converted to {@link ContentValues}.
     * @return non-null {@link ContentValues}.
     */
    @NonNull
    protected abstract ContentValues mapToContentValues(@NonNull T object);

    @NonNull
    @Override
    public PutResult performPut(@NonNull StorIOSQLite sql, @NonNull T item) {
      final UpdateQuery updateQuery = mapToUpdateQuery(item);
      final StorIOSQLite.LowLevel lowLevel = sql.lowLevel();

      // for data consistency in concurrent environment, encapsulate Put Operation into transaction
      lowLevel.beginTransaction();

      try {
        final Cursor cursor = lowLevel.query(Query.builder()
            .table(updateQuery.table())
            .where(InternalQueries.nullableString(updateQuery.where()))
            .whereArgs((Object[]) InternalQueries.nullableArrayOfStringsFromListOfStrings(updateQuery.whereArgs()))
            .build());

        final PutResult putResult;

        try {
          final GalleryInfo info = item.getGalleryInfo();
          final ContentValues contentValues = mapToContentValues(item);
          contentValues.put(COLUMN_GID, info != null ? info.gid : 0);

          if (cursor.getCount() == 0) {
            if (info != null) {
              putGalleryInfo(lowLevel, gson, info);
            }
            final InsertQuery insertQuery = mapToInsertQuery(item);
            final long insertedId = lowLevel.insert(insertQuery, contentValues);
            putResult = PutResult.newInsertResult(insertedId, insertQuery.table());
          } else {
            if (info != null) {
              updateGalleryInfo(lowLevel, gson, info);
            }
            final int numberOfRowsUpdated = lowLevel.update(updateQuery, contentValues);
            putResult = PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table());
          }
        } finally {
          cursor.close();
        }

        // everything okay
        lowLevel.setTransactionSuccessful();

        return putResult;
      } finally {
        // in case of bad situations, db won't be affected
        lowLevel.endTransaction();
      }
    }
  }

  /**
   * The item table use {@link GalleryInfo} as primary key.
   */
  public static abstract class UniquePutResolver<T extends GalleryInfoContainer>
      extends PutResolver<T> {

    private String tableName;

    protected UniquePutResolver(@NonNull Gson gson, @NonNull String tableName) {
      super(gson);
      this.tableName = tableName;
    }

    @NonNull
    @Override
    protected InsertQuery mapToInsertQuery(@NonNull T object) {
      return InsertQuery.builder()
          .table(tableName)
          .build();
    }

    @NonNull
    @Override
    protected UpdateQuery mapToUpdateQuery(@NonNull T object) {
      GalleryInfo info = object.getGalleryInfo();
      return UpdateQuery.builder()
          .table(tableName)
          .where(COLUMN_GID + " = ?")
          .whereArgs(info != null ? info.gid : 0)
          .build();
    }
  }

  /**
   * Gets {@link GalleryInfoContainer} from database.
   * The table must contains a column named {@link #COLUMN_GID}
   * to store {@link GalleryInfo#gid}.
   */
  public static abstract class GetResolver<T extends GalleryInfoContainer> extends
      com.pushtorefresh.storio.sqlite.operations.get.GetResolver<T> {

    private StorIOSQLite.LowLevel lowLevel;
    private Gson gson;

    protected GetResolver(@NonNull Gson gson) {
      this.gson = gson;
    }

    /**
     * Converts {@link Cursor} with already set position to object of required type.
     * <p>
     * {@link GalleryInfoContainer#setGalleryInfo(GalleryInfo)} will be called after it.
     *
     * @param cursor not closed {@link Cursor} with already set position
     *               that should be parsed and converted to object of required type.
     * @return non-null object of required type with data parsed from passed {@link Cursor}.
     */
    @NonNull
    protected abstract T mapFromCursor2(@NonNull Cursor cursor);

    @NonNull
    @Override
    public T mapFromCursor(@NonNull Cursor cursor) {
      T item = mapFromCursor2(cursor);
      long gid = DBUtils.getLong(cursor, COLUMN_GID, 0);
      if (gid != 0) {
        GalleryInfo info = getGalleryInfo(lowLevel, gson, gid);
        if (info != null) {
          item.setGalleryInfo(info);
        }
      }
      return item;
    }

    @NonNull
    @Override
    public Cursor performGet(@NonNull StorIOSQLite storIOSQLite, @NonNull RawQuery rawQuery) {
      lowLevel = storIOSQLite.lowLevel();
      return storIOSQLite.lowLevel().rawQuery(rawQuery);
    }

    @NonNull
    @Override
    public Cursor performGet(@NonNull StorIOSQLite storIOSQLite, @NonNull Query query) {
      lowLevel = storIOSQLite.lowLevel();
      return storIOSQLite.lowLevel().query(query);
    }
  }

  /**
   * Deletes {@link GalleryInfoContainer} from database.
   * The table must contains a column named {@link #COLUMN_GID}
   * to store {@link GalleryInfo#gid}.
   */
  public static abstract class DeleteResolver<T extends GalleryInfoContainer> extends
      com.pushtorefresh.storio.sqlite.operations.delete.DeleteResolver<T> {

    private Gson gson;

    protected DeleteResolver(@NonNull Gson gson) {
      this.gson = gson;
    }

    /**
     * Converts object to {@link DeleteQuery}.
     *
     * @param object object that should be deleted.
     * @return {@link DeleteQuery} that will be performed.
     */
    @NonNull
    protected abstract DeleteQuery mapToDeleteQuery(@NonNull T object);

    @NonNull
    @Override
    public DeleteResult performDelete(@NonNull StorIOSQLite sql, @NonNull T item) {
      final StorIOSQLite.LowLevel lowLevel = sql.lowLevel();

      // for data consistency in concurrent environment, encapsulate Put Operation into transaction
      lowLevel.beginTransaction();

      try {
        // Delete GalleryInfo
        GalleryInfo info = item.getGalleryInfo();
        if (info != null) {
          deleteGalleryInfo(lowLevel, gson, info);
        }

        final DeleteQuery deleteQuery = mapToDeleteQuery(item);
        final int numberOfRowsDeleted = lowLevel.delete(deleteQuery);
        final DeleteResult deleteResult =
            DeleteResult.newInstance(numberOfRowsDeleted, deleteQuery.table());

        // everything okay
        lowLevel.setTransactionSuccessful();

        return deleteResult;
      } finally {
        // in case of bad situations, db won't be affected
        lowLevel.endTransaction();
      }
    }
  }

  /**
   * The item table use {@link GalleryInfo} as primary key.
   */
  public static abstract class UniqueDeleteResolver<T extends GalleryInfoContainer>
      extends DeleteResolver<T> {

    private String tableName;

    protected UniqueDeleteResolver(@NonNull Gson gson, @NonNull String tableName) {
      super(gson);
      this.tableName = tableName;
    }

    @NonNull
    @Override
    protected DeleteQuery mapToDeleteQuery(@NonNull T object) {
      GalleryInfo info = object.getGalleryInfo();
      return DeleteQuery.builder()
          .table(tableName)
          .where(COLUMN_GID + " = ?")
          .whereArgs(info != null ? info.gid: 0)
          .build();
    }
  }
}
