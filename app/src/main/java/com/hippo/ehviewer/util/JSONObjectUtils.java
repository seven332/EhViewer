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
 * Created by Hippo on 2/24/2017.
 */

import org.json.JSONException;
import org.json.JSONObject;

public final class JSONObjectUtils {
  private JSONObjectUtils() {}

  /**
   * Returns the value mapped by {@code name} if it exists, coercing it if
   * necessary, or throws if no such mapping exists.
   * <p>
   * {@code null} doesn't treated as {@code "null"}.
   */
  public static String getString(JSONObject jo, String name) throws JSONException {
    String value = jo.getString(name);
    if (jo.isNull(name)) {
      return null;
    } else {
      return value;
    }
  }

  /**
   * Returns the value mapped by {@code name} if it exists, coercing it if
   * necessary, or {@code null} if no such mapping exists.
   * <p>
   * {@code null} doesn't treated as {@code "null"}.
   */
  public static String optString(JSONObject jo, String name) {
    return optString(jo, name, null);
  }

  /**
   * Returns the value mapped by {@code name} if it exists, coercing it if
   * necessary, or {@code fallback} if no such mapping exists.
   * <p>
   * {@code null} doesn't treated as {@code "null"}.
   */
  public static String optString(JSONObject jo, String name, String fallback) {
    if (jo.has(name)) {
      String value = jo.optString(name, fallback);
      if (jo.isNull(name)) {
        return null;
      } else {
        return value;
      }
    } else {
      return fallback;
    }
  }
}
