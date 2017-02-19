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
 * Created by Hippo on 1/26/2017.
 */

import android.animation.Animator;
import android.view.ViewGroup;
import com.hippo.ehviewer.widget.crossfade.CrossFadeView;
import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionValues;

/**
 * This transition uses {@link CrossFadeView} to catch previous view state
 * and fades between views. It performs better than {@link com.transitionseverywhere.Crossfade}.
 */
public class CrossFade extends Transition {

  private static final String PROPNAME_DATA = "cross_fade:data";
  private static final String[] PROPNAMES = {
      PROPNAME_DATA,
  };

  private void captureValues(TransitionValues transitionValues) {
    if (transitionValues.view instanceof CrossFadeView) {
      CrossFadeView crossFadeView = (CrossFadeView) transitionValues.view;
      transitionValues.values.put(PROPNAME_DATA, crossFadeView.cloneData());
    }
  }

  @Override
  public void captureStartValues(TransitionValues transitionValues) {
    captureValues(transitionValues);
  }

  @Override
  public void captureEndValues(TransitionValues transitionValues) {
    captureValues(transitionValues);
  }

  @Override
  public String[] getTransitionProperties() {
    return PROPNAMES;
  }

  @Override
  public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
      TransitionValues endValues) {
    // TODO A better way to check type
    if (startValues == null || endValues == null
        || !(startValues.view instanceof CrossFadeView) || !(endValues.view instanceof CrossFadeView)
        || (startValues.view.getClass() != endValues.view.getClass())) {
      return null;
    }

    int fromWidth = startValues.view.getWidth();
    int fromHeight = startValues.view.getHeight();
    Object fromData = startValues.values.get(PROPNAME_DATA);
    Object toData = endValues.values.get(PROPNAME_DATA);
    return ((CrossFadeView) endValues.view).crossFade(fromWidth, fromHeight, fromData, toData);
  }
}
