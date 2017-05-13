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
 * Created by Hippo on 5/13/2017.
 */

import android.content.Context;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;

public class AutoGridLayout extends GridLayout {

  private int columnSize = -1;
  private boolean columnSizeChanged = true;

  public AutoGridLayout(Context context) {
    super(context);
  }

  public AutoGridLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AutoGridLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  /**
   * Set column size to adjust column count.
   */
  public void setColumnSize(int columnSize) {
    if (columnSize == this.columnSize) {
      return;
    }
    this.columnSize = columnSize;
    columnSizeChanged = true;
    requestLayout();
  }

  private static int calculateColumnCount(int total, int single) {
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

  private void adjustColumn(int widthSpec, int heightSpec) {
    if (columnSizeChanged && columnSize > 0) {
      int totalSpace;
      if (getOrientation() == HORIZONTAL &&
          MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY) {
        totalSpace = MeasureSpec.getSize(widthSpec);
      } else if (getOrientation() == VERTICAL &&
          MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) {
        totalSpace = MeasureSpec.getSize(heightSpec);
      } else {
        return;
      }

      int columnCount = calculateColumnCount(totalSpace, columnSize);
      setColumnCount(columnCount);
      columnSizeChanged = false;
    }
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    adjustColumn(widthSpec, heightSpec);
    super.onMeasure(widthSpec, heightSpec);
  }
}
