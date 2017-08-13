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

package com.hippo.ehviewer.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/*
 * Created by Hippo on 6/17/2017.
 */

internal class MSQLiteOpenHelper(
    context: Context,
    name: String,
    val version: Int,
    val sqlBuilder: MSQLiteBuilder
) : SQLiteOpenHelper(context, name, null, version) {

  override fun onCreate(db: SQLiteDatabase) { onUpgrade(db, 0, version) }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
      sqlBuilder.getCommands(oldVersion, newVersion).forEach { it.run(db) }

  override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    dropAllTables(db)
    onCreate(db)
  }

  private fun dropAllTables(db: SQLiteDatabase) {
    val tables = mutableListOf<String>()
    db.rawQuery("SELECT name FROM sqlite_master WHERE type='table';", null).use {
      while (it.moveToNext()) {
        val table = it.getString(0)
        if ("android_metadata" != table && "sqlite_sequence" != table && table.isNotEmpty()) {
          tables.add(table)
        }
      }
    }
    tables.forEach { db.execSQL("DROP TABLE IF EXISTS $it;") }
  }
}
