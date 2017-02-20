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
 * Created by Hippo on 2/6/2017.
 */

import android.animation.Animator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.controller.base.MvpController;
import com.hippo.ehviewer.transition.RunAnimator;
import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionSet;

/**
 * {@code RecolorStatusBarTransitionChangeHandler} handles status bar color transition.
 * <p>
 * Only works for {@link MvpController}
 * in {@link EhvActivity}.
 */
public abstract class RecolorStatusBarTransitionChangeHandler extends NullableTransitionChangeHandler {

  @Nullable
  private Transition getRecolorBarTransition(@NonNull ViewGroup container,
      @Nullable View from, @Nullable View to) {
    Animator animator = RecolorStatusBar.createRecolorStatusBarAnimator(container, from, to);
    if (animator != null) {
      return new RunAnimator(animator);
    } else {
      return null;
    }
  }

  @Nullable
  @Override
  protected final Transition getTransition2(
      @NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush) {
    Transition transition = getTransition3(container, from, to, isPush);
    if (transition == null) {
      return null;
    }

    Transition recolorBar = getRecolorBarTransition(container, from, to);

    if (recolorBar == null) {
      return transition;
    }

    // Treat transition as TransitionSet if it is
    if (transition instanceof TransitionSet) {
      TransitionSet set = (TransitionSet) transition;
      if (set.getOrdering() == TransitionSet.ORDERING_TOGETHER) {
        set.addTransition(recolorBar);
        return transition;
      }
    }

    return new TransitionSet()
        .setOrdering(TransitionSet.ORDERING_TOGETHER)
        .addTransition(transition)
        .addTransition(recolorBar);
  }

  @Nullable
  protected abstract Transition getTransition3(
      @NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush);
}
