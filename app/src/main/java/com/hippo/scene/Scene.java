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
import android.view.ViewTreeObserver;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;

import com.hippo.util.AssertUtils;
import com.hippo.util.ViewUtils;

/**
 * {@link com.hippo.scene.Scene} is a {@code Activity} of {@link android.app.Activity}.
 * <p>
 * When start a new {@code Scene}, previous {@code Scene} can stay at screen for
 * a while. Or retain for a while before destroy.
 */
public abstract class Scene {

    static final int SCENE_STATE_CREATE = 0;
    static final int SCENE_STATE_RUN = 1;
    static final int SCENE_STATE_DESTROY = 2;
    static final int SCENE_STATE_OPEN = 3;
    static final int SCENE_STATE_CLOSE = 4;
    static final int SCENE_STATE_PAUSE = 5;

    private @Nullable Curtain mCurtain;

    private @Nullable AbsoluteLayout mSceneView;

    private int mBackgroundColor = 0xffeeeeee; // TODO Need a better to set background color

    private int mState;

    private static SceneManager sSceneManager;

    private int mFitPaddingBottom = -1;

    static void setSceneManager(SceneManager sceneManager) {
        sSceneManager = sceneManager;
    }

    void setCurtain(@Nullable Curtain curtain) {
        mCurtain = curtain;
    }

    @Nullable Curtain getCurtain() {
        return mCurtain;
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
    public @Nullable ViewGroup getSceneView() {
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

    int getState() {
        return mState;
    }

    void setState(int state) {
        mState = state;
    }

    public final void finish() {
        sSceneManager.finishScene(this);
    }

    void create(@Nullable Bundle savedInstanceState) {
        onCreate(savedInstanceState);

        if (mSceneView == null) {
            mSceneView = new AbsoluteLayout(getStageActivity());
            initBackground(mSceneView);
        }

        // Make sure scene view is attach from stage
        attachToStage();

        // Hide it if it is hidden
        if (savedInstanceState != null && savedInstanceState.getBoolean(getIsGoneKey(), false)) {
            mSceneView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ViewUtils.removeOnGlobalLayoutListener(mSceneView.getViewTreeObserver(), this);
                    ViewUtils.setVisibility(mSceneView, View.GONE);
                }
            });
        }
    }

    void destroy() {
        onDestroy();
    }

    void open() {
        onOpen();
    }

    void close() {
        onClose();
    }

    void pause() {
        onPause();
    }

    void resume() {
        onResume();
    }

    void out() {
        onOut();
    }

    void getFitPaddingBottom(int b) {
        if (mFitPaddingBottom != b) {
            mFitPaddingBottom = b;
            onGetFitPaddingBottom(b);
        }
    }

    /**
     * return true if it is close or about to close
     */
    public boolean isFinishing() {
        return mState == SCENE_STATE_CLOSE || mState == SCENE_STATE_DESTROY;
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
        StageActivity sa = getStageActivity();
        mSceneView = new AbsoluteLayout(sa);
        initBackground(mSceneView);
        mSceneView.setBackgroundColor(mBackgroundColor);
        sa.getLayoutInflater().inflate(resId, mSceneView);
    }

    protected void setContentView(View view) {
        StageActivity sa = getStageActivity();
        mSceneView = new AbsoluteLayout(sa);
        initBackground(mSceneView);
        mSceneView.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void initBackground(@NonNull View bg) {
        bg.setBackgroundColor(mBackgroundColor);
        bg.setClickable(true);
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
    }

    protected void onDestroy() {
    }

    protected void onOpen() {
    }

    protected void onClose() {
    }

    protected void onPause() {
    }

    protected void onResume() {
    }

    protected void onOut() {
    }

    protected void onGetFitPaddingBottom(int b) {
    }


    public View onCreateStageBar() {
        return null;
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

    // Remove scene view from stage layout
    void detachFromeStage() {
        if (mSceneView != null && isInStage()) {
            doDetachFromeStage();
        }
    }

    void doAttachToStage() {
        getStageActivity().attachSceneToStage(this);
    }

    void doDetachFromeStage() {
        getStageActivity().detachSceneFromStage(this);
    }

    public void onBackPressed() {
        if (!endCurtainAnimation() && !sSceneManager.endLegacyScene()) {
            sSceneManager.finishScene(this);
        }
    }

    /**
     * End curtainAnimation if it is running
     *
     * @return True if curtain animation is running
     */
    boolean endCurtainAnimation() {
        if (mCurtain != null && mCurtain.isInAnimation()) {
            mCurtain.endAnimation();
            return true;
        }
        return false;
    }

    void openFinished() {
        open();
        setState(Scene.SCENE_STATE_RUN);
    }

    void closeFinished() {
        sSceneManager.removeLegacyScene(this);

        close();
        setState(Scene.SCENE_STATE_DESTROY);
        detachFromeStage();
    }

    // It is constant
    private String getStateKey() {
        return "scene:" + Integer.toHexString(hashCode());
    }

    private String getIsGoneKey() {
        return getStateKey() + ":gone";
    }

    void saveInstanceState(Bundle outState) {
        SparseArray<Parcelable> states = new SparseArray<>();
        onSaveInstanceState(states);
        outState.putSparseParcelableArray(getStateKey(), states);

        if (mSceneView != null) {
            outState.putBoolean(getIsGoneKey(), mSceneView.getVisibility() == View.GONE);
        }
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
