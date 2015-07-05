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

import android.app.Application;
import android.content.Context;
import android.util.SparseArray;

import com.hippo.util.IntIdGenerator;
import com.hippo.util.Log;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class SceneApplication extends Application {
    private static final String TAG = SceneApplication.class.getSimpleName();

    private SparseArray<SceneManager> mSceneManagers;
    private IntIdGenerator mIdGenerator;

    private RefWatcher mRefWatcher;

    @Override
    public void onCreate() {
        super.onCreate();

        mSceneManagers = new SparseArray<>();
        mIdGenerator = IntIdGenerator.create();

        mRefWatcher = LeakCanary.install(this);
    }

    private SceneManager createSceneManager() {
        int id = mIdGenerator.nextId();
        SceneManager sceneManager = new SceneManager(id);
        mSceneManagers.put(id, sceneManager);
        return sceneManager;
    }

    private SceneManager getSceneManager(int id) {
        SceneManager sceneManager = mSceneManagers.get(id);
        if (sceneManager == null) {
            Log.e(TAG, "Can't get SceneManager by id " + id);
            sceneManager = new SceneManager(id);
        }
        return sceneManager;
    }

    private void removeSceneManager(SceneManager sceneManager) {
        mSceneManagers.remove(sceneManager.getId());
    }

    static SceneManager createSceneManager(Context context) {
        SceneApplication application = (SceneApplication) context.getApplicationContext();
        return application.createSceneManager();
    }

    static SceneManager getSceneManager(Context context, int id) {
        SceneApplication application = (SceneApplication) context.getApplicationContext();
        return application.getSceneManager(id);
    }

    static void removeSceneManager(Context context, SceneManager sceneManager) {
        SceneApplication application = (SceneApplication) context.getApplicationContext();
        application.removeSceneManager(sceneManager);
    }

    public static RefWatcher getRefWatcher(Context context) {
        SceneApplication application = (SceneApplication) context.getApplicationContext();
        return application.mRefWatcher;
    }
}
