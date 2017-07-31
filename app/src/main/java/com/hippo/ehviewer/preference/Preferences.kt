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

import android.content.SharedPreferences
import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable

/*
 * Created by Hippo on 6/29/2017.
 */

/**
 * Base preferences.
 */
abstract class Preferences(protected val pref: SharedPreferences) {

  private inline fun <T> setValue(
      key: String,
      value: T,
      getter: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor
  ) {
    pref.edit().getter(key, value).apply()
  }

  private inline fun <T> getValue(
      key: String,
      defValue: T,
      typeName: String,
      getter: SharedPreferences.(String, T) -> T
  ): T {
    try {
      return pref.getter(key, defValue)
    } catch (e: ClassCastException) {
      Log.e("Preferences", "The value of $key is not a $typeName.", e)
      return defValue
    }
  }

  protected fun setBoolean(key: String, value: Boolean) =
      setValue(key, value, SharedPreferences.Editor::putBoolean)

  protected fun getBoolean(key: String, defValue: Boolean) =
      getValue(key, defValue, "boolean", SharedPreferences::getBoolean)

  protected fun setInt(key: String, value: Int) =
      setValue(key, value, SharedPreferences.Editor::putInt)

  protected fun getInt(key: String, defValue: Int) =
      getValue(key, defValue, "int", SharedPreferences::getInt)

  protected fun setLong(key: String, value: Long) =
      setValue(key, value, SharedPreferences.Editor::putLong)

  protected fun getLong(key: String, defValue: Long) =
      getValue(key, defValue, "long", SharedPreferences::getLong)

  protected fun setFloat(key: String, value: Float) =
      setValue(key, value, SharedPreferences.Editor::putFloat)

  protected fun getFloat(key: String, defValue: Float) =
      getValue(key, defValue, "float", SharedPreferences::getFloat)

  protected fun setString(key: String, value: String) =
      setValue(key, value, SharedPreferences.Editor::putString)

  protected fun getString(key: String, defValue: String) =
      getValue(key, defValue, "string", SharedPreferences::getString)!!

  protected fun setDecimalInt(key: String, value: Int) =
      setValue(key, value.toString(), SharedPreferences.Editor::putString)

  protected fun getDecimalInt(key: String, defValue: Int) =
      getValue(key, defValue.toString(), "decimal int", SharedPreferences::getString).toIntOrNull() ?: defValue

  protected fun edit(): SharedPreferences.Editor {
    return pref.edit()
  }

  abstract inner class Preference<T>(
      private val key: String,
      defValue: T,
      getter: Preferences.(String, T) -> T,
      private val setter: Preferences.(String, T) -> Unit
  ) {
    private var relay : Relay<T>? = null

    var value: T = getter(key, defValue)
      set(value) {
        if (field != value) {
          field = value
          setter(key, value)
          relay?.accept(value)
        }
      }

    val observable: Observable<T>
      get() = relay ?: BehaviorRelay.createDefault(value).also { relay = it }
  }

  inner class BooleanPreference(key: String, defValue: Boolean) : Preference<Boolean>(key, defValue, Preferences::getBoolean, Preferences::setBoolean)
  inner class IntPreference(key: String, defValue: Int) : Preference<Int>(key, defValue, Preferences::getInt, Preferences::setInt)
  inner class LongPreference(key: String, defValue: Long) : Preference<Long>(key, defValue, Preferences::getLong, Preferences::setLong)
  inner class FloatPreference(key: String, defValue: Float) : Preference<Float>(key, defValue, Preferences::getFloat, Preferences::setFloat)
  inner class StringPreference(key: String, defValue: String) : Preference<String>(key, defValue, Preferences::getString, Preferences::setString)
  inner class DecimalIntPreference(key: String, defValue: Int) : Preference<Int>(key, defValue, Preferences::getDecimalInt, Preferences::setDecimalInt)
}
