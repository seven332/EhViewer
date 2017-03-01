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
 * Created by Hippo on 2/26/2017.
 */

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ToolbarLayout extends HeaderLayout {

  private static final String LOG_TAG = ToolbarLayout.class.getSimpleName();

  private DrawerArrowDrawable drawable;

  public ToolbarLayout(Context context) {
    super(context);
  }

  public ToolbarLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ToolbarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setDrawerArrowDrawable(DrawerArrowDrawable drawable) {
    this.drawable = drawable;
  }

  public DrawerArrowDrawable getDrawerArrowDrawable() {
    return drawable;
  }

  @Nullable
  @Override
  public Toolbar getActualHeader() {
    View view = super.getActualHeader();
    if (view instanceof Toolbar) {
      return (Toolbar) view;
    } else {
      Log.e(LOG_TAG, "The actual header of ToolbarLayout must be CrossFadeToolbar");
      return null;
    }
  }
}
