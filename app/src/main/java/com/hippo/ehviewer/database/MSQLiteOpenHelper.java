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

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code MSQLiteOpenHelper} is good at creating and updating database.
 */
// I can't find any library to make SQLiteOpenHelper easier.
// So I create this MSQLiteOpenHelper.
public abstract class MSQLiteOpenHelper extends SQLiteOpenHelper {

  private int version;
  private MSQLite ms;

  public MSQLiteOpenHelper(Context context, String name, int version) {
    super(context, name, null, version);
    init(version);
  }

  public MSQLiteOpenHelper(Context context, String name,
      SQLiteDatabase.CursorFactory factory, int version) {
    super(context, name, factory, version);
    init(version);
  }

  public MSQLiteOpenHelper(Context context, String name,
      SQLiteDatabase.CursorFactory factory, int version,
      DatabaseErrorHandler errorHandler) {
    super(context, name, factory, version, errorHandler);
    init(version);
  }

  private void init(int version) {
    this.version = version;
    this.ms = new MSQLite();
    onInit(ms);
  }

  /**
   * Add commands to {@code MSQLite} here.
   * <p>
   * For example:<br>
   * Version 1: table1<br>
   * Version 2: table1 table2<br>
   * Version 3: table2
   * <pre>{@code
   *   ms.version(1)
   *       .createTable("table1");
   *   ms.version(2)
   *       .createTable("table2");
   *   ms.version(3)
   *       .dropTable("table1");
   * }</pre>
   */
  public abstract void onInit(MSQLite ms);

  @Override
  public void onCreate(SQLiteDatabase db) {
    onUpgrade(db, 0, version);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    List<MSQLite.Command> commands = ms.getCommands(oldVersion, newVersion);
    for (MSQLite.Command command: commands) {
      command.run(db);
    }
  }

  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    dropAllTables(db);
    onCreate(db);
  }

  @VisibleForTesting
  static void dropAllTables(SQLiteDatabase db) {
    List<String> tables = new ArrayList<>();

    Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table';", null);
    while (c.moveToNext()) {
      String table = c.getString(0);
      if (!"android_metadata".equals(table)
          && !"sqlite_sequence".equals(table)
          && !TextUtils.isEmpty(table)) {
        tables.add(table);
      }
    }
    c.close();

    for (String table: tables) {
      db.execSQL("DROP TABLE IF EXISTS " + table + ";");
    }
  }
}
