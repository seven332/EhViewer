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

package com.hippo.ehviewer.preference

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/*
 * Created by Hippo on 2017/7/30.
 */

@Config(manifest = Config.NONE)
@RunWith(ParameterizedRobolectricTestRunner::class)
class PreferenceTest<T>(
    creator: () -> Preferences.Preference<T>,
    private val defValue: T,
    private val anotherValue: T,
    testName: String
) {

  class TestPreferences : Preferences(RuntimeEnvironment.application.getSharedPreferences("ehv2", 0)) {
    val testBoolean = BooleanPreference("test_boolean", false)
    val testInt = IntPreference("test_int", 100)
    val testLong = LongPreference("test_long", 100L)
    val testFloat = FloatPreference("test_float", 100.0f)
    val testString = StringPreference("test_string", "100")
    val testDecimalInt = DecimalIntPreference("test_string", 100)
  }

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{index}-{3}")
    fun parameters(): List<Array<Any>> {
      return listOf(
          arrayOf<Any>({ TestPreferences().testBoolean }, false, true, "boolean"),
          arrayOf<Any>({ TestPreferences().testInt }, 100, 120, "int"),
          arrayOf<Any>({ TestPreferences().testLong }, 100L, 120L, "long"),
          arrayOf<Any>({ TestPreferences().testFloat }, 100.0f, 120.0f, "float"),
          arrayOf<Any>({ TestPreferences().testString }, "100", "120", "string"),
          arrayOf<Any>({ TestPreferences().testDecimalInt }, 100, 120, "decimal int")
      )
    }
  }

  private val preference = creator()

  @Test
  fun testValue() {
    assertEquals(defValue, preference.value)

    preference.value = anotherValue
    assertEquals(anotherValue, preference.value)
  }

  @Test
  fun testObservable() {
    val actual = mutableListOf<T>()
    val expected = mutableListOf<T>()
    preference.observable.subscribe { actual.add(it) }

    expected.add(defValue)
    assertEquals(expected, actual)

    preference.value = anotherValue
    expected.add(anotherValue)
    assertEquals(expected, actual)

    preference.value = anotherValue
    assertEquals(expected, actual)
  }
}
