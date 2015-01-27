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

package com.hippo.ehviewer.ui.scene;

import com.hippo.ehviewer.R;
import com.hippo.scene.Scene;

public class GalleryListScene extends Scene {

    @Override
    public void onCreate() {
        setContentView(R.layout.scene_gallery_list);
    }

    @Override
    public void onResume() {
        // Empty
    }

    @Override
    public void onPause() {
        dispatchRemove();
    }

    @Override
    public void onDestroy() {
        dispatchRemove();
    }
}
