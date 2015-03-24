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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.hippo.util.AssertUtils;

/**
 * {@link com.hippo.scene.Scene} is a {@code Activity} of {@link android.app.Activity}.
 * <p>
 * When start a new {@code Scene}, previous {@code Scene} can stay at screen for
 * a while. Or retain for a while before destroy.
 */
public abstract class Scene {

    private @Nullable FrameLayout mSceneView;

    private int mBackgroundColor = 0xffeeeeee; // TODO Need a better to set background color

    private static SceneManager sSceneManager;

    static void setSceneManager(SceneManager sceneManager) {
        sSceneManager = sceneManager;
    }

    // If there is no StageActivity for SceneManager, yout will get AssertError
    public @NonNull StageActivity getStageActivity() {
        StageActivity stageActivity = sSceneManager.getStageActivity();
        AssertUtils.assertNotNull("StageActivity is null", stageActivity);
        return stageActivity;
    }

    /**
     * You will get null before onCreate and after onDestroy mostly
     *
     * @return Null or nonull
     */
    public @Nullable View getSceneView() {
        return mSceneView;
    }

    public void startScene(@NonNull Class sceneClass) {
        sSceneManager.startScene(sceneClass, null, null);
    }

    public void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer) {
        sSceneManager.startScene(sceneClass, announcer, null);
    }

    public void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer,
            @Nullable Curtain curtain) {
        sSceneManager.startScene(sceneClass, announcer, curtain);
    }

    public final void finish() {
        sSceneManager.finishScene(this);
    }

    void create(Bundle savedInstanceState) {
        onCreate(savedInstanceState);

        if (mSceneView == null) {
            mSceneView = new FrameLayout(getStageActivity());
            initBackground(mSceneView);
        }

        // Make sure scene view is attach from stage
        attachToStage();
    }

    void resume() {
        onResume();
    }

    void pause() {
        onPause();
    }

    void destroy(boolean removeScene) {
        onDestroy();

        if (removeScene) {
            // Make sure scene view is detached from stage
            detachFromeStage();
        }
    }

    /**
     * It is called when scene is created
     *
     * @param savedInstanceState null for first time, non null for recrearte
     */
    protected void onCreate(Bundle savedInstanceState) {
    }

    public void setBackgroundColor(int bgColor) {
        if (mBackgroundColor != bgColor) {
            mBackgroundColor = bgColor;
            View sceneView = getSceneView();
            if (sceneView != null) {
                sceneView.setBackgroundColor(bgColor);
            }
        }
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }



    protected void setContentView(int resId) {
        AssertUtils.assertNull("Only call setContentView once", mSceneView);

        StageActivity sa = getStageActivity();
        mSceneView = new FrameLayout(sa);
        initBackground(mSceneView);
        mSceneView.setBackgroundColor(mBackgroundColor);
        sa.getLayoutInflater().inflate(resId, mSceneView);
    }

    protected void setContentView(View view) {
        AssertUtils.assertNull("Only call setContentView once", mSceneView);

        StageActivity sa = getStageActivity();
        mSceneView = new FrameLayout(sa);
        initBackground(mSceneView);
        mSceneView.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void initBackground(@NonNull View bg) {
        bg.setBackgroundColor(mBackgroundColor);
        bg.setClickable(true);
    }

    protected void onResume() {
    }

    protected void onPause() {
    }

    protected void onDestroy() {
    }


    /**
     * Finds a view that was identified by the id attribute from the XML that
     * was processed in {@link #onCreate}.
     *
     * @param resId the Id
     * @return The view if found or null otherwise.
     */
    public View findViewById(int resId) {
        View sceneView = getSceneView();
        if (sceneView != null) {
            return sceneView.findViewById(resId);
        } else {
            return null;
        }
    }


    /**
     * Is scene view attached to stage
     *
     * @return if scene view is null, always return false;
     */
    public boolean isInStage() {
        if (mSceneView == null) {
            return false;
        } else {
            ViewParent parent = mSceneView.getParent();

            if (parent == null) {
                return false;
            } else if (parent == getStageActivity().getStageLayout()) {
                return true;
            } else {
                throw new IllegalStateException("Scene view should only be the child of stage layout");
            }
        }
    }

    // Add scene view to stage layout
    void attachToStage() {
        if (mSceneView != null && !isInStage()) {
            doAttachToStage();
        }
    }

    /*
    void attachToStageAsPreScene() {
    }
    */

    // Remove scene view from stage layout
    void detachFromeStage() {
        if (mSceneView != null && isInStage()) {
            doDetachFromeStage();
        }
    }

    void doAttachToStage() {
        getStageActivity().attachSceneToStage(this);
    }

    /*
    void doAttachToStageAsPreScene() {
        getStageActivity().attachSceneToStageAsPreScene(this);
    }
    */

    void doDetachFromeStage() {
        getStageActivity().detachSceneFromStage(this);
    }

    public void onBackPressed() {
        finish();
    }

    // It is constant
    private String getStateKey() {
        return "scene:" + Integer.toHexString(hashCode());
    }

    void saveInstanceState(Bundle outState) {
        SparseArray<Parcelable> states = new SparseArray<>();
        onSaveInstanceState(states);
        outState.putSparseParcelableArray(getStateKey(), states);
    }

    void restoreInstanceState(@NonNull Bundle savedInstanceState) {
        if (mSceneView != null) {
            SparseArray<Parcelable> savedStates
                    = savedInstanceState.getSparseParcelableArray(getStateKey());
            if (savedStates != null) {
                onRestoreInstanceState(savedStates);
            }
        }
    }

    protected void onSaveInstanceState(@NonNull SparseArray<Parcelable> outState) {
        if (mSceneView != null) {
            mSceneView.saveHierarchyState(outState);
        }
    }

    protected void onRestoreInstanceState(@NonNull SparseArray<Parcelable> savedStates) {
        if (mSceneView != null) {
            mSceneView.restoreHierarchyState(savedStates);
        }
    }

    public void startActivityForResult(Intent intent, ActivityResultListener listener) {
        getStageActivity().startActivityForResult(intent, listener);
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with StageActivity.
     */
    public FragmentManager getSupportFragmentManager() {
        return getStageActivity().getSupportFragmentManager();
    }

    public interface ActivityResultListener {
        void onGetResult(int resultCode, Intent data);
    }
}
