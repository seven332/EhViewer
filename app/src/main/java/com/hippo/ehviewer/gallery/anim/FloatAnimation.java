/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.hippo.ehviewer.gallery.anim;

public class FloatAnimation extends Animation {

    private final float mFrom;
    private final float mTo;
    private float mCurrent;

    public FloatAnimation(float from, float to) {
        mFrom = from;
        mTo = to;
        mCurrent = from;
    }

    @Override
    protected void onCalculate(float progress) {
        mCurrent = mFrom + (mTo - mFrom) * progress;
    }

    public float get() {
        return mCurrent;
    }
}
