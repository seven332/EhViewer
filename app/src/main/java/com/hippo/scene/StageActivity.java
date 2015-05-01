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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;

import com.hippo.util.IntIdGenerator;

public abstract class StageActivity extends AppCompatActivity {

    private IntIdGenerator mActivityResultIdGenerator = IntIdGenerator.create();
    private SparseArray<Scene.ActivityResultListener> mActivityResultListenerMap =
            new SparseArray<>();

    private static SceneManager sSceneManager;

    private int mFitPaddingBottom;

    static void setSceneManager(SceneManager sceneManager) {
        sSceneManager = sceneManager;
    }

    public abstract @NonNull StageLayout getStageLayout();

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

    public int getFitPaddingBottom() {
        return mFitPaddingBottom;
    }

    public void setFitPaddingBottom(int b) {
        if (mFitPaddingBottom != b) {
            mFitPaddingBottom = b;
            for (Scene scene : sSceneManager.getSceneStack()) {
                scene.setFitPaddingBottom(b);
            }
        }
    }

    void attachSceneToStage(Scene scene) {
        View sceneView = scene.getSceneView();
        if (sceneView != null) {
            getStageLayout().addView(sceneView);
        }
    }

    void detachSceneFromStage(Scene scene) {
        View sceneView = scene.getSceneView();
        if (sceneView != null) {
            getStageLayout().removeView(sceneView);
        }
    }

    public void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer) {
        sSceneManager.startScene(sceneClass, announcer, null);
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
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        sSceneManager.onRestoreInstanceState(savedInstanceState);
    }
}
