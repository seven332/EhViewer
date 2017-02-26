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
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.changehandler.ChangeHandlerAnimators;
import com.hippo.ehviewer.controller.base.MvpController;

/**
 * {@code RecolorStatusBarAnimatorChangeHandler} handles status bar color animator.
 * <p>
 * Only works for {@link MvpController}
 * in {@link EhvActivity}.
 */
public abstract class RecolorStatusBarAnimatorChangeHandler extends NullableAnimatorChangeHandler {

  private static final String KEY_DURATION = "RecolorStatusBarAnimatorChangeHandler.recolorStatusBarAnimationDuration";

  private long recolorStatusBarAnimationDuration = DEFAULT_ANIMATION_DURATION;

  @Override
  public void saveToBundle(@NonNull Bundle bundle) {
    super.saveToBundle(bundle);
    bundle.putLong(KEY_DURATION, recolorStatusBarAnimationDuration);
  }

  @Override
  public void restoreFromBundle(@NonNull Bundle bundle) {
    super.restoreFromBundle(bundle);
    recolorStatusBarAnimationDuration = bundle.getLong(KEY_DURATION);
  }

  public void setRecolorStatusBarAnimationDuration(long duration) {
    recolorStatusBarAnimationDuration = duration;
  }

  @Nullable
  @Override
  protected final Animator getAnimator2(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush, boolean toAddedToContainer) {
    Animator animator = getAnimator3(container, from, to, isPush, toAddedToContainer);
    if (animator == null) {
      return null;
    }

    Animator recolorBar = ChangeHandlerAnimators.recolorStatusBar(container, from, to);

    if (recolorBar == null) {
      return animator;
    }

    if (recolorStatusBarAnimationDuration >= 0) {
      recolorBar.setDuration(recolorStatusBarAnimationDuration);
    }

    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.playTogether(animator, recolorBar);
    return animatorSet;
  }

  @Nullable
  protected abstract Animator getAnimator3(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush, boolean toAddedToContainer);

  @Override
  protected void resetFromView(@NonNull View from) {}
}
