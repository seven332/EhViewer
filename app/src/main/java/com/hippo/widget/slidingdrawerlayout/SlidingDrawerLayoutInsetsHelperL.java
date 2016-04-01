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

package com.hippo.widget.slidingdrawerlayout;

import android.support.v4.view.ViewCompat;
import android.view.View;

public class SlidingDrawerLayoutInsetsHelperL implements SlidingDrawerLayoutInsetsHelper {

    @Override
    public void setupForWindowInsets(View view) {
        if (ViewCompat.getFitsSystemWindows(view)) {
            // Now set the sys ui flags to enable us to lay out in the window insets
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
