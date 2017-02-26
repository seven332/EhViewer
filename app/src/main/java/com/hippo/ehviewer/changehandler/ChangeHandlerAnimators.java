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

package com.hippo.ehviewer.changehandler;

/*
 * Created by Hippo on 2/26/2017.
 */

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.FloatProperty;
import com.hippo.ehviewer.util.IntProperty;
import com.hippo.ehviewer.widget.ControllerContainer;
import com.hippo.yorozuya.android.AnimationUtils;

public final class ChangeHandlerAnimators {
  private ChangeHandlerAnimators() {}

  ///////////////////////////////////////////////////////////////////////////
  // Recolor status bar
  ///////////////////////////////////////////////////////////////////////////

  private static final boolean SUPPORT_COLOR_BAR =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

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
   * Only works for {@link com.hippo.ehviewer.view.base.EhvView}
   * in {@link com.hippo.ehviewer.activity.EhvActivity}.
   */
  @Nullable
  public static Animator recolorStatusBar(
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
    if (!(startObj instanceof Integer) || !(endObj instanceof Integer) || startObj.equals(endObj)) {
      return null;
    }

    ObjectAnimator statusBarAnimator = ObjectAnimator.ofInt(
        drawerLayout, STATUS_BAR_COLOR,
        (Integer) startObj, (Integer) endObj
    );
    statusBarAnimator.setEvaluator(new ArgbEvaluator());

    return statusBarAnimator;
  }


  ///////////////////////////////////////////////////////////////////////////
  // Exchange content
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Remember to call {@code from.setTranslationY(0)} and {@code from.setAlpha(1.0f)}
   * in {@link com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler#resetFromView(View)}.
   *
   * @param offsetY the y distance to transfer content view
   * @param duration the duration for single content animator
   * @param delay the delay for to content animator
   */
  @Nullable
  public static Animator exchangeContent(@Nullable View from, @Nullable View to, float offsetY,
      long duration, long delay) {
    if (from == null || to == null) {
      return null;
    }

    Animator fromYAnimator = ObjectAnimator.ofFloat(from, View.TRANSLATION_Y, 0, offsetY);
    fromYAnimator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
    Animator fromAlphaAnimator = ObjectAnimator.ofFloat(from, View.ALPHA, 1.0f, 0.0f);
    AnimatorSet fromAnimator = new AnimatorSet();
    fromAnimator.playTogether(fromYAnimator, fromAlphaAnimator);
    fromAnimator.setDuration(duration);

    to.setTranslationY(offsetY);
    to.setAlpha(0.0f);
    Animator toYAnimator = ObjectAnimator.ofFloat(to, View.TRANSLATION_Y, offsetY, 0);
    toYAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
    Animator toAlphaAnimator = ObjectAnimator.ofFloat(to, View.ALPHA, 0.0f, 1.0f);
    AnimatorSet toAnimator = new AnimatorSet();
    toAnimator.playTogether(toYAnimator, toAlphaAnimator);
    toAnimator.setDuration(duration);
    toAnimator.setStartDelay(delay);

    AnimatorSet set = new AnimatorSet();
    set.playTogether(fromAnimator, toAnimator);
    return set;
  }


  ///////////////////////////////////////////////////////////////////////////
  // Change DrawerArrowDrawable
  ///////////////////////////////////////////////////////////////////////////

  private static final Property<DrawerArrowDrawable, Float> DRAWER_ARROW_DRAWABLE_PROGRESS =
      new FloatProperty<DrawerArrowDrawable>(null) {
        @Override
        public void setValue(DrawerArrowDrawable object, float value) {
          object.setProgress(value);
        }

        @Override
        public Float get(DrawerArrowDrawable object) {
          return object.getProgress();
        }
      };

  @Nullable
  public static Animator changeDrawerArrowDrawable(@Nullable DrawerArrowDrawable from,
      @Nullable DrawerArrowDrawable to) {
    if (from == null || to == null || from.getProgress() == to.getProgress()) {
      return null;
    }

    return ObjectAnimator.ofFloat(
        to, DRAWER_ARROW_DRAWABLE_PROGRESS,
        from.getProgress(), to.getProgress()
    );
  }
}
