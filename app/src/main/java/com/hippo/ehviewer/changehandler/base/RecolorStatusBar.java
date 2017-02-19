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

package com.hippo.ehviewer.changehandler.base;

/*
 * Created by Hippo on 2/7/2017.
 */

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.controller.base.MvpController;
import com.hippo.ehviewer.util.IntProperty;
import com.hippo.ehviewer.widget.ControllerContainer;

/**
 * Creates recolor status bar animator for ControllerChangeHandler.
 * <p>
 * Only works for {@link MvpController}
 * in {@link EhvActivity}.
 */
public final class RecolorStatusBar {
  private RecolorStatusBar() {}

  private static final boolean SUPPORT_COLOR_BAR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

  private static final Property<DrawerLayout, Integer> STATUS_BAR_COLOR =
      new IntProperty<DrawerLayout>(null) {
        @Override
        public void setValue(DrawerLayout object, int value) {
          object.setStatusBarColor(value);
        }

        @Override
        public Integer get(DrawerLayout object) {
          return object.getStatusBarColor();
        }
      };

  /**
   * Creates recolor status bar animator for ControllerChangeHandler.
   * <p>
   * Return {@code null} if not supported.
   * <p>
   * Only works for {@link MvpController}
   * in {@link EhvActivity}.
   */
  @Nullable
  public static Animator createRecolorStatusBarAnimator(
      @NonNull ViewGroup container, @Nullable View from, @Nullable View to) {
    if (!SUPPORT_COLOR_BAR
        || !(container instanceof ControllerContainer)
        || from == null || to == null) {
      return null;
    }
    ControllerContainer controllerContainer = (ControllerContainer) container;
    DrawerLayout drawerLayout = controllerContainer.getDrawLayout();
    if (drawerLayout == null) {
      return null;
    }

    Object startObj = from.getTag(R.id.controller_status_bar_color);
    Object endObj = to.getTag(R.id.controller_status_bar_color);
    if (!(startObj instanceof Integer) || !(endObj instanceof Integer)) {
      return null;
    }

    ObjectAnimator statusBarAnimator = ObjectAnimator.ofInt(
        drawerLayout, STATUS_BAR_COLOR,
        (Integer) startObj, (Integer) endObj
    );
    statusBarAnimator.setEvaluator(new ArgbEvaluator());

    return statusBarAnimator;
  }
}
