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

package com.hippo.ehviewer.transition;

/*
 * Created by Hippo on 2/7/2017.
 */

import android.animation.Animator;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionValues;

/**
 * Runs an Animator in Transition.
 */
public class RunAnimator extends Transition {

  private static final String PROPNAME_CAUGHT = "run_animator:caught";
  private static final String PROPVALUE_CAUGHT_START = "start";
  private static final String PROPVALUE_CAUGHT_END = "end";
  private static final String[] PROPNAMES = {
      PROPNAME_CAUGHT,
  };

  private Animator animator;
  private boolean startCaught;
  private boolean endCaught;

  public RunAnimator(@NonNull Animator animator) {
    this.animator = animator;
  }

  @Override
  public void captureStartValues(TransitionValues transitionValues) {
    if (!startCaught) {
      startCaught = true;
      transitionValues.values.put(PROPNAME_CAUGHT, PROPVALUE_CAUGHT_START);
    }
  }

  @Override
  public void captureEndValues(TransitionValues transitionValues) {
    if (!endCaught) {
      endCaught = true;
      transitionValues.values.put(PROPNAME_CAUGHT, PROPVALUE_CAUGHT_END);
    }
  }

  @Override
  public String[] getTransitionProperties() {
    return PROPNAMES;
  }

  @Override
  public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
      TransitionValues endValues) {
    if (startValues == null || endValues == null
        || !PROPVALUE_CAUGHT_START.equals(startValues.values.get(PROPNAME_CAUGHT))
        || !PROPVALUE_CAUGHT_END.equals(endValues.values.get(PROPNAME_CAUGHT))) {
      return null;
    }

    Animator animator = this.animator;
    this.animator = null;
    return animator;
  }
}
