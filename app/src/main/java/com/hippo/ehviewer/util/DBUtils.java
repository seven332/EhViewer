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

package com.hippo.ehviewer.util;

/*
 * Created by Hippo on 3/3/2017.
 */

import android.database.Cursor;
import android.util.Log;

public final class DBUtils {
  private DBUtils() {}

  private static final String LOG_TAG = DBUtils.class.getSimpleName();

  /**
   * Gets boolean from the {@code cursor}.
   */
  public static boolean getBoolean(Cursor cursor, String name, boolean defValue) {
    if (cursor == null || name == null) {
      return defValue;
    }

    try {
      int index = cursor.getColumnIndex(name);
      if (index == -1) {
        return defValue;
      }
      return cursor.getInt(index) != 0;
    } catch (Throwable e) {
      Log.d(LOG_TAG, "Can't get boolean", e);
      return defValue;
    }
  }

  /**
   * Gets int from the {@code cursor}.
   */
  public static int getInt(Cursor cursor, String name, int defValue) {
    if (cursor == null || name == null) {
      return defValue;
    }

    try {
      int index = cursor.getColumnIndex(name);
      if (index == -1) {
        return defValue;
      }
      return cursor.getInt(index);
    } catch (Throwable e) {
      Log.d(LOG_TAG, "Can't get int", e);
      return defValue;
    }
  }

  /**
   * Gets long from the {@code cursor}.
   */
  public static long getLong(Cursor cursor, String name, long defValue) {
    if (cursor == null || name == null) {
      return defValue;
    }

    try {
      int index = cursor.getColumnIndex(name);
      if (index == -1) {
        return defValue;
      }
      return cursor.getLong(index);
    } catch (Throwable e) {
      Log.d(LOG_TAG, "Can't get long", e);
      return defValue;
    }
  }

  /**
   * Gets int from the {@code cursor}.
   */
  public static float getFloat(Cursor cursor, String name, float defValue) {
    if (cursor == null || name == null) {
      return defValue;
    }

    try {
      int index = cursor.getColumnIndex(name);
      if (index == -1) {
        return defValue;
      }
      return cursor.getFloat(index);
    } catch (Throwable e) {
      Log.d(LOG_TAG, "Can't get float", e);
      return defValue;
    }
  }

  /**
   * Gets string from the {@code cursor}.
   */
  public static String getString(Cursor cursor, String name, String defValue) {
    if (cursor == null || name == null) {
      return defValue;
    }

    try {
      int index = cursor.getColumnIndex(name);
      if (index == -1) {
        return defValue;
      }
      return cursor.getString(index);
    } catch (Throwable e) {
      Log.d(LOG_TAG, "Can't get String", e);
      return defValue;
    }
  }
}
