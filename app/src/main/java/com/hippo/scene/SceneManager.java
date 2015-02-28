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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene;

import android.os.Bundle;

import com.hippo.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

class SceneManager {

    private static final String TAG = SceneManager.class.getSimpleName();

    private Stack<Scene> mSceneStack = new Stack<>();
    private StageActivity mStageActivity;

    // Should only be called by SceneApplication
    SceneManager() {
    }

    void setStageActivity(StageActivity stageActivity) {
        mStageActivity = stageActivity;
    }

    StageActivity getStageActivity() {
        return mStageActivity;
    }

    void startScene(@NotNull Class sceneClass, @Nullable Announcer announcer) {
        Scene scene;
        try {
            scene = (Scene) sceneClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Can't instance " + sceneClass.getName());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("The constructor of " +
                    sceneClass.getName() + " is not visible");
        } catch (ClassCastException e) {
            throw new IllegalStateException(sceneClass.getName() + " can not cast to scene");
        }

        Scene topScene = getTopState();
        if (topScene != null) {
            topScene.pause();
        }

        mSceneStack.push(scene);

        scene.create(null);
        scene.resume();
    }

    void finishScene(@NotNull Scene scene) {
        if (mSceneStack.remove(scene)) {
            scene.destroy();
        } else {
            Log.e(TAG, "The scene is not in stage");
        }
    }

    boolean onBackPressed() {
        Scene scene = getTopState();
        if (scene != null) {
            scene.onBackPressed();
            return true;
        } else {
            Log.w(TAG, "There is no scene in the stage");
            return false;
        }
    }

    private Scene getTopState() {
        if (!mSceneStack.isEmpty()) {
            return mSceneStack.peek();
        } else {
            return null;
        }
    }

    private Scene getSecondTopState() {
        int secondTopIndex = mSceneStack.size() - 2;
        if (secondTopIndex >= 0) {
            return mSceneStack.get(secondTopIndex);
        } else {
            return null;
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        for (Scene scene : mSceneStack) {
            scene.saveInstanceState(outState);
        }
    }

    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        for (Scene scene : mSceneStack) {
            // Recreate
            scene.create(savedInstanceState);
            scene.resume();
            scene.restoreInstanceState(savedInstanceState);
        }
    }
}
