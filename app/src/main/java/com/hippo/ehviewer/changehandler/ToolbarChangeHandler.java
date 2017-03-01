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
 * Created by Hippo on 2/25/2017.
 */

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.hippo.ehviewer.changehandler.base.RecolorStatusBarAnimatorChangeHandler;
import com.hippo.ehviewer.widget.ToolbarLayout;
import com.hippo.ehviewer.widget.ToolbarUtils;
import java.util.LinkedList;
import java.util.List;

public class ToolbarChangeHandler extends RecolorStatusBarAnimatorChangeHandler {

  private static final String LOG_TAG = ToolbarChangeHandler.class.getSimpleName();

  private static final int CONTENT_DURATION = 300;
  private static final int CONTENT_DELAY = 100;
  private static final int MENU_DURATION = 150;
  private static final int TOTAL_DURATION = CONTENT_DELAY + CONTENT_DURATION;

  @Nullable
  @Override
  protected Animator getAnimator3(@NonNull ViewGroup container, @Nullable View from,
      @Nullable View to, boolean isPush, boolean toAddedToContainer) {
    if (!(from instanceof ToolbarLayout) || !(to instanceof ToolbarLayout)) {
      if (from != null && to != null) {
        Log.e(LOG_TAG, "ToolbarChangeHandler only works for ToolbarController, "
            + "and ToolbarController's content view must be ToolbarLayout.");
      }
      return null;
    }

    ToolbarLayout fromView = (ToolbarLayout) from;
    ToolbarLayout toView = (ToolbarLayout) to;

    // Hide from header
    fromView.getHeader().setVisibility(View.INVISIBLE);

    setRecolorStatusBarAnimationDuration(TOTAL_DURATION);

    List<Animator> animators = new LinkedList<>();
    addContentAnimator(animators, fromView, toView);
    addDrawerArrowDrawableAnimator(animators, fromView, toView);
    addTitleAnimator(animators, fromView, toView, isPush);
    addMenuAnimator(animators, fromView, toView, isPush);
    if (animators.isEmpty()) {
      return null;
    } else if (animators.size() == 1) {
      return animators.get(0);
    } else {
      AnimatorSet set = new AnimatorSet();
      set.playTogether(animators);
      return set;
    }
  }

  private static void addContentAnimator(List<Animator> list,
      ToolbarLayout from, ToolbarLayout to) {
    View fromContent = from.getContent();
    View toContent = to.getContent();
    int distance = from.getHeight() / 4;
    Animator animator = ChangeHandlerAnimators.exchangeContent(
        fromContent, toContent, distance, CONTENT_DURATION, CONTENT_DELAY);
    if (animator != null) {
      list.add(animator);
    }
  }

  private static void addDrawerArrowDrawableAnimator(List<Animator> list,
      ToolbarLayout from, ToolbarLayout to) {
    DrawerArrowDrawable fromDrawable = from.getDrawerArrowDrawable();
    DrawerArrowDrawable toDrawable = to.getDrawerArrowDrawable();
    Animator animator = ChangeHandlerAnimators.changeDrawerArrowDrawable(fromDrawable, toDrawable);
    if (animator != null) {
      animator.setDuration(TOTAL_DURATION);
      list.add(animator);
    }
  }

  private static void addTitleAnimator(List<Animator> list,
      ToolbarLayout from, ToolbarLayout to, boolean isPush) {
    Toolbar fromHeader = from.getActualHeader();
    Toolbar toHeader = to.getActualHeader();
    if (fromHeader == null || toHeader == null) {
      return;
    }

    TextView fromTitle = ToolbarUtils.getTitleView(fromHeader);
    TextView toTitle = ToolbarUtils.getTitleView(toHeader);
    if (fromTitle == null || toTitle == null) {
      return;
    }

    Animator animator = ChangeHandlerAnimators.crossFade(fromTitle, toTitle, false, isPush);
    if (animator != null) {
      animator.setDuration(TOTAL_DURATION);
      list.add(animator);
    }
  }

  private static void addMenuAnimator(List<Animator> list,
      ToolbarLayout from, ToolbarLayout to, boolean isPush) {
    Toolbar fromHeader = from.getActualHeader();
    Toolbar toHeader = to.getActualHeader();
    if (fromHeader == null || toHeader == null) {
      return;
    }

    ActionMenuView fromMenu = ToolbarUtils.getActionMenuView(fromHeader);
    ActionMenuView toMenu = ToolbarUtils.getActionMenuView(toHeader);

    Animator animator = ChangeHandlerAnimators.changeActionMenu(
        fromMenu, toMenu, MENU_DURATION, isPush);
    if (animator != null) {
      list.add(animator);
    }
  }

  @Override
  protected void resetFromView(@NonNull View from) {
    super.resetFromView(from);

    if (from instanceof ToolbarLayout) {
      ToolbarLayout fromView = (ToolbarLayout) from;
      View fromContent = fromView.getContent();
      if (fromContent != null) {
        fromContent.setTranslationY(0.0f);
        fromContent.setAlpha(1.0f);
      }
      View header = fromView.getHeader();
      if (header != null) {
        header.setVisibility(View.VISIBLE);
      }
    }
  }
}
