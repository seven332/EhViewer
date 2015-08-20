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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.hippo.yorozuya.AssertUtils;

/**
 * {@link com.hippo.scene.Scene} is a {@code Activity} of {@link android.app.Activity}.
 * <p>
 * When start a new {@code Scene}, previous {@code Scene} can stay at screen for
 * a while. Or retain for a while before destroy.
 */
public abstract class Scene {

    public static final int LAUNCH_MODE_STANDARD = 0;
    public static final int LAUNCH_MODE_SINGLE_TOP = 1;

    static final int SCENE_STATE_INIT = 0;
    static final int SCENE_STATE_PREPARE = 1;
    static final int SCENE_STATE_OPEN = 2;
    static final int SCENE_STATE_RUN = 3;
    static final int SCENE_STATE_CLOSE = 4;
    static final int SCENE_STATE_DESTROY = 5;
    static final int SCENE_STATE_DIE = 6;
    static final int SCENE_STATE_PAUSE = 7;
    static final int SCENE_STATE_REBIRTH = 8;

    SceneManager mSceneManager;

    private @Nullable Curtain mCurtain;

    private @Nullable Announcer mAnnouncer;

    private SceneView mSceneView;

    private int mBackgroundColor = 0xffeeeeee; // TODO Need a better to set background color

    private int mState = SCENE_STATE_INIT;

    private int mId;

    private boolean mHide = false;

    public void setSceneManager(SceneManager sceneManager) {
        mSceneManager = sceneManager;
        mId = sceneManager.nextId();
    }

    int getId() {
        return mId;
    }

    void setCurtain(@Nullable Curtain curtain) {
        mCurtain = curtain;
    }

    void setAnnouncer(@Nullable Announcer announcer) {
        mAnnouncer = announcer;
    }

    @Nullable Curtain getCurtain() {
        return mCurtain;
    }

    public @Nullable Announcer getAnnouncer() {
        return mAnnouncer;
    }

    // If there is no StageActivity for SceneManager, yout will get AssertError
    public @NonNull StageActivity getStageActivity() {
        StageActivity stageActivity = mSceneManager.getStageActivity();
        AssertUtils.assertNotNull("StageActivity is null", stageActivity);
        return stageActivity;
    }

    /**
     * Aliases of {@link #getStageActivity()}
     */
    public Context getContext() {
        return getStageActivity();
    }

    public int getLaunchMode() {
        return LAUNCH_MODE_STANDARD;
    }

    private void checkSceneView() {
        AssertUtils.assertNotNull("Must call it after onCreate and before onDestroy", mSceneView);
    }

    /**
     * Must call it after onCreate and before onDestroy
     */
    public @NonNull SceneView getSceneView() {
        checkSceneView();
        return mSceneView;
    }

    public int getSceneCount() {
        return mSceneManager.getSceneCount();
    }

    public void startScene(@NonNull Class sceneClass) {
        startScene(sceneClass, null);
    }

