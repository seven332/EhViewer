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

package com.hippo.scene;

import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.hippo.ehviewer.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SceneFragment extends Fragment {

    @IntDef({LAUNCH_MODE_STANDARD, LAUNCH_MODE_SINGLE_TOP})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LaunchMode {}

    public static final int LAUNCH_MODE_STANDARD = 0;
    public static final int LAUNCH_MODE_SINGLE_TOP = 1;

    @LaunchMode
    public int getLaunchMode() {
        return LAUNCH_MODE_STANDARD;
    }

    public int getSoftInputMode() {
        return 0; // WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED |
                  // WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED
    }

    public void onNewArguments(@NonNull Bundle args) {}

    public <T extends SceneFragment> void startScene(Class<T> clazz) {
        startScene(clazz, null, null);
    }

    public <T extends SceneFragment> void startScene(Class<T> clazz, Bundle args) {
        startScene(clazz, args, null);
    }

    public <T extends SceneFragment> void startScene(Class<T> clazz, TransitionHelper transitionHelper) {
        startScene(clazz, null, transitionHelper);
    }

    public <T extends SceneFragment> void startScene(Class<T> clazz, Bundle args, TransitionHelper transitionHelper) {
        FragmentActivity activity = getActivity();
        if (activity instanceof StageActivity) {
            ((StageActivity) activity).startScene(clazz, args, transitionHelper);
        }
    }

    public void finish() {
        finish(null);
    }

    public void finish(TransitionHelper transitionHelper) {
        FragmentActivity activity = getActivity();
        if (activity instanceof StageActivity) {
            ((StageActivity) activity).finishScene(this, transitionHelper);
        }
    }

    public void finishStage() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    /**
     * @return negative for error
     */
    public int getStackIndex() {
        FragmentActivity activity = getActivity();
        if (activity instanceof StageActivity) {
            return ((StageActivity) activity).getStackIndex(this);
        } else {
            return -1;
        }
    }

    public void onBackPressed() {
        finish();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.setTag(R.id.fragment_tag, getTag());
        view.setBackgroundColor(getContext().getResources().getColor(R.color.background_light));
    }
}
