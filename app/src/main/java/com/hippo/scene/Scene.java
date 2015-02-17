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

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
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

    public StageActivity getStageActivity() {
        return mStageActivity;
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

    public void onBackPressed() {
        finish();
    }

    protected void onSaveInstanceState(Bundle outState) {
        SparseArray<Parcelable> states = new SparseArray<>();
        mRootView.saveHierarchyState(states);
        outState.putSparseParcelableArray(Integer.toHexString(hashCode()), states);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        SparseArray<Parcelable> savedStates
                = savedInstanceState.getSparseParcelableArray(Integer.toHexString(hashCode()));
        if (savedStates != null) {
            mRootView.restoreHierarchyState(savedStates);
        }
    }

    public void startActivityForResult(Intent intent, ActivityResultListener listener) {
        mStageActivity.startActivityForResult(intent, listener);
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with StageActivity.
     */
    public FragmentManager getSupportFragmentManager() {
        return mStageActivity.getSupportFragmentManager();
    }

    public interface ActivityResultListener {
        public void onGetResult(int resultCode, Intent data);
    }
}
