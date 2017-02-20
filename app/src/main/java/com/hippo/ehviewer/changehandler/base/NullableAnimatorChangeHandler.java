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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler;

public abstract class NullableAnimatorChangeHandler extends AnimatorChangeHandler {

  @NonNull
  @Override
  protected final Animator getAnimator(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush, boolean toAddedToContainer) {
    Animator animator = getAnimator2(container, from, to, isPush, toAddedToContainer);

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
