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

package com.hippo.ehviewer.network;

/*
 * Created by Hippo on 1/15/2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.Nullable;
import android.util.Log;
import com.hippo.ehviewer.database.MSQLite;
import com.hippo.ehviewer.database.MSQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.Cookie;

class CookieDatabase {

  private static final String LOG_TAG = CookieDatabase.class.getSimpleName();

  private static Map<String, Integer> columnIndexMap = new ConcurrentHashMap<>();

  private OpenHelper helper;
  private SQLiteDatabase db;
  private Map<Cookie, Long> cookieIdMap = new HashMap<>();

  private AtomicBoolean getAllCookiesCalled = new AtomicBoolean();

  public CookieDatabase(Context context, String name) {
    helper = new OpenHelper(context, name);
    db = helper.getWritableDatabase();
  }

  // It is safe to cache column index to a thread-safe map,
  // because the database schema can't change during app runtime.
  private static int getColumnIndex(Cursor cursor, String column) {
    Integer index = columnIndexMap.get(column);
    if (index != null) return index;

    index = cursor.getColumnIndexOrThrow(column);
    columnIndexMap.put(column, index);
    return index;
  }

  private static String getString(Cursor cursor, String column) {
    return cursor.getString(getColumnIndex(cursor, column));
  }

  private static long getLong(Cursor cursor, String column) {
    return cursor.getLong(getColumnIndex(cursor, column));
  }

  private static boolean getBoolean(Cursor cursor, String column) {
    return cursor.getInt(getColumnIndex(cursor, column)) != 0;
  }

  // Return valid and not expired cookie or null
  @Nullable
  private static Cookie parse(Cursor cursor, long currentTimeMillis) {
    try {
      String name = getString(cursor, OpenHelper.COLUMN_NAME);
      String value = getString(cursor, OpenHelper.COLUMN_VALUE);
      long expiresAt = getLong(cursor, OpenHelper.COLUMN_EXPIRES_AT);
      String domain = getString(cursor, OpenHelper.COLUMN_DOMAIN);
      String path = getString(cursor, OpenHelper.COLUMN_PATH);
      boolean secure = getBoolean(cursor, OpenHelper.COLUMN_SECURE);
      boolean httpOnly = getBoolean(cursor, OpenHelper.COLUMN_HTTP_ONLY);
      boolean persistent = getBoolean(cursor, OpenHelper.COLUMN_PERSISTENT);
      boolean hostOnly = getBoolean(cursor, OpenHelper.COLUMN_HOST_ONLY);

      // Check non-persistent or expired
      if (!persistent || expiresAt <= currentTimeMillis) {
        return null;
      }

      Cookie.Builder builder = new Cookie.Builder();
      builder.name(name);
      builder.value(value);
      if (hostOnly) {
        builder.hostOnlyDomain(domain);
      } else {
        builder.domain(domain);
      }
      builder.path(path);
      builder.expiresAt(expiresAt);
      if (secure) builder.secure();
      if (httpOnly) builder.httpOnly();

      return builder.build();
    } catch (Throwable e) {
      Log.e(LOG_TAG, "Can't parse a cookie in database", e);
      return null;
    }
  }

  // Return a domain-cookies map
  public Map<String, CookieSet> getAllCookies() {
    if (!getAllCookiesCalled.compareAndSet(false, true)) {
      throw new IllegalStateException("Only call getAllCookies() once.");
    }

    long now = System.currentTimeMillis();
    Map<String, CookieSet> map = new HashMap<>();
    List<Long> toRemove = new ArrayList<>();

    Cursor cursor = db.rawQuery("SELECT * FROM " + OpenHelper.TABLE_COOKIE + ";", null);
    while (cursor.moveToNext()) {
      long id = getLong(cursor, MSQLite.COLUMN_ID);
      Cookie cookie = parse(cursor, now);

      if (cookie == null) {
        toRemove.add(id);
        continue;
      }

      // Save id of the cookie in db
      cookieIdMap.put(cookie, id);

      CookieSet set = map.get(cookie.domain());
      if (set == null) {
        set = new CookieSet();
        map.put(cookie.domain(), set);
      }
      set.add(cookie);
    }
    cursor.close();

    // Remove invalid or expired cookie
    if (!toRemove.isEmpty()) {
      SQLiteStatement statement = db.compileStatement(
          "DELETE FROM " + OpenHelper.TABLE_COOKIE + " WHERE " + MSQLite.COLUMN_ID + " = ?;");
      db.beginTransaction();
      try {
        for (long id: toRemove) {
          statement.bindLong(1, id);
          statement.executeUpdateDelete();
        }
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
      statement.close();
    }

    return map;
  }

  private static ContentValues getCookieContentValues(Cookie cookie) {
    ContentValues values = new ContentValues();
    values.put(OpenHelper.COLUMN_NAME, cookie.name());
    values.put(OpenHelper.COLUMN_VALUE, cookie.value());
    values.put(OpenHelper.COLUMN_EXPIRES_AT, cookie.expiresAt());
    values.put(OpenHelper.COLUMN_DOMAIN, cookie.domain());
    values.put(OpenHelper.COLUMN_PATH, cookie.path());
    values.put(OpenHelper.COLUMN_SECURE, cookie.secure());
    values.put(OpenHelper.COLUMN_HTTP_ONLY, cookie.httpOnly());
    values.put(OpenHelper.COLUMN_PERSISTENT, cookie.persistent());
    values.put(OpenHelper.COLUMN_HOST_ONLY, cookie.hostOnly());
    return values;
  }

  public void add(Cookie cookie) {
    long id = db.insert(OpenHelper.TABLE_COOKIE, null, getCookieContentValues(cookie));
    if (id != -1) {
      Long oldId = cookieIdMap.put(cookie, id);
      if (oldId != null) Log.e(LOG_TAG, "Add a duplicate cookie");
    } else {
      Log.e(LOG_TAG, "An error occurred when insert a cookie");
    }
  }

  public void update(Cookie from, Cookie to) {
    Long id = cookieIdMap.get(from);
    if (id == null) {
      Log.e(LOG_TAG, "Can't get id when update the cookie");
      return;
    }

    ContentValues values = getCookieContentValues(to);
    String whereClause = MSQLite.COLUMN_ID + " = ?";
    String[] whereArgs = new String[] { Long.toString(id) };
    int count = db.update(OpenHelper.TABLE_COOKIE, values, whereClause, whereArgs);
    if (count != 1) Log.e(LOG_TAG, "Bad result when update cookie: " + count);

    // Update it in cookie-id map
    cookieIdMap.remove(from);
    cookieIdMap.put(to, id);
  }

  public void remove(Cookie cookie) {
    Long id = cookieIdMap.get(cookie);
    if (id == null) {
      Log.e(LOG_TAG, "Can't get id when remove the cookie");
      return;
    }

    String whereClause = MSQLite.COLUMN_ID + " = ?";
    String[] whereArgs = new String[] { Long.toString(id) };
    int count = db.delete(OpenHelper.TABLE_COOKIE, whereClause, whereArgs);
    if (count != 1) Log.e(LOG_TAG, "Bad result when remove cookie: " + count);

    // Remove it from cookie-id map
    cookieIdMap.remove(cookie);
  }

  public void clear() {
    db.delete(OpenHelper.TABLE_COOKIE, null, null);
    cookieIdMap.clear();
  }

  public void close() {
    db.close();
    helper.close();
  }

  private static class OpenHelper extends MSQLiteOpenHelper {

    private static int VERSION_1 = 1;
    private static String TABLE_COOKIE = "OK_HTTP_3_COOKIE";
    private static String COLUMN_NAME = "NAME";
    private static String COLUMN_VALUE = "VALUE";
    private static String COLUMN_EXPIRES_AT = "EXPIRES_AT";
    private static String COLUMN_DOMAIN = "DOMAIN";
    private static String COLUMN_PATH = "PATH";
    private static String COLUMN_SECURE = "SECURE";
    private static String COLUMN_HTTP_ONLY = "HTTP_ONLY";
    private static String COLUMN_PERSISTENT = "PERSISTENT";
    private static String COLUMN_HOST_ONLY = "HOST_ONLY";

    private static int VERSION_CURRENT = VERSION_1;

    public OpenHelper(Context context, String name) {
      super(context, name, VERSION_CURRENT);
    }

    @Override
    public void onInit(MSQLite ms) {
      ms.version(1)
          .createTable(TABLE_COOKIE)
          .insertColumn(TABLE_COOKIE, COLUMN_NAME, String.class)
          .insertColumn(TABLE_COOKIE, COLUMN_VALUE, String.class)
          .insertColumn(TABLE_COOKIE, COLUMN_EXPIRES_AT, long.class)
          .insertColumn(TABLE_COOKIE, COLUMN_DOMAIN, String.class)
          .insertColumn(TABLE_COOKIE, COLUMN_PATH, String.class)
          .insertColumn(TABLE_COOKIE, COLUMN_SECURE, boolean.class)
          .insertColumn(TABLE_COOKIE, COLUMN_HTTP_ONLY, boolean.class)
          .insertColumn(TABLE_COOKIE, COLUMN_PERSISTENT, boolean.class)
          .insertColumn(TABLE_COOKIE, COLUMN_HOST_ONLY, boolean.class);
    }
  }
}
