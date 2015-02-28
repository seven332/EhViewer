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
import android.util.SparseArray;

import com.hippo.ehviewer.ui.AbsActionBarActivity;
import com.hippo.util.IntIdGenerator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class StageActivity extends AbsActionBarActivity {

    private IntIdGenerator mActivityResultIdGenerator = IntIdGenerator.create();
    private SparseArray<Scene.ActivityResultListener> mActivityResultListenerMap =
            new SparseArray<>();

    private static SceneManager sSceneManager;

    static void setSceneManager(SceneManager sceneManager) {
        sSceneManager = sceneManager;
    }

    public abstract StageLayout getStageLayout();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((SceneApplication) getApplication()).getSceneManager().setStageActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ((SceneApplication) getApplication()).getSceneManager().setStageActivity(null);
    }

    void attachSceneToStage(Scene scene) {
        getStageLayout().addView(scene.getSceneView());
    }

    void attachSceneToStageAsPreScene(Scene scene) {
        StageLayout stageLayout = getStageLayout();
        stageLayout.addView(scene.getSceneView(), stageLayout.getChildCount() - 1);
    }

    void detachSceneFromStage(Scene scene) {
        getStageLayout().removeView(scene.getSceneView());
    }

    public void startScene(@NotNull Class sceneClass, @Nullable Announcer announcer) {
        sSceneManager.startScene(sceneClass, announcer);
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
        if (!sSceneManager.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        sSceneManager.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        sSceneManager.onRestoreInstanceState(savedInstanceState);
    }
}
