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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.changehandler.base.RecolorStatusBarAnimatorChangeHandler;
import com.hippo.ehviewer.widget.headerlayout.HeaderLayoutInterface;
import com.hippo.yorozuya.android.AnimationUtils;

/**
 * ChangeHandler between controllers with {@link HeaderLayoutInterface}.
 */
public class HeaderChangeHandler extends RecolorStatusBarAnimatorChangeHandler {

  private static final long DURATION_HEADER = 300L;
  private static final long DURATION_CONTENT = 400L;
  private static final long DELAY_HEADER = 300L;
  private static final long DELAY_CONTENT = 200L;

  @Nullable
  @Override
  protected Animator getAnimator3(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush, boolean toAddedToContainer) {
    if (!(from instanceof HeaderLayoutInterface) || !(to instanceof HeaderLayoutInterface)) {
      return null;
    }

    int distanceY = container.getHeight() / 4;

    View fromHeader = ((HeaderLayoutInterface) from).getHeader();
    Animator fromHeaderAnimator = ObjectAnimator.ofFloat(fromHeader, View.TRANSLATION_Y, 0, -fromHeader.getHeight());
    fromHeaderAnimator.setDuration(DURATION_HEADER);
    fromHeaderAnimator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);

    View fromContent = ((HeaderLayoutInterface) from).getContent();
    Animator fromContentAnimatorY = ObjectAnimator.ofFloat(fromContent, View.TRANSLATION_Y, 0, distanceY);
    Animator fromContentAnimatorAlpha = ObjectAnimator.ofFloat(fromContent, View.ALPHA, 1.0f, 0.0f);
    AnimatorSet fromContentAnimator = new AnimatorSet();
    fromContentAnimator.playTogether(fromContentAnimatorY, fromContentAnimatorAlpha);
    fromContentAnimator.setDuration(DURATION_CONTENT);
    fromContentAnimator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);

    View toHeader = ((HeaderLayoutInterface) to).getHeader();
    toHeader.setTranslationY(-toHeader.getHeight());
    Animator toHeaderAnimator = ObjectAnimator.ofFloat(toHeader, View.TRANSLATION_Y, -toHeader.getHeight(), 0);
    toHeaderAnimator.setDuration(DURATION_HEADER);
    toHeaderAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
    toHeaderAnimator.setStartDelay(DELAY_HEADER);

    View toContent = ((HeaderLayoutInterface) to).getContent();
    toContent.setTranslationY(distanceY);
    toContent.setAlpha(0.0f);
    Animator toContentAnimatorY = ObjectAnimator.ofFloat(toContent, View.TRANSLATION_Y, distanceY, 0);
    Animator toContentAnimatorAlpha = ObjectAnimator.ofFloat(toContent, View.ALPHA, 0.0f, 1.0f);
    AnimatorSet toContentAnimator = new AnimatorSet();
    toContentAnimator.playTogether(toContentAnimatorY, toContentAnimatorAlpha);
    toContentAnimator.setDuration(DURATION_CONTENT);
    toContentAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
    toContentAnimator.setStartDelay(DELAY_CONTENT);

    AnimatorSet set = new AnimatorSet();
    set.playTogether(fromHeaderAnimator, fromContentAnimator, toHeaderAnimator, toContentAnimator);
    return set;
  }

  @Override
  protected void resetFromView(@NonNull View from) {
    if (from instanceof HeaderLayoutInterface) {
      HeaderLayoutInterface headerLayout = (HeaderLayoutInterface) from;
      View header = headerLayout.getHeader();
      header.setTranslationY(0);
      header.setAlpha(1.0f);
      View content = headerLayout.getContent();
      content.setTranslationY(0);
      content.setAlpha(1.0f);
    }
  }
}
