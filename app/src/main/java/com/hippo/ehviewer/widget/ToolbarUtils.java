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

package com.hippo.ehviewer.widget;

/*
 * Created by Hippo on 2/28/2017.
 */

import android.support.annotation.Nullable;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import com.hippo.yorozuya.StringUtils;
import java.lang.reflect.Field;

public final class ToolbarUtils {
  private ToolbarUtils() {}

  private static final String LOG_TAG = ToolbarUtils.class.getSimpleName();

  private static final Field FIELD_TITLE_TEXT_VIEW;
  private static final Field FIELD_MENU_VIEW;

  static {
    Field titleTextView;
    try {
      titleTextView = Toolbar.class.getDeclaredField("mTitleTextView");
      titleTextView.setAccessible(true);
    } catch (NoSuchFieldException e) {
      Log.d(LOG_TAG, "Can't get Toolbar.mTitleTextView field", e);
      titleTextView = null;
    }
    FIELD_TITLE_TEXT_VIEW = titleTextView;

    Field menuView;
    try {
      menuView = Toolbar.class.getDeclaredField("mMenuView");
      menuView.setAccessible(true);
    } catch (NoSuchFieldException e) {
      Log.d(LOG_TAG, "Can't get Toolbar.mMenuView field", e);
      menuView = null;
    }
    FIELD_MENU_VIEW = menuView;
  }

  /**
   * Gets title view for a toolbar.
   */
  @Nullable
  public static TextView getTitleView(Toolbar toolbar) {
    if (toolbar == null) {
      return null;
    }

    CharSequence title = toolbar.getTitle();
    if (StringUtils.isEmpty(title)) {
      return null;
    }

    try {
      Object obj = FIELD_TITLE_TEXT_VIEW.get(toolbar);
      if (obj instanceof TextView) {
        return (TextView) obj;
      } else {
        Log.d(LOG_TAG, "Toolbar.mTitleTextView is " + obj);
      }
    } catch (IllegalAccessException e) {
      Log.d(LOG_TAG, "Can't get Toolbar.mTitleTextView field", e);
    }
    return null;
  }

  /**
   * Gets action menu view for a toolbar.
   */
  @Nullable
  public static ActionMenuView getActionMenuView(Toolbar toolbar) {
    if (toolbar == null) {
      return null;
    }

    try {
      Object obj = FIELD_MENU_VIEW.get(toolbar);
      if (obj instanceof ActionMenuView) {
        return (ActionMenuView) obj;
      } else {
        Log.d(LOG_TAG, "Toolbar.mMenuView is " + obj);
      }
    } catch (IllegalAccessException e) {
      Log.d(LOG_TAG, "Can't get Toolbar.mMenuView field", e);
    }
    return null;
  }
}