    public void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer) {
        startScene(sceneClass, announcer, getDefaultCurtain());
    }

    static Curtain getDefaultCurtain() {
        return new OffsetCurtain(OffsetCurtain.DIRECTION_BOTTOM);
    }

    public void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer,
            @Nullable Curtain curtain) {
        mSceneManager.startScene(sceneClass, announcer, curtain);
    }

    int getState() {
        return mState;
    }

    void setState(int state) {
        mState = state;
    }

    SceneView createSceneView(Context context) {
        return new SceneView(context);
    }

    public final void finish() {
        mSceneManager.finishScene(this, false);
    }

    /**
     * Tell {@link SceneManager} how to {@link View#setVisibility(int)} when it is not the first scene.
     * If it covers previous scene completely, {@link SceneManager} will setVisibility(View.INVISIBLE),
     * otherwise setVisibility(View.VISIBLE)
     *
     * @return {@code true} if it covers previous scene completely
     */
    protected boolean coverCompletely() {
        return true;
    }

    void setHide(boolean hide) {
        checkSceneView();
        if (mHide != hide) {
            mHide = hide;
            if (hide) {
                mSceneView.setVisibility(View.INVISIBLE);
            } else {
                mSceneView.setVisibility(View.VISIBLE);
            }
        }
    }

    boolean isHide() {
        return mHide;
    }

    void init() {
        onInit();
    }

    void rebirth() {
        if (mCurtain != null) {
            mCurtain.onRebirth();
        }

        onRebirth();
    }

    void create(boolean rebirth) {
        onCreate(rebirth);

        if (mSceneView == null) {
            // Not call setContentView in onCreate, create scene view by ourself
            mSceneView = createSceneView(getStageActivity());
        }

        mSceneView.setBackgroundColor(mBackgroundColor);

        mSceneView.setFocusable(true);
        mSceneView.setFocusableInTouchMode(true);
        mSceneView.requestFocus();

        // Make sure scene view is attach from stage
        attachToStage();

        // Hide it if it is hidden
        if (mHide) {
            mSceneView.setVisibility(View.INVISIBLE);
        } else {
            mSceneView.setVisibility(View.VISIBLE);
        }

        if (!rebirth) {
            // Make sure soft key broad is hidden
            InputMethodManager imm = (InputMethodManager) getStageActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getSceneView().getWindowToken(), 0);
        }
    }

    void bind() {
        onBind();
    }

    void restore() {
        onRestore();
    }

    void destroy(boolean die) {
        onDestroy(die);
    }

    void die(boolean keepView) {
        onDie();

        // Make sure soft key broad is hidden
        InputMethodManager imm = (InputMethodManager) getStageActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getSceneView().getWindowToken(), 0);

        if (!keepView) {
            detachFromeStage();
        }

        SceneApplication.getRefWatcher(getStageActivity()).watch(this);
    }

    void pause() {
        getSceneView().setEnableTouch(false);

        onPause();
    }

    void resume() {
        getSceneView().setEnableTouch(true);

        mSceneView.requestFocus();

        onResume();
    }

    void open() {
        onOpen();
    }

    void close() {
        onClose();
    }

    protected void onInit() {

    }

    protected void onRebirth() {

    }

    protected void onCreate(boolean rebirth) {

    }

    protected void onBind() {

    }

    protected void onRestore() {

    }

    protected void onDestroy(boolean die) {

    }

    protected void onDie() {

    }

    protected void onPause() {

    }

    protected void onResume() {

    }

    protected void onOpen() {

    }

    protected void onClose() {

    }

    void setFitPaddingBottom(int b) {
        onGetFitPaddingBottom(b);
    }

    /**
     * @return true if the scene is dead or will be
     */
    public boolean canNotBeSaved() {
        return mState == SCENE_STATE_CLOSE || mState == SCENE_STATE_DESTROY || mState == SCENE_STATE_DIE;
    }

    /**
     * @return true if the scene is dead
     */
    public boolean isDead() {
        return mState == SCENE_STATE_DIE;
    }

    public boolean isRunning() {
        return mState == SCENE_STATE_RUN;
    }

    public void setBackgroundColor(int bgColor) {
        if (mBackgroundColor != bgColor) {
            mBackgroundColor = bgColor;
            View sceneView = mSceneView;
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
        mSceneView = createSceneView(sa);
        mSceneView.setBackgroundColor(mBackgroundColor);
        sa.getLayoutInflater().inflate(resId, mSceneView);
    }

    protected void setContentView(View view) {
        StageActivity sa = getStageActivity();
        mSceneView = createSceneView(sa);
        mSceneView.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    protected void onNewAnnouncer(Announcer announcer) {
    }

    /**
     * It will be called if nav bar is transparent.
     * @param b the nav bar's height
     */
    protected void onGetFitPaddingBottom(int b) {
    }

    /**
     * Finds a view that was identified by the id attribute from the XML that
     * was processed in {@link #onCreate}.
     *
     * @param resId the Id
     * @return The view if found or null otherwise.
     */
    public View findViewById(int resId) {
        View sceneView = mSceneView;
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

    public StageLayout getStageLayout() {
        return getStageActivity().getStageLayout();
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

    void onBackPressedInternal() {
        if (!endCurtainAnimation() && !mSceneManager.endLegacyScene()) {
            onBackPressed();
        }
    }

    /**
     * Not all back press will call it
     */
    public void onBackPressed() {
        finish();
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
        mSceneManager.openScene(this);
    }

    void closeFinished() {
        mSceneManager.closeScene(this);
    }

    // It is constant
    private String getStateKey() {
        return "scene:" + mId;
    }

    private String getPersonalStateKey() {
        return "scene:personal:" + mId;
    }

    void saveInstanceState(Bundle outState) {
        SparseArray<Parcelable> states = new SparseArray<>();
        onSaveInstanceState(states);
        outState.putSparseParcelableArray(getStateKey(), states);
        Parcelable personalState = onSavePersonalState();
        if (personalState != null) {
            outState.putParcelable(getPersonalStateKey(), personalState);
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
        Parcelable personalState = savedInstanceState.getParcelable(getPersonalStateKey());
        if (personalState != null) {
            onRestorePersonalState(personalState);
        }
    }

    protected Parcelable onSavePersonalState() {
        return null;
    }

    protected void onRestorePersonalState(@NonNull Parcelable saved) {
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

    public interface ActivityResultListener {
        void onGetResult(int resultCode, Intent data);
    }
}
