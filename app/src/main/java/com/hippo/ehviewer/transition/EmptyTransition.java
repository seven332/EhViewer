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
 * Created by Hippo on 2/19/2017.
 */

import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionValues;

/**
 * {@code EmptyTransition} doesn't show any transition.
 */
public class EmptyTransition extends Transition {

  @Override
  public void captureStartValues(TransitionValues transitionValues) {}

  @Override
  public void captureEndValues(TransitionValues transitionValues) {}

  @Override
  public boolean isTransitionRequired(TransitionValues startValues, TransitionValues endValues) {
    return false;
  }
}
