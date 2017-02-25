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
 * Created by Hippo on 2/18/2017.
 */

import android.content.Context;
import android.content.res.Resources;
import com.hippo.ehviewer.R;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class ReadableTime {

  public static final long SECOND_MILLIS = 1000;
  public static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
  public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
  public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
  public static final long WEEK_MILLIS = 7 * DAY_MILLIS;
  public static final long YEAR_MILLIS = 365 * DAY_MILLIS;

  private static final SimpleDateFormat DATE_FORMAT_WITHOUT_YEAR =
      new SimpleDateFormat("MMM d", Locale.getDefault());
  private static final SimpleDateFormat DATE_FORMAT_WITHOUT_YEAR_ZH =
      new SimpleDateFormat("M月d日", Locale.getDefault());
  private static final SimpleDateFormat DATE_FORMAT_WIT_YEAR =
      new SimpleDateFormat("yyyy MMM d", Locale.getDefault());
  private static final SimpleDateFormat DATE_FORMAT_WIT_YEAR_ZH =
      new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());

  private static final Calendar CALENDAR = Calendar.getInstance();

  private static final Object CALENDAR_LOCK = new Object();

  public static String getTimeAgo(Context context, long time) {
    Resources resources = context.getResources();

    long now = System.currentTimeMillis();
    if (time > now + (10 * MINUTE_MILLIS) || time < 0) {
      return context.getString(R.string.from_the_future);
    }

    final long diff = now - time;
    if (diff < MINUTE_MILLIS) {
      return resources.getString(R.string.just_now);

    } else if (diff < 2 * MINUTE_MILLIS) {
      return resources.getQuantityString(R.plurals.some_minutes_ago, 1, 1);

    } else if (diff < 50 * MINUTE_MILLIS) {
      int minutes = (int) (diff / MINUTE_MILLIS);
      return resources.getQuantityString(R.plurals.some_minutes_ago, minutes, minutes);

    } else if (diff < 90 * MINUTE_MILLIS) {
      return resources.getQuantityString(R.plurals.some_hours_ago, 1, 1);

    } else if (diff < 48 * HOUR_MILLIS) {
      int hours = (int) (diff / HOUR_MILLIS);
      return resources.getQuantityString(R.plurals.some_hours_ago, hours, hours);

    } else if (diff < 2 * WEEK_MILLIS) {
      int days = (int) (diff / DAY_MILLIS);
      return resources.getString(R.string.some_days_ago, days);

    } else {
      synchronized (CALENDAR_LOCK) {
        Date nowDate = new Date(now);
        Date timeDate = new Date(time);
        CALENDAR.setTime(nowDate);
        int nowYear = CALENDAR.get(Calendar.YEAR);
        CALENDAR.setTime(timeDate);
        int timeYear = CALENDAR.get(Calendar.YEAR);
        boolean chinese = resources.getBoolean(R.bool.chinese_time);
        if (nowYear == timeYear) {
          DateFormat format = chinese ? DATE_FORMAT_WITHOUT_YEAR_ZH : DATE_FORMAT_WITHOUT_YEAR;
          return format.format(timeDate);
        } else {
          DateFormat format = chinese ? DATE_FORMAT_WIT_YEAR_ZH : DATE_FORMAT_WIT_YEAR;
          return format.format(timeDate);
        }
      }
    }
  }
}
