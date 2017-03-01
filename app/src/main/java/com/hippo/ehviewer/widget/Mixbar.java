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
 * Created by Hippo on 1/29/2017.
 */

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * {Mixbar} uses one {@code Toolbar} as tool bar,
 * uses one {@code Toolbar} as action bar.
 * Tool bar is always shown.
 * Action bar is only shown and covers tool bar in action mode.
 */
public class Mixbar extends FrameLayout {

  private Toolbar toolbar;
  private Toolbar actionbar;
  private boolean inActionMode;

  public Mixbar(Context context) {
    super(context);
  }

  public Mixbar(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public Mixbar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void addView(View child, int index, final ViewGroup.LayoutParams params) {
    if (child instanceof Toolbar) {
      if (toolbar == null) {
        toolbar = (Toolbar) child;
      } else if (actionbar == null) {
        actionbar = (Toolbar) child;
        // Hide actionbar at first
        actionbar.setVisibility(GONE);
      }
    }
    super.addView(child, index, params);
  }

  /**
   * Return the tool bar.
   *
   * @return the tool bar
   */
  public Toolbar getToolbar() {
    if (toolbar == null) {
      throw new IllegalStateException("No toolbar in mixbar");
    }
    return toolbar;
  }

  /**
   * Starts action mode.
   *
   * @return the action bar
   */
  public Toolbar startActionMode() {
    if (!inActionMode) {
      inActionMode = true;
      actionbar.setVisibility(VISIBLE);
    }
    return actionbar;
  }

  /**
   * Ends action mode.
   */
  public void endActionMode() {
    if (inActionMode) {
      inActionMode = false;
      actionbar.setVisibility(GONE);
      // TODO clear actionbar
    }
  }
}
