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

package com.hippo.ehviewer;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.hippo.scene.SceneFragment;


public final class Analytics {

    private static FirebaseAnalytics analytics;

    private Analytics() {}

    public static void start(Context context) {
        analytics = FirebaseAnalytics.getInstance(context);
        analytics.setUserId(Settings.getUserID());
    }

    public static boolean isEnabled() {
        return analytics != null && Settings.getEnableAnalytics();
    }

    public static void onSceneView(SceneFragment scene) {
        if (isEnabled()) {
            Bundle bundle = new Bundle();
            bundle.putString("scene_simple_class", scene.getClass().getSimpleName());
            bundle.putString("scene_class", scene.getClass().getName());
            analytics.logEvent("scene_view", bundle);
        }
    }
}
