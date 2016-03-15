/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.ui.scene;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.scene.SceneFragment;
import com.hippo.util.ActivityHelper;

public abstract class BaseScene extends SceneFragment {

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    private boolean mViewCreated;

    public void updateAvatar() {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).updateProfile();
        }
    }

    public void setDrawerLockMode(int lockMode, int edgeGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).setDrawerLockMode(lockMode, edgeGravity);
        }
    }

    public int getDrawerLockMode(int edgeGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            return ((MainActivity) activity).getDrawerLockMode(edgeGravity);
        } else {
            return DrawerLayout.LOCK_MODE_UNLOCKED;
        }
    }

    public void openDrawer(int drawerGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).openDrawer(drawerGravity);
        }
    }

    public void closeDrawer(int drawerGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).closeDrawer(drawerGravity);
        }
    }

    public void toggleDrawer(int drawerGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).toggleDrawer(drawerGravity);
        }
    }

    /**
     * @param resId 0 for clear
     */
    public void setNavCheckedItem(@IdRes int resId) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).setNavCheckedItem(resId);
        }
    }

    public void showTip(CharSequence message, int length) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).showTip(message, length);
        }
    }

    public void showTip(@StringRes int id, int length) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).showTip(id, length);
        }
    }

    public boolean needShowLeftDrawer() {
        return true;
    }

    public int getNavCheckedItem() {
        return 0;
    }

    public View onCreateDrawerView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    public void onDestroyDrawerView() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewCreated = true;

        // Update left drawer locked state
        if (needShowLeftDrawer()) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }

        // Update nav checked item
        setNavCheckedItem(getNavCheckedItem());

        // Hide soft ime
        ActivityHelper.hideSoftInput(getActivity());
    }

    @Override
    public void onDestroyView() {
        mViewCreated = false;
        super.onDestroyView();
    }

    public boolean isViewCreated() {
        return mViewCreated;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    }
}
