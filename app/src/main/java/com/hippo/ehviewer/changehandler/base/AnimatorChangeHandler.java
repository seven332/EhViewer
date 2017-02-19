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
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

public abstract class AnimatorChangeHandler
    extends com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler {

  private static final String KEY_DURATION = "com.hippo.ehviewer.changehandler.base.AnimatorChangeHandler.duration";

  private long animationDuration = DEFAULT_ANIMATION_DURATION;

  @Override
  public void saveToBundle(@NonNull Bundle bundle) {
    super.saveToBundle(bundle);
    bundle.putLong(KEY_DURATION, animationDuration);
  }

  @Override
  public void restoreFromBundle(@NonNull Bundle bundle) {
    super.restoreFromBundle(bundle);
    animationDuration = bundle.getLong(KEY_DURATION);
  }

  @Override
  public long getAnimationDuration() {
    return animationDuration;
  }

  public void setAnimationDuration(long duration) {
    animationDuration = duration;
  }

  @NonNull
  @Override
  protected final Animator getAnimator(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush, boolean toAddedToContainer) {
    Animator animator = getAnimator2(container, from, to, isPush, toAddedToContainer);

    // Apply duration if it's valid
    if (animator != null && animationDuration >= 0) {
      animator.setDuration(animationDuration);
    }

    // Avoid null
    if (animator == null) {
      animator = ValueAnimator.ofFloat(0.0f, 1.0f);
      animator.setDuration(0);
    }

    return animator;
  }

  @Nullable
  protected abstract Animator getAnimator2(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush, boolean toAddedToContainer);
}
