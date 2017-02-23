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
 * Created by Hippo on 2/22/2017.
 */

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * {@code AutoGridLayoutManager} {@code setSpanCount} based on {@code ColumnSize}.
 */
public class AutoGridLayoutManager extends GridLayoutManager {

  private int mColumnSize = -1;
  private boolean mColumnSizeChanged = true;

  public AutoGridLayoutManager(Context context, int columnSize) {
    super(context, 1);
    setColumnSize(columnSize);
  }

  public AutoGridLayoutManager(Context context, int columnSize, int orientation, boolean reverseLayout) {
    super(context, 1, orientation, reverseLayout);
    setColumnSize(columnSize);
  }

  /**
   * Set column size to change span count.
   */
  public void setColumnSize(int columnSize) {
    if (columnSize == mColumnSize) {
      return;
    }
    mColumnSize = columnSize;
    mColumnSizeChanged = true;
    requestLayout();
  }

  private static int calculateSpanCount(int total, int single) {
    int span = total / single;
    if (span <= 0) {
      return 1;
    }
    // If (span + 1) is acceptable, return it
    if (total / (span + 1) >= single * 8 / 10) {
      return span + 1;
    } else {
      return span;
    }
  }

  @Override
  public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
    if (mColumnSizeChanged && mColumnSize > 0) {
      int totalSpace;
      if (getOrientation() == VERTICAL) {
        totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
      } else {
        totalSpace = getHeight() - getPaddingTop() - getPaddingBottom();
      }

      int spanCount = calculateSpanCount(totalSpace, mColumnSize);
      setSpanCount(spanCount);
      mColumnSizeChanged = false;
    }
    super.onLayoutChildren(recycler, state);
  }
}
