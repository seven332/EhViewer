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

import android.content.Context
import com.hippo.ehviewer.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Created by Hippo on 6/9/2017.
 */

const private val SECOND_MILLIS = 1000L
const private val MINUTE_MILLIS = 60 * SECOND_MILLIS
const private val HOUR_MILLIS = 60 * MINUTE_MILLIS
const private val DAY_MILLIS = 24 * HOUR_MILLIS
const private val WEEK_MILLIS = 7 * DAY_MILLIS

private val CALENDAR = Calendar.getInstance()
private val DATE_FORMAT = SimpleDateFormat("M/d", Locale.getDefault())
private val DATE_FORMAT_WITH_YEAR = SimpleDateFormat("y/M/d", Locale.getDefault())
private val LOCK = Any()

fun Long.prettyTime(context: Context, now: Long = System.currentTimeMillis()): String {
  val diff = (now - this).abs()
  val ago = now > this

  // If the interval is longer than a week, use time instead of interval
  if (diff >= WEEK_MILLIS) {
    synchronized (LOCK) {
      val nowDate = Date(now)
      val timeDate = Date(this)
      CALENDAR.time = nowDate
      val nowYear = CALENDAR.get(Calendar.YEAR)
      CALENDAR.time = timeDate
      val timeYear = CALENDAR.get(Calendar.YEAR)
      if (nowYear == timeYear) {
        return DATE_FORMAT.format(timeDate)
      } else {
        return DATE_FORMAT_WITH_YEAR.format(timeDate)
      }
    }
  }

  if (diff < 2 * MINUTE_MILLIS) {
    return context.string(R.string.pretty_time_just_now)
  }

  val time: String
  if (diff < 50 * MINUTE_MILLIS) {
    val minutes = (diff / MINUTE_MILLIS).toInt()
    time = context.string(R.string.pretty_time_minutes, minutes)
  } else if (diff < 90 * MINUTE_MILLIS) {
    val hours = 1
    time = context.string(R.string.pretty_time_hours, hours)
  } else if (diff < DAY_MILLIS) {
    val hours = (diff / HOUR_MILLIS).toInt()
    time = context.string(R.string.pretty_time_hours, hours)
  } else {
    val days = (diff / DAY_MILLIS).toInt()
    time = context.string(R.string.pretty_time_days, days)
  }

  return context.string(if (ago) R.string.pretty_time_ago else R.string.pretty_time_before, time)
}
