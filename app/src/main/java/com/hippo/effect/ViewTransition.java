/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.effect;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.ViewUtils;

public class ViewTransition {

    private static long ANIMATE_TIME = 300L;

    private View mView1;
    private View mView2;
    private boolean mSwitch = false;

    public ViewTransition(@NonNull View view1, @NonNull View view2) {
        mView1 = view1;
        mView2 = view2;

        ViewUtils.setVisibility(view1, View.VISIBLE);
        ViewUtils.setVisibility(view2, View.GONE);
    }

    public void toggle() {
        if (mSwitch) {
            showFirstView();
        } else {
            showSecondView();
        }
    }

    public void showFirstView() {
        if (mSwitch) {
            mSwitch = false;

            ObjectAnimator oa1 = ObjectAnimator.ofFloat(mView1, "alpha", 0f, 1f);
            oa1.setDuration(ANIMATE_TIME);
            oa1.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ViewUtils.setVisibility(mView1, View.VISIBLE);
                }
            });
            ObjectAnimator oa2 = ObjectAnimator.ofFloat(mView2, "alpha", 1f, 0f);
            oa2.setDuration(ANIMATE_TIME);
            oa2.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ViewUtils.setVisibility(mView2, View.GONE);
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                oa1.setAutoCancel(true);
                oa2.setAutoCancel(true);
            }
            oa1.start();
            oa2.start();
        }
    }

    public void showSecondView() {
        if (!mSwitch) {
            mSwitch = true;

            ObjectAnimator oa1 = ObjectAnimator.ofFloat(mView1, "alpha", 1f, 0f);
            oa1.setDuration(ANIMATE_TIME);
            oa1.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ViewUtils.setVisibility(mView1, View.GONE);
                }
            });
            ObjectAnimator oa2 = ObjectAnimator.ofFloat(mView2, "alpha", 0f, 1f);
            oa2.setDuration(ANIMATE_TIME);
            oa2.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ViewUtils.setVisibility(mView2, View.VISIBLE);
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                oa1.setAutoCancel(true);
                oa2.setAutoCancel(true);
            }
            oa1.start();
            oa2.start();
        }
    }
}
