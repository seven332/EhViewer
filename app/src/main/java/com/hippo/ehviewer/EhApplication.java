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

package com.hippo.ehviewer;

import android.content.Context;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.hippo.vectorold.content.VectorContext;
import com.hippo.ehviewer.network.EhOkHttpClient;
import com.hippo.ehviewer.util.Config;
import com.hippo.scene.SceneApplication;
import com.hippo.util.Log;

public class EhApplication extends SceneApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
                .newBuilder(this, EhOkHttpClient.getInstance())
                .build();
        Fresco.initialize(this, config);
        Config.initialize(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(VectorContext.wrapContext(newBase));
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Log.d("Application hiden");
        }
    }
}
