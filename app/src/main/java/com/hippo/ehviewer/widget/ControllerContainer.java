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
 * Created by Hippo on 2/6/2017.
 */

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import com.bluelinelabs.conductor.ChangeHandlerFrameLayout;
import com.hippo.drawerlayout.DrawerLayout;

/**
 * {@code ControllerContainer} is a container for controllers.
 */
public class ControllerContainer extends ChangeHandlerFrameLayout {

  @Nullable
  private DrawerLayout drawerLayout;

  public ControllerContainer(Context context) {
    super(context);
  }

  public ControllerContainer(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ControllerContainer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /**
   * Gets DrawerLayout of the Activity.
   */
  @Nullable
  public DrawerLayout getDrawLayout() {
    return drawerLayout;
  }

  /**
   * Sets DrawerLayout of the Activity.
   */
  public void setDrawLayout(DrawerLayout drawLayout) {
    this.drawerLayout = drawLayout;
  }
}
