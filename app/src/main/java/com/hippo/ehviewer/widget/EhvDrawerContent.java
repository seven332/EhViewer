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
 * Created by Hippo on 3/1/2017.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.hippo.drawerlayout.DrawerLayoutChild;

/**
 * Content for {@link com.hippo.drawerlayout.DrawerLayout}.
 */
public class EhvDrawerContent extends FrameLayout implements DrawerLayoutChild {

  private int layoutPaddingTop;
  private int layoutPaddingBottom;

  public EhvDrawerContent(Context context) {
    super(context);
  }

  public EhvDrawerContent(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public EhvDrawerContent(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void setFitPadding(int top, int bottom) {
    layoutPaddingTop = top;
    layoutPaddingBottom = bottom;
  }

  @Override
  public int getLayoutPaddingTop() {
    return layoutPaddingTop;
  }

  @Override
  public int getLayoutPaddingBottom() {
    return layoutPaddingBottom;
  }
}
