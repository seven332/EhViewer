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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * {@code DividerItemDecoration} draws divider between items.
 * <p>
 * Works for {@link GridLayoutManager} and {@link android.support.v7.widget.LinearLayoutManager}
 * with default configures.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

  private Paint paint;

  public DividerItemDecoration(@ColorInt int color, int thickness) {
    paint = new Paint();
    paint.setColor(color);
    paint.setStrokeWidth(thickness);
    paint.setStyle(Paint.Style.STROKE);
  }

  @Override
  public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
    RecyclerView.Adapter adapter = parent.getAdapter();
    if (adapter == null) {
      return;
    }

    int span;
    RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
    if (layoutManager instanceof GridLayoutManager) {
      span = ((GridLayoutManager) layoutManager).getSpanCount();
    } else {
      span = 1;
    }
    int width = parent.getWidth();
    int height = parent.getHeight();

    // Draw warps
    if (span > 1) {
      int paddingLeft = parent.getPaddingLeft();
      int paddingRight = parent.getPaddingRight();
      int spanSize = (width - paddingLeft - paddingRight) / span;

      for (int i = 1; i < span; ++i) {
        int x = paddingLeft + i * spanSize;
        c.drawLine(x, 0, x, height, paint);
      }
    }

    // Draw wefts
    for (int i = 0, n = parent.getChildCount(); i < n; i += span) {
      final View child = parent.getChildAt(i);
      int childBottom = child.getBottom();
      c.drawLine(0, childBottom, width, childBottom, paint);
    }
  }
}
