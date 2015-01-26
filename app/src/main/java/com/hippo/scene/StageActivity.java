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

import android.view.View;
import android.widget.FrameLayout;

import com.hippo.ehviewer.ui.AbsActionBarActivity;

import java.util.LinkedList;
import java.util.Stack;

public abstract class StageActivity extends AbsActionBarActivity {

    private Stack<Scene> mSceneStack = new Stack<>();
    private LinkedList<ScenseAction> mScenseAction = new LinkedList<>();
    private Scene mCurrentScene = null;

    public abstract FrameLayout getStageView();

    protected void pushScene(Class<?> sceneClass) {
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

        Scene lastScence = mCurrentScene;
        mCurrentScene = scene;
        if (lastScence != null) {
            // It is not the first scense
            mSceneStack.push(lastScence);
            lastScence.pause();
        }
        mCurrentScene.create();
    }

    public void popScense() {
        if (mSceneStack.isEmpty()) {
            // TODO Should finish now ?
        } else {
            Scene previousScence = mCurrentScene;
            mCurrentScene = mSceneStack.pop();

            previousScence.onDestroy();
            mCurrentScene.onResume();
        }
    }

    void removeScenseView(View view) {
        getStageView().removeView(view);
    }

    void addScenseView(View view) {
        getStageView().addView(view);
    }

    @Override
    public void onBackPressed() {
        // TODO super.onBackPressed();
    }



    interface ScenseAction {
        public void doAction();
    }

    class PushScenseAction implements ScenseAction {
        @Override
        public void doAction() {

        }
    }

    class PopScenseAction implements ScenseAction {

        @Override
        public void doAction() {
            popScense();
        }
    }

}
