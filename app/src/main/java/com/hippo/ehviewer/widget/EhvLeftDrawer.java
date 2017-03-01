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
 * Created by Hippo on 2/7/2017.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.util.AttributeSet;
import com.hippo.drawerlayout.DrawerLayoutChild;

/**
 * Left drawer for {@link com.hippo.ehviewer.activity.EhvActivity}.
 */
public class EhvLeftDrawer extends NavigationView implements DrawerLayoutChild {

  private static final int SCRIM_COLOR = 0x44000000;
  private static final boolean DRAW_SCRIM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

  private int layoutPaddingTop;
  private int layoutPaddingBottom;
  private Paint paint;

  public EhvLeftDrawer(Context context) {
    super(context);
    init();
  }

  public EhvLeftDrawer(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public EhvLeftDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    if (DRAW_SCRIM) {
      setWillNotDraw(false);
    }
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    super.draw(canvas);

    if (DRAW_SCRIM && layoutPaddingTop > 0) {
      if (paint == null) {
        paint = new Paint();
        paint.setColor(SCRIM_COLOR);
      }
      canvas.drawRect(0, 0, getWidth(), layoutPaddingTop, paint);
    }
  }

  @Override
  public void setFitPadding(int top, int bottom) {
    layoutPaddingTop = top;
    layoutPaddingBottom = bottom;
  }

  @Override
  public int getLayoutPaddingTop() {
    return 0;
  }

  @Override
  public int getLayoutPaddingBottom() {
    return layoutPaddingBottom;
  }
}
