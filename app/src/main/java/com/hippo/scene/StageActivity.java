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

import android.widget.FrameLayout;

import com.hippo.ehviewer.ui.AbsActionBarActivity;

import java.util.LinkedList;
import java.util.Stack;

public abstract class StageActivity extends AbsActionBarActivity {

    private Stack<Scene> mSceneStack = new Stack<>();
    private LinkedList<SceneAction> mScenseActionQueue = new LinkedList<>();
    private Scene mCurrentScene = null;

    private Scene mRetainedScene = null;

    public abstract FrameLayout getStageView();

    private void pushScene(Class sceneClass) {
        Scene scene = null;
        try {
            scene = (Scene) sceneClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Can't instance " + sceneClass.getName());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("The constructor of " +
                    sceneClass.getName() + " is not vivible");
        } catch (ClassCastException e) {
            throw new IllegalStateException(sceneClass.getName() + " can not cast to scene");
        }

        scene.setStageActivity(this);

        Scene lastScene = mCurrentScene;
        mCurrentScene = scene;
        mCurrentScene.create();
        if (lastScene != null) {
            // It is not the first scense
            mSceneStack.push(lastScene);

            mRetainedScene = lastScene;
            lastScene.pause();
        }
    }

    private void popScense() {
        if (mSceneStack.isEmpty()) {
            // It is the last scene
            mRetainedScene = mCurrentScene;
            mCurrentScene.destroy();
        } else {
            Scene previousScene = mCurrentScene;
            mCurrentScene = mSceneStack.pop();
            mRetainedScene = previousScene;

            mCurrentScene.resume();
            previousScene.destroy();
        }
    }

    void addSceneView(Scene scene) {
        if (scene != mCurrentScene) {
            throw new IllegalStateException("This scene should be current scene.");
        }

        getStageView().addView(scene.getRootView());
    }

    void stopSceneRetain(Scene scene) {
        if (mRetainedScene == null) {
            throw new IllegalStateException("Do not call dispatchRemove more than " +
                    "once.");
        }
        if (mRetainedScene != scene) {
            throw new IllegalStateException("Only call dispatchRemove in onPause " +
                    "and onDestroy");
        }

        Scene retainScene = mRetainedScene;
        mRetainedScene = null;
        getStageView().removeView(scene.getRootView());

        if (retainScene == mCurrentScene) {
            // No need to handle scene action
            mScenseActionQueue.clear();
            // It is the last scene, let's finish the activity.
            finish();
        } else {
            // Handle SceneAction
            if (!mScenseActionQueue.isEmpty()) {
                mScenseActionQueue.poll().doAction();
            }
        }
    }

    protected void startFirstScene(Class sceneClass) {
        if (mCurrentScene != null) {
            throw new IllegalStateException("Only call startFirstScene to start " +
                    "first scene.");
        }

        pushScene(sceneClass);
    }

    void startScene(Scene fromScene, Class sceneClass) {
        if (mRetainedScene == null) {
            // No retained scene, no animation
            if (fromScene == mCurrentScene) {
                // Only current scene can start new scene.
                // If not, just miss it.
                pushScene(sceneClass);
            }
        } else {
            mScenseActionQueue.offer(new StartSceneAction(fromScene, sceneClass));
        }
    }

    void finishScene(Scene scene) {
        if (scene != mRetainedScene && scene != mCurrentScene) {
            if (mSceneStack.contains(scene)) {
                // Remove without animation
                getStageView().removeView(scene.getRootView());
                mSceneStack.remove(scene);
            }
        } else {
            if (mRetainedScene == null) {
                // Because scene can't be null, so scene == mCurrentScene
                popScense();
            } else {
                mScenseActionQueue.offer(new FinishSceneAction(scene));
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentScene != null) {
            mCurrentScene.onBackPressed();
        }
    }

    interface SceneAction {
        public void doAction();
    }

    class StartSceneAction implements SceneAction {
        private Scene mFromScene;
        private Class mSceneClass;

        public StartSceneAction(Scene fromScene, Class sceneClass) {
            mFromScene = fromScene;
            mSceneClass = sceneClass;
        }

        @Override
        public void doAction() {
            startScene(mFromScene, mSceneClass);
        }
    }

    class FinishSceneAction implements SceneAction {
        private Scene mScene;

        public FinishSceneAction(Scene scene) {
            mScene = scene;
        }

        @Override
        public void doAction() {
            finishScene(mScene);
        }
    }

}
