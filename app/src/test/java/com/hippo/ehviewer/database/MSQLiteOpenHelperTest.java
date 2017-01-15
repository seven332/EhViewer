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

package com.hippo.ehviewer.database;

/*
 * Created by Hippo on 1/15/2017.
 */

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import com.hippo.ehviewer.BuildConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class MSQLiteOpenHelperTest {

  private static class DbOpenHelper extends MSQLiteOpenHelper {

    private static String DB_NAME = "test.db";

    private static String VERSION_1_TABLE_A = "A";
    private static String VERSION_2_TABLE_B = "B";

    public DbOpenHelper(Context context, int version) {
      super(context, DB_NAME, version);
    }

    @Override
    public void onInit(MSQLite ms) {
      ms.version(1)
          .createTable(VERSION_1_TABLE_A);
      ms.version(2)
          .createTable(VERSION_2_TABLE_B);
      ms.version(3)
          .dropTable(VERSION_1_TABLE_A);
    }
  }

  @Test
  public void testUpdate() {
    Context app = RuntimeEnvironment.application;
    DbOpenHelper helper;
    SQLiteDatabase db;

    helper = new DbOpenHelper(app, 1);
    db = helper.getReadableDatabase();
    assertTrue(existsTable(db, DbOpenHelper.VERSION_1_TABLE_A));
    assertFalse(existsTable(db, DbOpenHelper.VERSION_2_TABLE_B));
    db.close();
    helper.close();

    helper = new DbOpenHelper(app, 2);
    db = helper.getReadableDatabase();
    assertTrue(existsTable(db, DbOpenHelper.VERSION_1_TABLE_A));
    assertTrue(existsTable(db, DbOpenHelper.VERSION_2_TABLE_B));
    db.close();
    helper.close();

    helper = new DbOpenHelper(app, 3);
    db = helper.getReadableDatabase();
    assertFalse(existsTable(db, DbOpenHelper.VERSION_1_TABLE_A));
    assertTrue(existsTable(db, DbOpenHelper.VERSION_2_TABLE_B));

    db.close();
    helper.close();
  }

  @Test
  public void testDropAllTables() {
    SQLiteOpenHelper helper = new SQLiteOpenHelper(RuntimeEnvironment.application, "drop.db", null, 1) {
      @Override
      public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table1 (_id INTEGER PRIMARY KEY NOT NULL);");
        db.execSQL("CREATE TABLE table2 (_id INTEGER PRIMARY KEY NOT NULL);");
        db.execSQL("CREATE TABLE table3 (_id INTEGER PRIMARY KEY NOT NULL);");
        db.execSQL("CREATE TABLE table4 (_id INTEGER PRIMARY KEY NOT NULL);");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    };
    SQLiteDatabase db = helper.getWritableDatabase();

    assertTrue(existsTable(db, "table1"));
    assertTrue(existsTable(db, "table2"));
    assertTrue(existsTable(db, "table3"));
    assertTrue(existsTable(db, "table4"));
    assertTrue(existsTable(db, "android_metadata"));

    MSQLiteOpenHelper.dropAllTables(db);

    assertFalse(existsTable(db, "table1"));
    assertFalse(existsTable(db, "table2"));
    assertFalse(existsTable(db, "table3"));
    assertFalse(existsTable(db, "table4"));
    assertTrue(existsTable(db, "android_metadata"));

    db.close();
    helper.close();
  }

  private boolean existsTable(SQLiteDatabase db, String table) {
    boolean result = false;
    Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "';", null);
    while (c.moveToNext()) {
      result = true;
    }
    c.close();
    return result;
  }
}
