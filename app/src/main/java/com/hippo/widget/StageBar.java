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

package com.hippo.widget;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.util.UiUtils;

/**
 * It is shown at the top of stage, just like actionbar
 */
public class StageBar extends CardView {

    private Context mContext;

    public StageBar(Context context) {
        super(context);
    }

    public StageBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StageBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(Context context) {
        mContext = context;
        setRadius(UiUtils.dp2pix(context, 2));
    }

    public void updateContent(View view) {
        // TODO
    }

    public void endAnimation() {
        // TODO
    }

    public boolean hasContent() {
        // TODO
        return false;
    }



}
