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

package com.hippo.ehviewer.util

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/*
 * Created by Hippo on 2017/8/13.
 */

private inline fun <T> Cursor.getValue(
    column: String,
    defValue: T,
    getter: Cursor.(Int) -> T
): T {
  try {
    val index = getColumnIndex(column)
    if (index != -1) {
      return getter(index)
    }
  } catch (e: Throwable) {}
  return defValue
}

/** Gets boolean value for special column from the `cursor`. */
fun Cursor.getBoolean(column: String, defValue: Boolean): Boolean =
    getValue(column, if (defValue) 1 else 0, Cursor::getInt) != 0

/** Gets int value for special column from the `cursor`. */
fun Cursor.getInt(column: String, defValue: Int): Int =
    getValue(column, defValue, Cursor::getInt)

/** Gets long value for special column from the `cursor`. */
fun Cursor.getLong(column: String, defValue: Long): Long =
    getValue(column, defValue, Cursor::getLong)

/** Gets float value for special column from the `cursor`. */
fun Cursor.getFloat(column: String, defValue: Float): Float =
    getValue(column, defValue, Cursor::getFloat)

/** Gets string value for special column from the `cursor`. */
fun Cursor.getString(column: String, defValue: String?): String? =
    getValue(column, defValue, Cursor::getString)

/** Runs the provided SQL and executes the given [block] on each row. */
inline fun SQLiteDatabase.rawQuery(sql: String, selectionArgs: Array<String>?, block: (Cursor) -> Unit) {
  val cursor = rawQuery(sql, selectionArgs)
  cursor.use {
    while (it.moveToNext()) {
      block(cursor)
    }
  }
}

/** Executes the given [block] function in transaction. */
inline fun <R> SQLiteDatabase.transaction(block: (SQLiteDatabase) -> R): R {
  beginTransaction()
  try {
    return block(this).also { setTransactionSuccessful() }
  } finally {
    endTransaction()
  }
}
