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

package com.hippo.ehviewer.network

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.hippo.ehviewer.database.MSQLiteBuilder
import com.hippo.ehviewer.util.getBoolean
import com.hippo.ehviewer.util.getLong
import com.hippo.ehviewer.util.getString
import com.hippo.ehviewer.util.rawQuery
import com.hippo.ehviewer.util.transaction
import okhttp3.Cookie
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Created by Hippo on 6/29/2017.
 */

internal class CookieDatabase(context: Context, name: String) {

  companion object {
    private val LOG_TAG = CookieDatabase::class.java.simpleName

    private const val VERSION_1 = 1
    private const val TABLE_COOKIE = "ok_http_cookie"
    private const val COLUMN_ID = MSQLiteBuilder.COLUMN_ID
    private const val COLUMN_NAME = "name"
    private const val COLUMN_VALUE = "value"
    private const val COLUMN_EXPIRES_AT = "expires_at"
    private const val COLUMN_DOMAIN = "domain"
    private const val COLUMN_PATH = "path"
    private const val COLUMN_SECURE = "secure"
    private const val COLUMN_HTTP_ONLY = "http_only"
    private const val COLUMN_PERSISTENT = "persistent"
    private const val COLUMN_HOST_ONLY = "host_only"

    private const val DB_VERSION = VERSION_1
  }

  private val cookieIdMap = mutableMapOf<Cookie, Long>()
  private val getAllCookiesCalled = AtomicBoolean()

  private val helper = MSQLiteBuilder()
      .version(VERSION_1)
      .createTable(TABLE_COOKIE)
      .insertColumn(TABLE_COOKIE, COLUMN_NAME, String::class)
      .insertColumn(TABLE_COOKIE, COLUMN_VALUE, String::class)
      .insertColumn(TABLE_COOKIE, COLUMN_EXPIRES_AT, Long::class)
      .insertColumn(TABLE_COOKIE, COLUMN_DOMAIN, String::class)
      .insertColumn(TABLE_COOKIE, COLUMN_PATH, String::class)
      .insertColumn(TABLE_COOKIE, COLUMN_SECURE, Boolean::class)
      .insertColumn(TABLE_COOKIE, COLUMN_HTTP_ONLY, Boolean::class)
      .insertColumn(TABLE_COOKIE, COLUMN_PERSISTENT, Boolean::class)
      .insertColumn(TABLE_COOKIE, COLUMN_HOST_ONLY, Boolean::class)
      .build(context, name, DB_VERSION)
  private val db = helper.writableDatabase

  private fun Cursor.getCookie(currentTimeMillis: Long): Cookie? {
    val name = getString(COLUMN_NAME, null) ?: return null
    val value = getString(COLUMN_VALUE, null)
    val expiresAt = getLong(COLUMN_EXPIRES_AT, 0)
    val domain = getString(COLUMN_DOMAIN, null) ?: return null
    val path = getString(COLUMN_PATH, null) ?: return null
    val secure = getBoolean(COLUMN_SECURE, false)
    val httpOnly = getBoolean(COLUMN_HTTP_ONLY, false)
    val persistent = getBoolean(COLUMN_PERSISTENT, false)
    val hostOnly = getBoolean(COLUMN_HOST_ONLY, false)

    // Check non-persistent or expired
    if (!persistent || expiresAt <= currentTimeMillis) {
      return null
    }

    val builder = Cookie.Builder()
    builder.name(name)
    builder.value(value)
    if (hostOnly) {
      builder.hostOnlyDomain(domain)
    } else {
      builder.domain(domain)
    }
    builder.path(path)
    builder.expiresAt(expiresAt)
    if (secure) builder.secure()
    if (httpOnly) builder.httpOnly()
    return builder.build()
  }

  /**
   * Return a domain-cookies map.
   */
  fun getAllCookies(): MutableMap<String, CookieSet> {
    if (!getAllCookiesCalled.compareAndSet(false, true)) {
      throw IllegalStateException("Only call getAllCookies() once.")
    }

    val now = System.currentTimeMillis()
    val map = mutableMapOf<String, CookieSet>()
    val toRemove = mutableListOf<Long>()

    db.rawQuery("SELECT * FROM $TABLE_COOKIE;", null) {
      val id = it.getLong(COLUMN_ID, 0)
      val cookie = it.getCookie(now)

      if (cookie != null) {
        // Save id of the cookie in db
        cookieIdMap.put(cookie, id)
        // Put cookie to set
        var set: CookieSet? = map[cookie.domain()]
        if (set == null) {
          set = CookieSet()
          map.put(cookie.domain(), set)
        }
        set.add(cookie)
      } else {
        // Mark to remove the cookie
        toRemove.add(id)
      }
    }

    // Remove invalid or expired cookie
    if (!toRemove.isEmpty()) {
      val statement = db.compileStatement(
          "DELETE FROM $TABLE_COOKIE WHERE $COLUMN_ID = ?;")
      db.transaction {
        for (id in toRemove) {
          statement.bindLong(1, id)
          statement.executeUpdateDelete()
        }
      }
    }

    return map
  }

  private fun Cookie.toContentValues() = ContentValues(9)
      .also {
        it.put(COLUMN_NAME, name())
        it.put(COLUMN_VALUE, value())
        it.put(COLUMN_EXPIRES_AT, expiresAt())
        it.put(COLUMN_DOMAIN, domain())
        it.put(COLUMN_PATH, path())
        it.put(COLUMN_SECURE, secure())
        it.put(COLUMN_HTTP_ONLY, httpOnly())
        it.put(COLUMN_PERSISTENT, persistent())
        it.put(COLUMN_HOST_ONLY, hostOnly())
      }

  fun add(cookie: Cookie) {
    val id = db.insert(TABLE_COOKIE, null, cookie.toContentValues())
    if (id != -1L) {
      val oldId = cookieIdMap.put(cookie, id)
      if (oldId != null) Log.e(LOG_TAG, "Add a duplicate cookie")
    } else {
      Log.e(LOG_TAG, "An error occurred when insert a cookie")
    }
  }

  fun update(from: Cookie, to: Cookie) {
    val id = cookieIdMap[from]
    if (id == null) {
      Log.e(LOG_TAG, "Can't get id when update the cookie")
      return
    }

    val values = to.toContentValues()
    val whereClause = "$COLUMN_ID = ?"
    val whereArgs = arrayOf(id.toString())
    val count = db.update(TABLE_COOKIE, values, whereClause, whereArgs)
    if (count != 1) Log.e(LOG_TAG, "Bad result when update cookie: $count")

    // Update it in cookie-id map
    cookieIdMap.remove(from)
    cookieIdMap.put(to, id)
  }

  fun remove(cookie: Cookie) {
    val id = cookieIdMap[cookie]
    if (id == null) {
      Log.e(LOG_TAG, "Can't get id when remove the cookie")
      return
    }

    val whereClause = "$COLUMN_ID = ?"
    val whereArgs = arrayOf(id.toString())
    val count = db.delete(TABLE_COOKIE, whereClause, whereArgs)
    if (count != 1) Log.e(LOG_TAG, "Bad result when remove cookie: $count")

    // Remove it from cookie-id map
    cookieIdMap.remove(cookie)
  }

  fun clear() {
    db.delete(TABLE_COOKIE, null, null)
    cookieIdMap.clear()
  }

  fun close() {
    db.close()
    helper.close()
  }
}
