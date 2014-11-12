/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.util;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

@SuppressLint("InlinedApi")
public final class FullScreenHelper implements View.OnSystemUiVisibilityChangeListener {

    private static final String TAG = FullScreenHelper.class.getSimpleName();

    private final int NOT_FULL_SCREEN_JELLY_BEAN =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

    private final int FULL_SCREEN_JELLY_BEAN =
            NOT_FULL_SCREEN_JELLY_BEAN |
            View.SYSTEM_UI_FLAG_LOW_PROFILE |
            View.SYSTEM_UI_FLAG_FULLSCREEN;

    private final int FULL_SCREEN_FLAG_JELLY_BEAN =
            View.SYSTEM_UI_FLAG_LOW_PROFILE |
            View.SYSTEM_UI_FLAG_FULLSCREEN;

    private final int NOT_FULL_SCREEN_KITKAT =
            NOT_FULL_SCREEN_JELLY_BEAN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    private final int FULL_SCREEN_KITKAT =
            NOT_FULL_SCREEN_KITKAT |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    private boolean mFullScreen;
    private final Activity mActivity;
    private final ActionBar mActionBar;
    private final Window mWindow;
    private final View mDecorView;

    private OnFullScreenBrokenListener mListener;

    public interface OnFullScreenBrokenListener {
        /**
         * FullScreen state should be fullScreen or not,
         * but user or system broke it
         *
         * @param fullScreen support to be
         */
        public void onFullScreenBroken(boolean fullScreen);
    }


    public FullScreenHelper(Activity activity) {
        mFullScreen = false;
        mActivity = activity;
        mActionBar = mActivity.getActionBar();
        mWindow = mActivity.getWindow();
        mDecorView = mWindow.getDecorView();
        mDecorView.setOnSystemUiVisibilityChangeListener(this);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            if ((mFullScreen && visibility != FULL_SCREEN_FLAG_JELLY_BEAN) ||
                    (!mFullScreen && visibility != 0)) {
                // User or system change visibility
                if (mListener != null)
                    mListener.onFullScreenBroken(mFullScreen);
            }
        }
    }

    public void setOnFullScreenBrokenListener(OnFullScreenBrokenListener l) {
        mListener = l;
    }

    public boolean getFullScreen() {
        return mFullScreen;
    }

    public void setFullScreen(boolean fullScreen) {
        mFullScreen = fullScreen;
        if (fullScreen) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                // Empty
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                mActionBar.hide();
                mWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                mActionBar.hide();
                mDecorView.setSystemUiVisibility(FULL_SCREEN_JELLY_BEAN);

            } else {
                mActionBar.hide();
                mDecorView.setSystemUiVisibility(FULL_SCREEN_KITKAT);
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                // Empty
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                mActionBar.show();
                mWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                mActionBar.show();
                mDecorView.setSystemUiVisibility(NOT_FULL_SCREEN_JELLY_BEAN);

            } else {
                mActionBar.show();
                mDecorView.setSystemUiVisibility(NOT_FULL_SCREEN_KITKAT);
            }
        }
    }
}
