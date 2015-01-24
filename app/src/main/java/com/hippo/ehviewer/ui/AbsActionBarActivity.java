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

package com.hippo.ehviewer.ui;

import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;

public abstract class AbsActionBarActivity extends ActionBarActivity {
    
    public boolean post(Runnable action) {
        Window w = getWindow();
        if (w != null) {
            View v = w.getDecorView();
            return v != null && v.post(action);
        } else {
            return false;
        }
    }

    public boolean postDelayed(Runnable action, long delayMillis) {
        Window w = getWindow();
        if (w != null) {
            View v = w.getDecorView();
            return v != null && v.postDelayed(action, delayMillis);
        } else {
            return false;
        }
    }

}
