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
 * Created by Hippo on 2/20/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.transition.EmptyTransition;
import com.transitionseverywhere.Transition;

public abstract class NullableTransitionChangeHandler extends TransitionChangeHandler {

  @NonNull
  @Override
  protected final Transition getTransition(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush) {
    Transition transition = getTransition2(container, from, to, isPush);

    if (transition == null) {
      transition = new EmptyTransition();
    }

    return transition;
  }

  @Nullable
  protected abstract Transition getTransition2(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush);
}
