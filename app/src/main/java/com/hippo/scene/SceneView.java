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

package com.hippo.scene;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.AbsoluteLayout;

@SuppressWarnings("deprecation")
public class SceneView extends AbsoluteLayout {

    private boolean mEnableTouch = true;

    public SceneView(Context context) {
        super(context);
        init();
    }

    public SceneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SceneView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
    }

    public void setEnableTouch(boolean enableTouch) {
        mEnableTouch = enableTouch;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // Don't dispatch it to children
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        return mEnableTouch && super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(@NonNull MotionEvent event) {
        return mEnableTouch && super.dispatchGenericMotionEvent(event);
    }
}
