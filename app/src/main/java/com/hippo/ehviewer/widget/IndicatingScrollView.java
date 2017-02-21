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
 * Created by Hippo on 2/11/2017.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import com.hippo.ehviewer.R;
import com.hippo.yorozuya.android.ResourcesUtils;

/**
 * {@code IndicatingScrollView} shows a indicating
 * at top or bottom if it can continue scrolling.
 */
public class IndicatingScrollView extends ObservedScrollView {

  private int indicatorHeight;
  private final Paint paint = new Paint();
  private final Rect rect = new Rect();

  public IndicatingScrollView(Context context) {
    super(context);
    init(context);
  }

  public IndicatingScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public IndicatingScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    if (isInEditMode()) {
      initEditMode(context);
      return;
    }

    indicatorHeight = context.getResources().getDimensionPixelSize(R.dimen.divider_thickness);
    paint.setColor(ResourcesUtils.getAttrColor(context, R.attr.dividerColor));
    paint.setStyle(Paint.Style.FILL);
  }

  private void initEditMode(Context context) {
    indicatorHeight = (int) (1 * context.getResources().getDisplayMetrics().density);
    paint.setColor(Color.BLACK);
    paint.setStyle(Paint.Style.FILL);
  }

  private void fillTopIndicatorDrawRect() {
    rect.set(0, 0, getWidth(), indicatorHeight);
  }

  private void fillBottomIndicatorDrawRect() {
    rect.set(0, getHeight() - indicatorHeight, getWidth(), getHeight());
  }

  private boolean needShowTopIndicator() {
    return canScrollVertically(-1);
  }

  private boolean needShowBottomIndicator() {
    return canScrollVertically(1);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    super.draw(canvas);

    final int restoreCount = canvas.save();
    canvas.translate(getScrollX(), getScrollY());

    // Draw top indicator
    if (needShowTopIndicator()) {
      fillTopIndicatorDrawRect();
      canvas.drawRect(rect, paint);
    }
    // Draw bottom indicator
    if (needShowBottomIndicator()) {
      fillBottomIndicatorDrawRect();
      canvas.drawRect(rect, paint);
    }

    canvas.restoreToCount(restoreCount);
  }
}
