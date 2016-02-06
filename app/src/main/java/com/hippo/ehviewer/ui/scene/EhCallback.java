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

package com.hippo.ehviewer.ui.scene;

import android.content.Context;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;

public abstract class EhCallback<E extends SceneFragment, T> implements EhClient.Callback<T>  {

    private EhApplication mApplication;
    private int mStageId;
    private String mSceneTag;

    public EhCallback(Context context, int stageId, String sceneTag) {
        mApplication = (EhApplication) context.getApplicationContext();
        mStageId = stageId;
        mSceneTag = sceneTag;
    }

    public abstract boolean isInstance(SceneFragment scene);

    @SuppressWarnings("unchecked")
    public E getScene() {
        StageActivity stage = mApplication.findStageActivityById(mStageId);
        if (stage == null) {
            return null;
        }
        SceneFragment scene = stage.findSceneByTag(mSceneTag);
        if (isInstance(scene)) {
            return (E) scene;
        } else {
            return null;
        }
    }
}
