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
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import com.hippo.drawerlayout.DrawerLayoutChild;
import com.hippo.ehviewer.R;

/**
 * Left drawer for {@link com.hippo.ehviewer.activity.EhvActivity}.
 */
public class EhvLeftDrawer extends LinearLayout implements DrawerLayoutChild {

  private static final int SCRIM_COLOR = 0x44000000;
  private static final boolean DRAW_SCRIM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

  private int layoutPaddingTop;
  private int layoutPaddingBottom;
  private Paint paint;

  private NavigationView navigationView;
  private Button button;

  public EhvLeftDrawer(Context context) {
    super(context);
    init(context);
  }

  public EhvLeftDrawer(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public EhvLeftDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.widget_ehv_left_drawer, this);
    navigationView = (NavigationView) getChildAt(0);
    button = (Button) getChildAt(1);

    setOrientation(VERTICAL);
    if (DRAW_SCRIM) {
      setWillNotDraw(false);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Reset LayoutParams
    button.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int buttonWidth = button.getMeasuredWidth();
    int navigationWidth = navigationView.getMeasuredWidth();
    if (buttonWidth > navigationWidth) {
      // Change LayoutParams to make button fit parent
      button.getLayoutParams().width = navigationWidth;
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

  /**
   * Gets the {@link NavigationView}.
   */
  public NavigationView getNavigationView() {
    return navigationView;
  }

  /**
   * Gets the bottom button.
   */
  public Button getBottomButton() {
    return button;
  }
}
