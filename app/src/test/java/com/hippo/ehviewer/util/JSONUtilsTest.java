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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.os.Build;
import com.hippo.ehviewer.BuildConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class JSONUtilsTest {

  private static final String JSON_OBJECT_STR = "{\"a\":null,\"b\":\"null\"}";
  private static final String JSON_ARRAY_STR = "[null,\"null\"]";

  private JSONObject jo;
  private JSONArray ja;

  @Before
  public void before() throws JSONException {
    jo = new JSONObject(JSON_OBJECT_STR);
    ja = new JSONArray(JSON_ARRAY_STR);
  }

  @Test
  public void testGetStringJSONObject() throws JSONException {
    assertEquals(null, JSONUtils.getString(jo, "a"));
    assertEquals("null", JSONUtils.getString(jo, "b"));
    try {
      assertEquals(null, JSONUtils.getString(jo, "c"));
      fail();
    } catch (JSONException e) {
    }

    assertEquals("null", jo.getString("a"));
  }

  @Test
  public void testGetStringJSONArray() throws JSONException {
    assertEquals(null, JSONUtils.getString(ja, 0));
    assertEquals("null", JSONUtils.getString(ja, 1));
    try {
      assertEquals(null, JSONUtils.getString(ja, 3));
      fail();
    } catch (JSONException e) {
    }

    assertEquals("null", ja.getString(0));
  }

  @Test
  public void testOptStringJSONObject() {
    assertEquals(null, JSONUtils.optString(jo, "a"));
    assertEquals("null", JSONUtils.optString(jo, "b"));
    assertEquals(null, JSONUtils.optString(jo, "c"));
    assertEquals("", JSONUtils.optString(jo, "c", ""));

    assertEquals("null", jo.optString("a"));
  }

  @Test
  public void testOptStringJSONArray() {
    assertEquals(null, JSONUtils.optString(ja, 0));
    assertEquals("null", JSONUtils.optString(ja, 1));
    assertEquals(null, JSONUtils.optString(ja, 2));
    assertEquals("", JSONUtils.optString(ja, 2, ""));

    assertEquals("null", ja.optString(0));
  }
}
