/*
 * Copyright 2019 Hippo Seven
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

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.DisplayCutout;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

// Offset widgets to avoid notch
public class GalleryHeader extends FrameLayout {

  private SparseIntArray originalTopMargins = new SparseIntArray();
  private DisplayCutout displayCutout = null;
  private int[] location = new int[2];
  private Rect rect = new Rect();

  public GalleryHeader(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @RequiresApi(api = Build.VERSION_CODES.P)
  public void setDisplayCutout(@Nullable DisplayCutout displayCutout) {
    // Backup top margins
    if (originalTopMargins.size() == 0) {
      for (int i = 0; i < getChildCount(); i++) {
        View child = getChildAt(i);
        originalTopMargins.append(child.getId(), ((MarginLayoutParams) child.getLayoutParams()).topMargin);
      }
    }

    if (displayCutout != null) {
      // Update top margins
      if (getWidth() == 0 && getHeight() == 0) {
        this.displayCutout = displayCutout;
      } else {
        updateTopMargins(displayCutout);
      }
    } else if (originalTopMargins.size() != 0) {
      // Reset
      for (int i = 0; i < getChildCount(); i++) {
        View child = getChildAt(i);
        ((MarginLayoutParams) child.getLayoutParams()).topMargin = originalTopMargins.get(child.getId());
      }
      // Layout
      requestLayout();
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    if (displayCutout != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      updateTopMargins(displayCutout);
      displayCutout = null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.P)
  private void updateTopMargins(@NonNull DisplayCutout displayCutout) {
    getLocationOnScreen(location);
    int y = location[1];

    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      child.getLocationOnScreen(location);

      // Fix top margin
      boolean intersected = false;
      for (Rect notch : displayCutout.getBoundingRects()) {
        rect.set(location[0], location[1], location[0] + child.getWidth(), location[1] + child.getHeight());
        if (Rect.intersects(notch, rect)) {
          intersected = true;
          ((MarginLayoutParams) child.getLayoutParams()).topMargin = originalTopMargins.get(child.getId()) + (notch.bottom - y);
          break;
        }
      }
      if (!intersected) {
        ((MarginLayoutParams) child.getLayoutParams()).topMargin = originalTopMargins.get(child.getId());
      }

      // Layout
      requestLayout();
    }
  }
}
