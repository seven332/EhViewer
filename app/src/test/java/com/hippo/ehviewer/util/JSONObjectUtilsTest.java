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
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class JSONObjectUtilsTest {

  private static final String JSON_STR = "{\"a\":null,\"b\":\"null\"}";

  private JSONObject jo;

  @Before
  public void before() throws JSONException {
    jo = new JSONObject(JSON_STR);
  }

  @Test
  public void testGetString() throws JSONException {
    assertEquals(null, JSONObjectUtils.getString(jo, "a"));
    assertEquals("null", JSONObjectUtils.getString(jo, "b"));
    try {
      assertEquals(null, JSONObjectUtils.getString(jo, "c"));
      fail();
    } catch (JSONException e) {
    }
  }

  @Test
  public void testOptString() {
    assertEquals(null, JSONObjectUtils.optString(jo, "a"));
    assertEquals("null", JSONObjectUtils.optString(jo, "b"));
    assertEquals(null, JSONObjectUtils.optString(jo, "c"));
    assertEquals("", JSONObjectUtils.optString(jo, "c", ""));
  }
}
