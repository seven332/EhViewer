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

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class SceneApplication extends Application {

    private SceneManager mSceneManager;

    private RefWatcher mRefWatcher;

    @Override
    public void onCreate() {
        super.onCreate();

        mRefWatcher = LeakCanary.install(this);

        mSceneManager = SceneManager.getInstance();
        StageActivity.setSceneManager(mSceneManager);
        Scene.setSceneManager(mSceneManager);
    }

    public SceneManager getSceneManager() {
        return mSceneManager;
    }

    public static RefWatcher getRefWatcher(Context context) {
        SceneApplication application = (SceneApplication) context.getApplicationContext();
        return application.mRefWatcher;
    }
}
