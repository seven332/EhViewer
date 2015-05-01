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

import com.hippo.util.Log;

import java.util.Stack;

class SceneManager {

    private static final String TAG = SceneManager.class.getSimpleName();

    private Stack<Scene> mSceneStack = new Stack<>();
    private StageActivity mStageActivity;

    private Scene mLegacyScene;

    // Should only be called by SceneApplication
    SceneManager() {
    }

    void setStageActivity(StageActivity stageActivity) {
        mStageActivity = stageActivity;
    }

    StageActivity getStageActivity() {
        return mStageActivity;
    }

    private boolean isStageAlive() {
        return mStageActivity != null;
    }

    // TODO check previousState state
    void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer,
            @Nullable Curtain curtain) {
        if (!isStageAlive()) {
            Log.w(TAG, "Stage is not alive, but attemp to create " + sceneClass.getSimpleName());
            return;
        }

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
        scene.setAnnouncer(announcer);
        scene.setCurtain(curtain);

        startScene(scene, curtain);
    }

    void showDialog(@NonNull SceneDialog dialog, @Nullable Curtain curtain) {
        if (!isStageAlive()) {
            Log.w(TAG, "Stage is not alive, but attemp to show dialog " + dialog.toString());
            return;
        }

        dialog.setCurtain(curtain);

        startScene(dialog, curtain);
    }

    private void startScene(Scene scene, Curtain curtain) {
        Scene previousState = getTopState();
        mSceneStack.push(scene);

        if (curtain != null) {
            curtain.setPreviousScene(previousState);
        }

        if (previousState != null) {
            previousState.pause();
            scene.setState(Scene.SCENE_STATE_PAUSE);
        }

        scene.setState(Scene.SCENE_STATE_CREATE);
        scene.create(null);

        // Update fit padding
        int fitPaddingBottom = getStageActivity().getFitPaddingBottom();
        if (fitPaddingBottom > 0) {
            scene.setFitPaddingBottom(fitPaddingBottom);
        }

        if (curtain != null && previousState != null) {
            scene.setState(Scene.SCENE_STATE_OPEN);
            curtain.open(scene, previousState);
            // Set state run by curtain
        } else {
            scene.setState(Scene.SCENE_STATE_RUN);
        }
    }

    /**
     * End legacy scene close animation at once
     *
     * @return True if legacy scene exist and its curtain animation is running
     */
    boolean endLegacyScene() {
        if (mLegacyScene != null) {
            boolean result = mLegacyScene.endCurtainAnimation();
            mLegacyScene = null;
            return result;
        }
        return false;
    }

    void finishScene(@NonNull Scene scene) {
        int index = getSceneIndex(scene);
        if (index >= 0 && index < mSceneStack.size()) {

            endLegacyScene();

            // Scene state might be SCENE_STATE_RUN, SCENE_STATE_OPEN, SCENE_STATE_PAUSE
            if (index == 0) {
                // It is the last scene, just finish the activity
                mSceneStack.remove(index);
                scene.destroy();
                scene.setState(Scene.SCENE_STATE_DESTROY);
                getStageActivity().finish();
            } else {
                // TODO check scene state

                mSceneStack.remove(index);
                Scene previousState = getTopState();

                // If Scene is CLOSE

                if (previousState != null) {
                    previousState.resume();
                    scene.setState(Scene.SCENE_STATE_RUN);
                }

                scene.destroy();

                Curtain curtain = scene.getCurtain();
                if (curtain != null && previousState != null && curtain.isPreviousScene(previousState)) {
                    scene.setState(Scene.SCENE_STATE_CLOSE);
                    mLegacyScene = scene;
                    curtain.close(previousState, scene);
                    // detachFromeStage by curtain
                } else {
                    scene.setState(Scene.SCENE_STATE_DESTROY);
                    scene.detachFromeStage();
                }
            }
        } else {
            Log.e(TAG, "The scene is not in stage, " + scene);
        }
    }

    void removeLegacyScene(Scene scene) {
        if (mLegacyScene == scene) {
            mLegacyScene = null;
        }
    }

    private int getSceneIndex(@NonNull Scene scene) {
        int size  = mSceneStack.size();
        while (--size >= 0) {
            if (scene.equals(mSceneStack.get(size))) {
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
            return mSceneStack.peek();
        } else {
            return null;
        }
    }

    Stack<Scene> getSceneStack() {
        return mSceneStack;
    }

    protected void onSaveInstanceState(Bundle outState) {
        for (Scene scene : mSceneStack) {
            scene.saveInstanceState(outState);
        }
    }

    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        for (Scene scene : mSceneStack) {
            // Recreate
            scene.create(savedInstanceState);
            scene.restoreInstanceState(savedInstanceState);
        }
    }
}
