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

package com.hippo.ehviewer.content;

/*
 * Created by Hippo on 2/6/2017.
 */

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import com.hippo.yorozuya.NumberUtils;

/**
 * {@code Preferences} provides safe and convenience
 * methods to put or get data based on {@link SharedPreferences}.
 * <p>
 * If types doesn't meet in getXXX(), default value is returned.
 */
public class Preferences {

  private static final String LOG_TAG = Preferences.class.getSimpleName();

  private SharedPreferences shardPref;

  public Preferences(@NonNull SharedPreferences shardPref) {
    this.shardPref = shardPref;
  }

  protected boolean getBoolean(String key, boolean defValue) {
    try {
      return shardPref.getBoolean(key, defValue);
    } catch (ClassCastException e) {
      Log.e(LOG_TAG, "The value of " + key + " is not a boolean.", e);
      return defValue;
    }
  }

  protected void putBoolean(String key, boolean value) {
    shardPref.edit().putBoolean(key, value).apply();
  }

  protected int getInt(String key, int defValue) {
    try {
      return shardPref.getInt(key, defValue);
    } catch (ClassCastException e) {
      Log.e(LOG_TAG, "The value of " + key + " is not a int.", e);
      return defValue;
    }
  }

  protected void putInt(String key, int value) {
    shardPref.edit().putInt(key, value).apply();
  }

  protected long getLong(String key, long defValue) {
    try {
      return shardPref.getLong(key, defValue);
    } catch (ClassCastException e) {
      Log.e(LOG_TAG, "The value of " + key + " is not a long.", e);
      return defValue;
    }
  }

  protected void putLong(String key, long value) {
    shardPref.edit().putLong(key, value).apply();
  }

  protected float getFloat(String key, float defValue) {
    try {
      return shardPref.getFloat(key, defValue);
    } catch (ClassCastException e) {
      Log.e(LOG_TAG, "The value of " + key + " is not a float.", e);
      return defValue;
    }
  }

  protected void putFloat(String key, float value) {
    shardPref.edit().putFloat(key, value).apply();
  }

  protected String getString(String key, String defValue) {
    try {
      return shardPref.getString(key, defValue);
    } catch (ClassCastException e) {
      Log.e(LOG_TAG, "The value of " + key + " is not a String.", e);
      return defValue;
    }
  }

  protected void putString(String key, String value) {
    shardPref.edit().putString(key, value).apply();
  }

  /**
   * Reads int from string as a decimal number.
   */
  protected int getDecimalInt(String key, int defValue) {
    try {
      return NumberUtils.parseInt(shardPref.getString(key, Integer.toString(defValue)), defValue);
    } catch (ClassCastException e) {
      Log.e(LOG_TAG, "The value of " + key + " is not a String.", e);
      return defValue;
    }
  }

  /**
   * Writes int to string as a decimal number.
   */
  protected void putDecimalInt(String key, int value) {
    shardPref.edit().putString(key, Integer.toString(value)).apply();
  }

  /**
   * Creates a {@link android.content.SharedPreferences.Editor}
   * from the {@link SharedPreferences}.
   */
  protected SharedPreferences.Editor edit() {
    return shardPref.edit();
  }
}
