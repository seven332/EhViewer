/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.hippo.ehviewer.AppContext;

public class RatingView extends TextView {

    private static final char START_EMPTY = '\uFA73';
    private static final char START_HALF = '\uFA74';
    private static final char START_FULL = '\uFA75';

    private int mRating = -1;

    public RatingView(Context context) {
        super(context);
        init(context);
    }
    public RatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public RatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setTypeface(((AppContext)context.getApplicationContext()).getFaceTypeface());
    }

    public void setRating(float rating) {
        mRating = Math.min(10, Math.round(rating * 2));
        int fullNum = mRating / 2;
        int halfNum = mRating % 2;
        int emptyNum = 5 - fullNum - halfNum;
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < fullNum; i++)
            sb.append(START_FULL);
        if (halfNum == 1)
            sb.append(START_HALF);
        for (int i = 0; i < emptyNum; i++)
            sb.append(START_EMPTY);
        setText(sb.toString());
    }
}
