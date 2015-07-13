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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;

import com.hippo.app.StatsActivity;
import com.hippo.yorozuya.IdGenerator;
import com.hippo.yorozuya.Say;

public abstract class StageActivity extends StatsActivity {

    private static final String TAG = StageActivity.class.getSimpleName();

    private static final String KEY_SCENEN_MANAGER_ID = "scene_manager_id";

    private IdGenerator mActivityResultIdGenerator = new IdGenerator();
    private SparseArray<Scene.ActivityResultListener> mActivityResultListenerMap =
            new SparseArray<>();

    private SceneManager mSceneManager;

    private int mFitPaddingBottom;

    public abstract @NonNull StageLayout getStageLayout();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // The first
            mSceneManager = SceneApplication.createSceneManager(this);
        } else {
            int id = savedInstanceState.getInt(KEY_SCENEN_MANAGER_ID, -1);
            if (id == -1) {
                Say.e(TAG, "Can't get KEY_SCENEN_MANAGER_ID");
                mSceneManager = SceneApplication.createSceneManager(this);
            } else {
                mSceneManager = SceneApplication.getSceneManager(this, id);
            }
        }

        mSceneManager.setStageActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSceneManager.getSceneCount() == 0) {
            // Remove this SceneManager
            SceneApplication.removeSceneManager(this, mSceneManager);
        }

        mSceneManager.setStageActivity(null);
        mSceneManager = null;
    }

    public int getFitPaddingBottom() {
        return mFitPaddingBottom;
    }

    public void setFitPaddingBottom(int b) {
        if (mFitPaddingBottom != b) {
            mFitPaddingBottom = b;
            for (Scene scene : mSceneManager.getSceneStack()) {
                scene.setFitPaddingBottom(b);
            }
        }
    }

    void attachSceneToStage(Scene scene) {
        View sceneView = scene.getSceneView();
        getStageLayout().addView(sceneView);
    }

    void detachSceneFromStage(Scene scene) {
        View sceneView = scene.getSceneView();
        getStageLayout().removeView(sceneView);
    }

    public void startScene(@NonNull Class sceneClass) {
        mSceneManager.startScene(sceneClass, null, null);
    }

    public void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer) {
        mSceneManager.startScene(sceneClass, announcer, null);
    }

    public void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer,
            @Nullable Curtain curtain) {
        mSceneManager.startScene(sceneClass, announcer, curtain);
    }

    /**
     * Start new Activity for result to listener
     *
     * @param intent the intent for new Activity
     * @param listener callback
     * @return request code
     */
    public int startActivityForResult(Intent intent, Scene.ActivityResultListener listener) {
        int id = mActivityResultIdGenerator.nextId();
        mActivityResultListenerMap.put(id, listener);
        startActivityForResult(intent, id);
        return id;
    }

    public void addSceneStateListener(SceneManager.SceneStateListener listener) {
        mSceneManager.addSceneStateListener(listener);
    }

    public void removeSceneStateListener(SceneManager.SceneStateListener listener) {
        mSceneManager.removeSceneStateListener(listener);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only work when there is no scene in the stage
     * </p>
     */
    @Override
    public void finish() {
        if (mSceneManager.getSceneCount() == 0) {
            super.finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Scene.ActivityResultListener listener = mActivityResultListenerMap.get(requestCode);
        if (listener != null) {
            listener.onGetResult(resultCode, data);
        }
        mActivityResultListenerMap.delete(requestCode);
    }

    @Override
    public void onBackPressed() {
        if (!mSceneManager.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mSceneManager.onSaveInstanceState(outState);

        outState.putInt(KEY_SCENEN_MANAGER_ID, mSceneManager.getId());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSceneManager.onRestoreInstanceState(savedInstanceState);
    }
}
