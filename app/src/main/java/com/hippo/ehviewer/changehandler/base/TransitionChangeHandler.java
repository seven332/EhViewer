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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.ControllerChangeHandler;
import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionManager;

/**
 * A base {@link ControllerChangeHandler} that facilitates using {@link android.transition.Transition}s to replace Controller Views.
 */
public abstract class TransitionChangeHandler extends ControllerChangeHandler {

  private boolean canceled;

  /**
   * Should be overridden to return the Transition to use while replacing Views.
   *
   * @param container The container these Views are hosted in
   * @param from The previous View in the container or {@code null} if there was no Controller before this transition
   * @param to The next View that should be put in the container or {@code null} if no Controller is being transitioned to
   * @param isPush True if this is a push transaction, false if it's a pop
   */
  @NonNull
  protected abstract Transition getTransition(@NonNull ViewGroup container, @Nullable View from, @Nullable View to, boolean isPush);

  @Override
  public void onAbortPush(@NonNull ControllerChangeHandler newHandler, @Nullable Controller newTop) {
    super.onAbortPush(newHandler, newTop);

    canceled = true;
  }

  @Override
  public void performChange(@NonNull final ViewGroup container, @Nullable View from, @Nullable View to, boolean isPush, @NonNull final ControllerChangeCompletedListener changeListener) {
    if (canceled) {
      changeListener.onChangeCompleted();
      return;
    }

    Transition transition = getTransition(container, from, to, isPush);
    transition.addListener(new Transition.TransitionListener() {
      @Override
      public void onTransitionStart(Transition transition) {}

      @Override
      public void onTransitionEnd(Transition transition) {
        changeListener.onChangeCompleted();
      }

      @Override
      public void onTransitionCancel(Transition transition) {
        changeListener.onChangeCompleted();
      }

      @Override
      public void onTransitionPause(Transition transition) {}

      @Override
      public void onTransitionResume(Transition transition) {}
    });

    TransitionManager.beginDelayedTransition(container, transition);
    if (from != null) {
      container.removeView(from);
    }
    if (to != null && to.getParent() == null) {
      container.addView(to);
    }
  }

  @Override
  public final boolean removesFromViewOnPush() {
    return true;
  }
}
