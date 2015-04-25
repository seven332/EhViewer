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

package com.hippo.util;

import android.os.Handler;
import android.os.Looper;

public class AppHandler extends Handler {

    private static Handler sInstance;

    public static Handler getInstance() {
        if (sInstance == null) {
            sInstance = new AppHandler(Looper.getMainLooper());
        }
        return sInstance;
    }

    public AppHandler(Looper mainLooper) {
        super(mainLooper);
    }
}
