/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.IntIdGenerator;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class StageActivity extends AppCompatActivity {

    private static final String TAG = StageActivity.class.getSimpleName();

    public static final String KEY_SCENE_NAME = "stage_activity_scene_name";
    public static final String KEY_SCENE_ARGS = "stage_activity_scene_args";

    private static final String KEY_STAGE_ID = "stage_activity_stage_id";
    private static final String KEY_SCENE_TAG_LIST = "stage_activity_scene_tag_list";
    private static final String KEY_NEXT_ID = "stage_activity_next_id";

    // TODO ArrayList or LinkedList
    private ArrayList<String> mSceneTagList = new ArrayList<>();
    private final AtomicInteger mIdGenerator = new AtomicInteger();

    private int mStageId = IntIdGenerator.INVALID_ID;

    public abstract int getContainerViewId();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            String clazzStr = intent.getStringExtra(KEY_SCENE_NAME);
            if (TextUtils.isEmpty(clazzStr)) {
                return;
            }

            Class clazz;
            try {
                clazz = Class.forName(clazzStr);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Can't find class " + clazzStr, e);
                return;
            }

            Bundle args = intent.getBundleExtra(KEY_SCENE_ARGS);

            startScene(new Announcer(clazz).setArgs(args));
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mStageId = savedInstanceState.getInt(KEY_STAGE_ID, IntIdGenerator.INVALID_ID);
            mSceneTagList = savedInstanceState.getStringArrayList(KEY_SCENE_TAG_LIST);
            mIdGenerator.lazySet(savedInstanceState.getInt(KEY_NEXT_ID));
        }

        if (mStageId == IntIdGenerator.INVALID_ID) {
            ((SceneApplication) getApplicationContext()).registerStageActivity(this);
        } else {
            ((SceneApplication) getApplicationContext()).registerStageActivity(this, mStageId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((SceneApplication) getApplicationContext()).unregisterStageActivity(mStageId);
    }

    public void onSceneViewCreated(SceneFragment scene, Bundle savedInstanceState) {
    }

    public void onSceneViewDestroyed(SceneFragment scene) {
    }

    protected void onRegister(int id) {
        mStageId = id;
    }

    protected void onUnregister() {
    }

    public int getStageId() {
        return mStageId;
    }

    private SceneFragment newSceneInstance(Class<?> clazz) {
        try {
            return (SceneFragment) clazz.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Can't instance " + clazz.getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("The constructor of " +
                    clazz.getName() + " is not visible", e);
        } catch (ClassCastException e) {
            throw new IllegalStateException(clazz.getName() + " can not cast to scene", e);
        }
    }

    public void startScene(Announcer announcer) {
        Class<?> clazz = announcer.clazz;
        Bundle args = announcer.args;
        TransitionHelper tranHelper = announcer.tranHelper;
        boolean createNewScene = true;
        boolean removeAllTheOthers = (announcer.flag & SceneFragment.FLAG_REMOVE_ALL_THE_OTHER_SCENES) != 0;

        FragmentManager fragmentManager = getSupportFragmentManager();

        // Current fragment
        Fragment currentFragment = null;
        if (mSceneTagList.size() > 0) {
            // Get last tag
            String tag = mSceneTagList.get(mSceneTagList.size() - 1);
            currentFragment = fragmentManager.findFragmentByTag(tag);
        }

        // Launch mode single top
        if (currentFragment instanceof SceneFragment && clazz.isInstance(currentFragment)) {
            SceneFragment currentScene = (SceneFragment) currentFragment;
            if (currentScene.getLaunchMode() == SceneFragment.LAUNCH_MODE_SINGLE_TOP) {
                createNewScene = false;
                if (args != null) {
                    currentScene.onNewArguments(args);
                }
                if (!removeAllTheOthers) {
                    // Check request
                    if (announcer.requestFrom != null) {
                        currentScene.addRequest(announcer.requestFrom.getTag(), announcer.requestCode);
                    }
                    // Done!
                    return;
                }
            }
        }

        SceneFragment newScene = null;
        String newTag = null;
        if (createNewScene) {
            // Create new scene
            newScene = newSceneInstance(clazz);
            newScene.setArguments(args);

            // Create new scene tag
            newTag = Integer.toString(mIdGenerator.getAndIncrement());

            // Add new tag to list
            mSceneTagList.add(newTag);
        }

        // 1. createNewScene false, removeAllTheOthers false
        // Will not go here
        // 2. createNewScene false, removeAllTheOthers true
        // Keep current scene, remove all the others, no animation
        // 3. createNewScene true, removeAllTheOthers false
        // Add new scene, detach current fragment, with animation if exist currentFragment
        // 4. createNewScene true, removeAllTheOthers true
        // Add new scene, remove all the others, with animation if exist currentFragment
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // Animation
        if (newScene != null && currentFragment != null) {
            if (tranHelper == null || !tranHelper.onTransition(
                    this, transaction, currentFragment, newScene)) {
                // Clear shared item
                currentFragment.setSharedElementEnterTransition(null);
                currentFragment.setSharedElementReturnTransition(null);
                currentFragment.setEnterTransition(null);
                currentFragment.setExitTransition(null);
                newScene.setSharedElementEnterTransition(null);
                newScene.setSharedElementReturnTransition(null);
                newScene.setEnterTransition(null);
                newScene.setExitTransition(null);
                // Set default animation
                transaction.setCustomAnimations(R.anim.scene_open_enter, R.anim.scene_open_exit);
            }
        }
        // Remove scene
        if (removeAllTheOthers) {
            int startIndex = mSceneTagList.size() - 2;
            // Remove scene
            for (int i = startIndex; i >= 0; i--) {
                String tag = mSceneTagList.get(i);
                Fragment fragment = fragmentManager.findFragmentByTag(tag);
                if (fragment != null) {
                    transaction.remove(fragment);
                } else {
                    Log.d(TAG, "Can't find fragment with tag: " + tag);
                }
            }
            // Remove scene tag
            if (startIndex >= 0) {
                mSceneTagList.subList(0, startIndex + 1).clear();
            }
        }
        // Detach
        if (!removeAllTheOthers && currentFragment != null) {
            transaction.detach(currentFragment);
        }
        // Add
        if (newScene != null) {
            transaction.add(getContainerViewId(), newScene, newTag);
        }
        transaction.commit();

        // Check request
        if (newScene != null && announcer.requestFrom != null) {
            newScene.addRequest(announcer.requestFrom.getTag(), announcer.requestCode);
        }

        // Update SoftInputMode
        if (newScene != null) {
            getWindow().setSoftInputMode(newScene.getSoftInputMode());
        }
    }

    int getStackIndex(SceneFragment scene) {
        return getStackIndex(scene.getTag());
    }

    int getStackIndex(String tag) {
        return mSceneTagList.indexOf(tag); // Collections.binarySearch(mSceneTagList, tag);
    }

    // TODO What about id is negative
    int compareScene(String tag1, String tag2) throws NumberFormatException {
        int int1 = Integer.parseInt(tag1);
        int int2 = Integer.parseInt(tag2);
        return int1 - int2;
    }

    public void finishScene(SceneFragment scene) {
        finishScene(scene, null);
    }

    public void finishScene(SceneFragment scene, TransitionHelper transitionHelper) {
        finishScene(scene.getTag(), transitionHelper);
    }

    private void finishScene(String tag, TransitionHelper transitionHelper) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Get scene
        Fragment scene = fragmentManager.findFragmentByTag(tag);
        if (scene == null) {
            Log.e(TAG, "finishScene: Can't find scene by tag: " + tag);
            return;
        }

        // Get scene index
        int index = mSceneTagList.indexOf(tag);//Collections.binarySearch(mSceneTagList, tag);
        if (index < 0) {
            Log.e(TAG, "finishScene: Can't find the tag in tag list: " + tag);
            return;
        }

        if (mSceneTagList.size() == 1) {
            // It is the last fragment, finish Activity now
            Log.i(TAG, "finishScene: It is the last scene, finish activity now");
            finish();
            return;
        }

        Fragment next = null;
        if (index == mSceneTagList.size() - 1) {
            // It is first fragment, show the next one
            next = fragmentManager.findFragmentByTag(mSceneTagList.get(index - 1));
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (next != null) {
            if (transitionHelper == null || !transitionHelper.onTransition(
                    this, transaction, scene, next)) {
                // Clear shared item
                scene.setSharedElementEnterTransition(null);
                scene.setSharedElementReturnTransition(null);
                scene.setEnterTransition(null);
                scene.setExitTransition(null);
                next.setSharedElementEnterTransition(null);
                next.setSharedElementReturnTransition(null);
                next.setEnterTransition(null);
                next.setExitTransition(null);
                // Do not show animate if it is not the first fragment
                transaction.setCustomAnimations(R.anim.scene_close_enter, R.anim.scene_close_exit);
            }
            // Attach fragment
            transaction.attach(next);
        }
        transaction.remove(scene);
        transaction.commit();

        // Remove tag
        mSceneTagList.remove(index);

        // Return result
        if (scene instanceof SceneFragment) {
            ((SceneFragment) scene).returnResult(this);
        }

        // Update SoftInputMode
        if (next instanceof SceneFragment) {
            getWindow().setSoftInputMode(((SceneFragment) next).getSoftInputMode());
        }
    }

    @Override
    public void onBackPressed() {
        int size = mSceneTagList.size();
        String tag = mSceneTagList.get(size - 1);
        SceneFragment scene;
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            Log.e(TAG, "onBackPressed: Can't find scene by tag: " + tag);
            return;
        }
        if (!(fragment instanceof SceneFragment)) {
            Log.e(TAG, "onBackPressed: The fragment is not SceneFragment");
            return;
        }

        scene = (SceneFragment) fragment;
        scene.onBackPressed();
    }

    public SceneFragment findSceneByTag(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment != null) {
            return (SceneFragment) fragment;
        } else {
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_STAGE_ID, mStageId);
        outState.putStringArrayList(KEY_SCENE_TAG_LIST, mSceneTagList);
        outState.putInt(KEY_NEXT_ID, mIdGenerator.getAndIncrement());
    }
}
