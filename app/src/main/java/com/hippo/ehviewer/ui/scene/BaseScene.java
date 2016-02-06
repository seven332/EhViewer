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
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.scene.SceneFragment;

public class BaseScene extends SceneFragment {

    private boolean mViewCreated;

    public void setDrawerLayoutEnable(boolean enable) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).setDrawerLayoutEnable(enable);
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

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewCreated = true;
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
