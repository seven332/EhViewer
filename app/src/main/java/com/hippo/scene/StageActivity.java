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
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.IntIdGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class StageActivity extends AppCompatActivity {

    private static final String TAG = StageActivity.class.getSimpleName();

    public static final String KEY_SCENE_NAME = "stage_activity_scene_name";
    public static final String KEY_SCENE_ARGS = "stage_activity_scene_args";

    private static final String KEY_STAGE_ID = "stage_activity_stage_id";
    private static final String KEY_SCENE_TAG_LIST = "stage_activity_scene_tag_list";
    private static final String KEY_NEXT_ID = "stage_activity_next_id";

    private ArrayList<String> mSceneTagList = new ArrayList<>();
    private final AtomicInteger mIdGenerator = new AtomicInteger();

    private int mStageId = IntIdGenerator.INVALID_ID;

    private static final Map<Class<?>, Integer> sLaunchModeMap = new HashMap<>();

    public static void registerLaunchMode(Class<?> clazz, @SceneFragment.LaunchMode int launchMode) {
        if (launchMode != SceneFragment.LAUNCH_MODE_STANDARD &&
                launchMode != SceneFragment.LAUNCH_MODE_SINGLE_TOP &&
                launchMode != SceneFragment.LAUNCH_MODE_SINGLE_TASK) {
            throw new IllegalStateException("Invalid launch mode: " + launchMode);
        }
        sLaunchModeMap.put(clazz, launchMode);
    }

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

    public int getSceneLaunchMode(Class<?> clazz) {
        Integer integer = sLaunchModeMap.get(clazz);
        if (integer == null) {
            throw new RuntimeException("Not register " + clazz.getName());
        } else {
            return integer;
        }
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
        FragmentManager fragmentManager = getSupportFragmentManager();
        int launchMode = getSceneLaunchMode(clazz);

        // Check LAUNCH_MODE_SINGLE_TASK
        if (launchMode == SceneFragment.LAUNCH_MODE_SINGLE_TASK) {
            for (int i = 0, n = mSceneTagList.size(); i < n; i++) {
                String tag = mSceneTagList.get(i);
                Fragment fragment = fragmentManager.findFragmentByTag(tag);
                if (fragment == null) {
                    Log.e(TAG, "Can't find fragment with tag: " + tag);
                    continue;
                }

                if (clazz.isInstance(fragment)) { // Get it
                    FragmentTransaction transaction = fragmentManager.beginTransaction();

                    // Use default animation
                    transaction.setCustomAnimations(R.anim.scene_open_enter, R.anim.scene_open_exit);

                    // Remove top fragments
                    for (int j = i + 1; j < n; j++) {
                        String topTag = mSceneTagList.get(j);
                        Fragment topFragment = fragmentManager.findFragmentByTag(topTag);
                        if (null == topFragment) {
                            Log.e(TAG, "Can't find fragment with tag: " + topTag);
                            continue;
                        }
                        // Clear shared element
                        topFragment.setSharedElementEnterTransition(null);
                        topFragment.setSharedElementReturnTransition(null);
                        topFragment.setEnterTransition(null);
                        topFragment.setExitTransition(null);
                        // Remove it
                        transaction.remove(topFragment);
                    }

                    // Remove tag from index i+1
                    mSceneTagList.subList(i + 1, mSceneTagList.size()).clear();

                    // Attach fragment
                    if (fragment.isDetached()) {
                        transaction.attach(fragment);
                    }

                    // Commit
                    transaction.commit();

                    // New arguments
                    if (args != null && fragment instanceof SceneFragment) {
                        // TODO Call onNewArguments when view created ?
                        ((SceneFragment) fragment).onNewArguments(args);
                    }

                    return;
                }
            }
        }

        // Get current fragment
        SceneFragment currentScene = null;
        if (mSceneTagList.size() > 0) {
            // Get last tag
            String tag = mSceneTagList.get(mSceneTagList.size() - 1);
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                AssertUtils.assertInstanceOf(fragment, SceneFragment.class);
                currentScene = (SceneFragment) fragment;
            }
        }

        // Check LAUNCH_MODE_SINGLE_TASK
        if (clazz.isInstance(currentScene) && launchMode == SceneFragment.LAUNCH_MODE_SINGLE_TOP) {
            if (args != null) {
                currentScene.onNewArguments(args);
            }
            return;
        }

        // Create new scene
        SceneFragment newScene = newSceneInstance(clazz);
        newScene.setArguments(args);

        // Create new scene tag
        String newTag = Integer.toString(mIdGenerator.getAndIncrement());

        // Add new tag to list
        mSceneTagList.add(newTag);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // Animation
        if (currentScene != null) {
            if (tranHelper == null || !tranHelper.onTransition(
                    this, transaction, currentScene, newScene)) {
                // Clear shared item
                currentScene.setSharedElementEnterTransition(null);
                currentScene.setSharedElementReturnTransition(null);
                currentScene.setEnterTransition(null);
                currentScene.setExitTransition(null);
                newScene.setSharedElementEnterTransition(null);
                newScene.setSharedElementReturnTransition(null);
                newScene.setEnterTransition(null);
                newScene.setExitTransition(null);
                // Set default animation
                transaction.setCustomAnimations(R.anim.scene_open_enter, R.anim.scene_open_exit);
            }
            // Detach current scene
            if (!currentScene.isDetached()) {
                transaction.detach(currentScene);
            } else {
                Log.e(TAG, "Current scene is detached");
            }
        }

        // Add new scene
        transaction.add(getContainerViewId(), newScene, newTag);

        // Commit
        transaction.commit();

        // Check request
        if (announcer.requestFrom != null) {
            newScene.addRequest(announcer.requestFrom.getTag(), announcer.requestCode);
        }
    }

    public void startSceneFirstly(Announcer announcer) {
        Class<?> clazz = announcer.clazz;
        Bundle args = announcer.args;
        FragmentManager fragmentManager = getSupportFragmentManager();
        int launchMode = getSceneLaunchMode(clazz);
        boolean forceNewScene = launchMode == SceneFragment.LAUNCH_MODE_STANDARD;
        boolean createNewScene = true;
        boolean findScene = false;
        SceneFragment scene = null;

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Set default animation
        transaction.setCustomAnimations(R.anim.scene_open_enter, R.anim.scene_open_exit);

        String findSceneTag = null;
        for (int i = 0, n = mSceneTagList.size(); i < n; i++) {
            String tag = mSceneTagList.get(i);
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment == null) {
                Log.e(TAG, "Can't find fragment with tag: " + tag);
                continue;
            }

            // Clear shared element
            fragment.setSharedElementEnterTransition(null);
            fragment.setSharedElementReturnTransition(null);
            fragment.setEnterTransition(null);
            fragment.setExitTransition(null);

            // Check is target scene
            if (!forceNewScene && !findScene && clazz.isInstance(fragment) &&
                    (launchMode == SceneFragment.LAUNCH_MODE_SINGLE_TASK || !fragment.isDetached())) {
                scene = (SceneFragment) fragment;
                findScene = true;
                createNewScene = false;
                findSceneTag = tag;
                if (fragment.isDetached()) {
                    transaction.attach(fragment);
                }
            } else {
                // Remove it
                transaction.remove(fragment);
            }
        }

        // Handle tag list
        mSceneTagList.clear();
        if (null != findSceneTag) {
            mSceneTagList.add(findSceneTag);
        }

        if (createNewScene) {
            scene = newSceneInstance(clazz);
            scene.setArguments(args);

            // Create scene tag
            String tag = Integer.toString(mIdGenerator.getAndIncrement());

            // Add tag to list
            mSceneTagList.add(tag);

            // Add scene
            transaction.add(getContainerViewId(), scene, tag);
        }

        // Commit
        transaction.commit();

        if (!createNewScene && args != null) {
            // TODO Call onNewArguments when view created ?
            scene.onNewArguments(args);
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
