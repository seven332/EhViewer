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

import static org.junit.Assert.assertEquals;

import android.os.Build;
import com.hippo.ehviewer.BuildConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class PreferencesTest {

  private static int id;

  private Preferences p;

  @Before
  public void before() {
    ShadowLog.stream = System.out;
    p = new Preferences(RuntimeEnvironment.application.getSharedPreferences(
        "shared_preferences_" + id++,
        0
    ));
  }

  @Test
  public void testBoolean() {
    assertEquals(true, p.getBoolean("BOOLEAN_1", true));
    p.putBoolean("BOOLEAN_1", false);
    assertEquals(false, p.getBoolean("BOOLEAN_1", true));
    p.putBoolean("BOOLEAN_1", true);
    assertEquals(true, p.getBoolean("BOOLEAN_1", true));

    p.putDecimalInt("BOOLEAN_1", 56);
    assertEquals(false, p.getBoolean("BOOLEAN_1", false));
  }

  @Test
  public void testInt() {
    assertEquals(56, p.getInt("INT_1", 56));
    p.putInt("INT_1", 78);
    assertEquals(78, p.getInt("INT_1", 56));
    p.putInt("INT_1", 98);
    assertEquals(98, p.getInt("INT_1", 56));

    p.putBoolean("INT_1", false);
    assertEquals(56, p.getInt("INT_1", 56));
  }

  @Test
  public void testLong() {
    assertEquals(56L, p.getLong("LONG_1", 56L));
    p.putLong("LONG_1", 78L);
    assertEquals(78L, p.getLong("LONG_1", 56L));
    p.putLong("LONG_1", 98L);
    assertEquals(98L, p.getLong("LONG_1", 56L));

    p.putInt("LONG_1", 66);
    assertEquals(56L, p.getLong("LONG_1", 56L));
  }

  @Test
  public void testFloat() {
    assertEquals(56.0f, p.getFloat("FLOAT_1", 56.0f), 0.0f);
    p.putFloat("FLOAT_1", 78.0f);
    assertEquals(78.0f, p.getFloat("FLOAT_1", 56.0f), 0.0f);
    p.putFloat("FLOAT_1", 98.0f);
    assertEquals(98.0f, p.getFloat("FLOAT_1", 56.0f), 0.0f);

    p.putLong("FLOAT_1", 66L);
    assertEquals(56.0f, p.getFloat("FLOAT_1", 56.0f), 0.0f);
  }

  @Test
  public void testString() {
    assertEquals("56", p.getString("STRING_1", "56"));
    p.putString("STRING_1", "78");
    assertEquals("78", p.getString("STRING_1", "56"));
    p.putString("STRING_1", "98");
    assertEquals("98", p.getString("STRING_1", "56"));

    p.putLong("STRING_1", 66L);
    assertEquals("56", p.getString("STRING_1", "56"));
  }

  @Test
  public void testDecimalInt() {
    assertEquals(56, p.getDecimalInt("DECIMAL_INT_1", 56));
    p.putDecimalInt("DECIMAL_INT_1", 78);
    assertEquals(78, p.getDecimalInt("DECIMAL_INT_1", 56));
    assertEquals("78", p.getString("DECIMAL_INT_1", "56"));
    p.putDecimalInt("DECIMAL_INT_1", 98);
    assertEquals(98, p.getDecimalInt("DECIMAL_INT_1", 56));
    assertEquals("98", p.getString("DECIMAL_INT_1", "56"));

    p.putString("DECIMAL_INT_1", "345");
    assertEquals(345, p.getDecimalInt("DECIMAL_INT_1", 56));
    p.putInt("DECIMAL_INT_1", 77);
    assertEquals(56, p.getDecimalInt("DECIMAL_INT_1", 56));
  }

  public void testEditor() {
    p.edit().putString("STRING_2", "asdfg").putInt("INT_2", 54321).apply();
    assertEquals("asdfg", p.getString("STRING_2", "gfdsa"));
    assertEquals(54321, p.getInt("INT_2", 12345));
  }
}
