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

package com.hippo.widget.recyclerview;

import android.content.Context;
import android.support.v7.widget.LinearSmoothScroller;

public abstract class SimpleSmoothScroller extends LinearSmoothScroller {

    private float mMillisecondsPerPx;

    public SimpleSmoothScroller(Context context, float millisecondsPerInch) {
        super(context);
        mMillisecondsPerPx = millisecondsPerInch / context.getResources().getDisplayMetrics().densityDpi;
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        if (mMillisecondsPerPx <= 0) {
            return super.calculateTimeForScrolling(dx);
        } else {
            return (int) Math.ceil(Math.abs(dx) * mMillisecondsPerPx);
        }
    }
}
