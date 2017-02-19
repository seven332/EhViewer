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
 * Created by Hippo on 2/7/2017.
 */

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.changehandler.base.RecolorStatusBarAnimatorChangeHandler;
import com.hippo.yorozuya.android.AnimationUtils;

public class DefaultChangeHandler extends RecolorStatusBarAnimatorChangeHandler {

  private static final long ANIMATOR_DURATION = 300L;
  private static final long ANIMATOR_DELAY = 200L;

  @Nullable
  @Override
  protected Animator getAnimator3(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush, boolean toAddedToContainer) {
    if (from == null || to == null) {
      setRecolorStatusBarAnimationDuration(0);
      return ValueAnimator.ofFloat(0.0f, 1.0f);
    }

    setRecolorStatusBarAnimationDuration(ANIMATOR_DELAY + ANIMATOR_DURATION);
    int offsetY = container.getHeight() / 4;
    if (isPush) {
      Animator fromTranslationY = ObjectAnimator.ofFloat(from, View.TRANSLATION_Y, 0, -offsetY);
      Animator fromAlpha = ObjectAnimator.ofFloat(from, View.ALPHA, 1.0f, 0.0f);
      AnimatorSet fromSet = new AnimatorSet();
      fromSet.playTogether(fromTranslationY, fromAlpha);
      fromSet.setDuration(ANIMATOR_DURATION);
      fromSet.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);

      Animator toTranslationY = ObjectAnimator.ofFloat(to, View.TRANSLATION_Y, offsetY, 0);
      Animator toAlpha = ObjectAnimator.ofFloat(to, View.ALPHA, 0.0f, 1.0f);
      AnimatorSet toSet = new AnimatorSet();
      toSet.playTogether(toTranslationY, toAlpha);
      toSet.setDuration(ANIMATOR_DURATION);
      toSet.setStartDelay(ANIMATOR_DELAY);
      toSet.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
      to.setAlpha(0.0f);

      AnimatorSet set = new AnimatorSet();
      set.playTogether(fromSet, toSet);
      return set;
    } else {
      Animator fromTranslationY = ObjectAnimator.ofFloat(from, View.TRANSLATION_Y, 0, offsetY);
      Animator fromAlpha = ObjectAnimator.ofFloat(from, View.ALPHA, 1.0f, 0.0f);
      AnimatorSet fromSet = new AnimatorSet();
      fromSet.playTogether(fromTranslationY, fromAlpha);
      fromSet.setDuration(ANIMATOR_DURATION);
      fromSet.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);

      Animator toTranslationY = ObjectAnimator.ofFloat(to, View.TRANSLATION_Y, -offsetY, 0);
      Animator toAlpha = ObjectAnimator.ofFloat(to, View.ALPHA, 0.0f, 1.0f);
      AnimatorSet toSet = new AnimatorSet();
      toSet.playTogether(toTranslationY, toAlpha);
      toSet.setDuration(ANIMATOR_DURATION);
      toSet.setStartDelay(ANIMATOR_DELAY);
      toSet.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
      to.setAlpha(0.0f);

      AnimatorSet set = new AnimatorSet();
      set.playTogether(fromSet, toSet);
      return set;
    }
  }

  @Override
  protected void resetFromView(@NonNull View from) {
    from.setTranslationY(0.0f);
    from.setAlpha(1.0f);
  }
}
