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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.hippo.util.Log;

import java.util.Stack;

class SceneManager {

    private static final String TAG = SceneManager.class.getSimpleName();

    private Stack<Pair<Scene, Curtain>> mSceneStack = new Stack<>();
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

    void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer,
            @Nullable Curtain curtain) {
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

        Scene previousState = getTopState();
        if (previousState != null) {
            previousState.pause();
        }

        mSceneStack.push(new Pair<>(scene, curtain));

        scene.create(null);
        scene.resume();

        // Do animation via curtain
        if (curtain != null && previousState != null) {
            curtain.open(scene, previousState);
        }
    }

    void finishScene(@NonNull Scene scene) {
        int index = getSceneIndex(scene);
        if (index >= 0 && index < mSceneStack.size()) {
            Pair<Scene, Curtain> pair = mSceneStack.remove(index);
            Curtain curtain = pair.second;
            Scene previousState = getTopState();

            // Do animation via curtain
            if (curtain != null) {
                scene.destroy(false);

                if (previousState != null) {
                    curtain.close(previousState, scene);
                }
            } else {
                scene.destroy(true);
            }
        } else {
            Log.e(TAG, "The scene is not in stage");
        }
    }

    private int getSceneIndex(@NonNull Scene scene) {
        int size  = mSceneStack.size();
        while (--size >= 0) {
            Pair<Scene, Curtain> pair = mSceneStack.get(size);
            if (scene.equals(pair.first)) {
                return size;
            }
        }
        return -1;
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
            return mSceneStack.peek().first;
        } else {
            return null;
        }
    }

    /*
    private Scene getSecondTopState() {
        int secondTopIndex = mSceneStack.size() - 2;
        if (secondTopIndex >= 0) {
            return mSceneStack.get(secondTopIndex).first;
        } else {
            return null;
        }
    }
    */

    protected void onSaveInstanceState(Bundle outState) {
        for (Pair<Scene, Curtain> p : mSceneStack) {
            Scene scene = p.first;
            scene.saveInstanceState(outState);
        }
    }

    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        for (Pair<Scene, Curtain> p : mSceneStack) {
            Scene scene = p.first;
            // Recreate
            scene.create(savedInstanceState);
            scene.resume();
            scene.restoreInstanceState(savedInstanceState);
        }
    }
}
