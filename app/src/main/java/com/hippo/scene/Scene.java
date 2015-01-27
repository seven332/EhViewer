/*
 * Copyright (C) 2014-2015 Hippo Seven
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

package com.hippo.scene;

import android.view.View;
import android.widget.FrameLayout;

import com.hippo.util.AssertUtils;

/**
 * {@link com.hippo.scene.Scene} is a {@code Activity} of {@link android.app.Activity}.
 * <p>
 * When start a new {@code Scene}, previous {@code Scene} can stay at screen for
 * a while. Or retain for a while before destroy.
 */
public abstract class Scene {

    private StageActivity mStageActivity;
    private FrameLayout mStageView;
    private View mRootView;

    void setStageActivity(StageActivity stageActivity) {
        mStageActivity = stageActivity;
        mStageView = stageActivity.getStageView();
    }

    public void setContentView(int layoutResID) {
        mRootView = mStageActivity.getLayoutInflater()
                .inflate(layoutResID, mStageView, false);
    }

    public void setContentView(View view) {
        mRootView = view;
    }

    public View findViewById(int id) {
        AssertUtils.assertNotNull("Call setContentView before findViewById",
                mRootView);

        return mRootView.findViewById(id);
    }

    View getRootView() {
        return mRootView;
    }

    public void startScene(Class sceneClass) {
        mStageActivity.startScene(this, sceneClass);
    }

    public final void finish() {
        mStageActivity.finishScene(this);
    }

    void create() {
        onCreate();

        AssertUtils.assertNotNull("You have to call setContentView in onCreate",
                mRootView);

        mStageActivity.addSceneView(this);
    }

    void resume() {
        onResume();

        mStageActivity.addSceneView(this);
    }

    void pause() {
        onPause();
    }

    void destroy() {
        onDestroy();
    }

    /**
     * You have to call setContentView in it.
     */
    public abstract void onCreate();

    public abstract void onResume();

    public abstract void onPause();

    public abstract void onDestroy();

    /**
     * This method should be called to remove when it need to remove
     */
    public final void dispatchRemove() {
        mStageActivity.stopSceneRetain(this);
    }

}
