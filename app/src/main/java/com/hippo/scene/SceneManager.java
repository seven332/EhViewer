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

import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.IdIntGenerator;
import com.hippo.yorozuya.Say;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class SceneManager {

    private static final String TAG = SceneManager.class.getSimpleName();

    private int mId;

    private Stack<Scene> mSceneStack = new Stack<>();
    private StageActivity mStageActivity;

    private boolean mLegacySceneSetLock;
    private Set<Scene> mLegacySceneSet = new HashSet<>();
    private Set<Scene> mPrepareToDieSceneSet = new HashSet<>();

    private IdIntGenerator mIdGenerator = new IdIntGenerator();

    private List<SceneStateListener> mSceneStateListenerList = new ArrayList<>();

    SceneManager(int id) {
        mId = id;
    }

    int getId() {
        return mId;
    }

    int nextId() {
        return mIdGenerator.nextId();
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

    public void addSceneStateListener(SceneStateListener listener) {
        mSceneStateListenerList.add(listener);
    }

    public void removeSceneStateListener(SceneStateListener listener) {
        mSceneStateListenerList.remove(listener);
    }


    private void notifyInit(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onInit(scene);
        }
    }

    private void notifyRebirth(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onRebirth(scene);
        }
    }

    private void notifyCreate(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onCreate(scene);
        }
    }

    private void notifyBind(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onBind(scene);
        }
    }

    private void notifyRestore(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onRestore(scene);
        }
    }

    private void notifyDestroy(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onDestroy(scene);
        }
    }

    private void notifyDie(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onDie(scene);
        }
    }

    private void notifyPause(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onPause(scene);
        }
    }

    private void notifyResume(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onResume(scene);
        }
    }

    private void notifyOpen(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onOpen(scene);
        }
    }

    private void notifyClose(Scene scene) {
        for (SceneStateListener l : mSceneStateListenerList) {
            l.onClose(scene);
        }
    }

    private void checkLoop() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("It is not main loop!");
        }
    }

    /// Ture is finishScene is called
    private boolean checkPrepareToDie(Scene scene) {
        if (mPrepareToDieSceneSet.contains(scene)) {
            mPrepareToDieSceneSet.remove(scene);
            finishScene(scene, true);
            return true;
        } else {
            return false;
        }
    }

    Scene createSceneByClass(@NonNull Class sceneClass) {
        try {
            Scene scene = (Scene) sceneClass.newInstance();
            scene.setSceneManager(this);
            return scene;
        } catch (InstantiationException e) {
            throw new IllegalStateException("Can't instance " + sceneClass.getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("The constructor of " +
                    sceneClass.getName() + " is not visible", e);
        } catch (ClassCastException e) {
            throw new IllegalStateException(sceneClass.getName() + " can not cast to scene", e);
        }
    }

    // TODO check previousState state
    void startScene(@NonNull Class sceneClass, @Nullable Announcer announcer,
            @Nullable Curtain curtain) {
        checkLoop();

        if (!isStageAlive()) {
            throw new IllegalStateException("Stage is not alive, but attemp to create " + sceneClass.getSimpleName());
        }

        // check LAUNCH_MODE_SINGLE_TOP
        Scene topScene = getTopScene();
        if (sceneClass.isInstance(topScene) &&
                topScene.getLaunchMode() == Scene.LAUNCH_MODE_SINGLE_TOP) {
            topScene.onNewAnnouncer(announcer);
            return;
        }

        Scene scene = createSceneByClass(sceneClass);
        scene.setAnnouncer(announcer);
        scene.setCurtain(curtain);

        startScene(scene, curtain);
    }

    void showDialog(@NonNull SceneDialog dialog, @Nullable Curtain curtain) {
        checkLoop();

        if (!isStageAlive()) {
            Say.w(TAG, "Stage is not alive, but attemp to show dialog " + dialog.toString());
            return;
        }

        dialog.setCurtain(curtain);

        startScene(dialog, curtain);
    }

    private void updateSceneVisibility() {
        int pointer = mSceneStack.size() - 1;
        for (; pointer >= 0; pointer--) {
            Scene scene = mSceneStack.get(pointer);
            scene.setHide(false);
            if (scene.coverCompletely()) {
                break;
            }
        }
        for (pointer--; pointer >= 0; pointer--) {
            mSceneStack.get(pointer).setHide(true);
        }
    }

    private void startScene(Scene scene, Curtain curtain) {
        Scene previousScene = getTopScene();
        mSceneStack.push(scene);

        if (curtain != null) {
            curtain.setPreviousScene(previousScene);
        }

        if (previousScene != null) {
            previousScene.endCurtainAnimation();
            previousScene.pause();
            notifyPause(previousScene);
            previousScene.setState(Scene.SCENE_STATE_PAUSE);
        }

        scene.init();
        notifyInit(scene);
        scene.setState(Scene.SCENE_STATE_PREPARE);
        scene.create(false);
        notifyCreate(scene);
        scene.bind();
        notifyBind(scene);

        // Update fit padding
        int fitPaddingBottom = getStageActivity().getFitPaddingBottom();
        if (fitPaddingBottom > 0) {
            scene.setFitPaddingBottom(fitPaddingBottom);
        }

        scene.setState(Scene.SCENE_STATE_RUN);
        if (!checkPrepareToDie(scene)) {
            // No finish called. Go on
            if (curtain != null && previousScene != null) {
                scene.setState(Scene.SCENE_STATE_OPEN);
                curtain.open(scene, previousScene);
            } else {
                // No curtain, set visiblity for previousState here
                if (previousScene != null) {
                    updateSceneVisibility();
                }
            }
        }
    }

    /**
     * End all legacy scene animation at once
     *
     * @return True if legacy scene exist and its curtain animation is running
     */
    boolean endLegacyScene() {
        if (!mLegacySceneSet.isEmpty()) {
            mLegacySceneSetLock = true;
            for (Scene scene : mLegacySceneSet) {
                scene.endCurtainAnimation();
            }
            mLegacySceneSetLock = false;
            mLegacySceneSet.clear();
            return true;
        } else {
            return false;
        }
    }

    void finishScene(@NonNull Scene scene, boolean noAnimation) {
        checkLoop();

        int index = getSceneIndex(scene);
        if (index >= 0 && index < mSceneStack.size()) {
            if (scene.canNotBeSaved()) {
                // the scene is hopeless
                scene.endCurtainAnimation();
                return;
            }

            if (!scene.isRunning()) {
                // the scene prepare to die
                mPrepareToDieSceneSet.add(scene);
                scene.endCurtainAnimation();
                return;
            }

            // TODO If the cene is under a dialog, the previous may not show
            if (index != mSceneStack.size() - 1 || index == 0) {
                // It is not the first or it is the last scene
                mSceneStack.remove(index);
                scene.destroy(true);
                notifyDestroy(scene);
                scene.setState(Scene.SCENE_STATE_DESTROY);
                scene.die(index == 0);
                notifyDie(scene);
                scene.setState(Scene.SCENE_STATE_DIE);

                if (index == 0) {
                    // It is the last scene, just finish the activity
                    getStageActivity().finish();
                } else {
                    // Update the scene under target scene visibility
                    updateSceneVisibility();
                }
            } else {
                mSceneStack.remove(index);
                Scene previousScene = mSceneStack.get(index - 1);

                previousScene.resume();
                notifyResume(previousScene);
                previousScene.setState(Scene.SCENE_STATE_RUN);

                scene.destroy(true);
                notifyDestroy(scene);

                // Update scene visibility
                updateSceneVisibility();

                Curtain curtain = scene.getCurtain();
                if (!noAnimation && curtain != null &&
                        (!curtain.needSpecifyPreviousScene() || curtain.isPreviousScene(previousScene))) {
                    scene.setState(Scene.SCENE_STATE_CLOSE);
                    curtain.close(previousScene, scene);
                    // add the scene to legacy set
                    mLegacySceneSet.add(scene);
                    // curtain will kill the scene
                } else {
                    scene.setState(Scene.SCENE_STATE_DESTROY);
                    scene.die(false);
                    notifyDie(scene);
                    scene.setState(Scene.SCENE_STATE_DIE);

                    previousScene.setHide(false);
                }
            }
        } else {
            Say.e(TAG, "The scene is not in stage, " + scene);
        }
    }

    private void checkSceneState(Scene scene, int expected) {
        AssertUtils.assertEquals(this + " state is " + scene.getState() + ", but it should be " + expected,
                expected, scene.getState());
    }

    void openScene(Scene scene) {
        checkSceneState(scene, Scene.SCENE_STATE_OPEN);
        scene.open();
        notifyOpen(scene);
        scene.setState(Scene.SCENE_STATE_RUN);
        checkPrepareToDie(scene);
        updateSceneVisibility();
    }

    void closeScene(Scene scene) {
        checkSceneState(scene, Scene.SCENE_STATE_CLOSE);
        scene.close();
        notifyClose(scene);
        scene.setState(Scene.SCENE_STATE_DESTROY);
        scene.die(false);
        notifyDie(scene);
        scene.setState(Scene.SCENE_STATE_DIE);

        // remove the scene from legacy set
        if (!mLegacySceneSetLock) {
            mLegacySceneSet.remove(scene);
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
        Scene scene = getTopScene();
        if (scene != null) {
            scene.onBackPressedInternal();
            return true;
        } else {
            Say.w(TAG, "There is no scene in the stage");
            return false;
        }
    }

    private Scene getTopScene() {
        if (!mSceneStack.isEmpty()) {
            return mSceneStack.peek();
        } else {
            return null;
        }
    }

    Stack<Scene> getSceneStack() {
        return mSceneStack;
    }

    int getSceneCount() {
        return mSceneStack.size();
    }

    protected void onSaveInstanceState(Bundle outState) {
        for (Scene scene : mSceneStack) {
            scene.saveInstanceState(outState);
        }
    }

    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        for (Scene scene : mSceneStack) {
            scene.destroy(false);
            notifyDestroy(scene);
            scene.setState(Scene.SCENE_STATE_REBIRTH);
            scene.rebirth();
            notifyRebirth(scene);
            scene.setState(Scene.SCENE_STATE_PREPARE);
            scene.create(true);
            notifyCreate(scene);
            scene.restore();
            notifyRestore(scene);
            scene.setState(Scene.SCENE_STATE_RUN);
            scene.restoreInstanceState(savedInstanceState);
        }
    }

    public interface SceneStateListener {

        void onInit(Scene scene);

        void onRebirth(Scene scene);

        void onCreate(Scene scene);

        void onBind(Scene scene);

        void onRestore(Scene scene);

        void onDestroy(Scene scene);

        void onDie(Scene scene);

        void onPause(Scene scene);

        void onResume(Scene scene);

        void onOpen(Scene scene);

        void onClose(Scene scene);
    }
}
