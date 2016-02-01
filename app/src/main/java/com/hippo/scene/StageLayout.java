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

package com.hippo.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hippo.ehviewer.R;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class StageLayout extends FrameLayout {

    private Field mDisappearingChildrenField;
    private ArrayList<View> mSuperDisappearingChildren;
    private View mDumpView;
    private boolean mDoTrick;

    public StageLayout(Context context) {
        super(context);
        init(context);
    }

    public StageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            mDisappearingChildrenField = ViewGroup.class.getDeclaredField("mDisappearingChildren");
            mDisappearingChildrenField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        if (mDisappearingChildrenField != null) {
            mDumpView = new View(context);
            addView(mDumpView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }
    }

    @SuppressWarnings("unchecked")
    private void getSuperDisappearingChildren() {
        if (mDisappearingChildrenField == null || mSuperDisappearingChildren != null) {
            return;
        }

        try {
            mSuperDisappearingChildren = (ArrayList<View>) mDisappearingChildrenField.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private String getViewTag(View view) {
        Object tag = view.getTag(R.id.fragment_tag);
        if (tag instanceof String) {
            return (String) tag;
        } else {
            return null;
        }
    }

    private boolean beforeDispatchDraw() {
        getSuperDisappearingChildren();

        if (mSuperDisappearingChildren == null ||
                mSuperDisappearingChildren.size() <= 0 || getChildCount() <= 1) { // dump view
            return false;
        }

        StageActivity stage = null;
        Context context = getContext();
        if (context instanceof StageActivity) {
            stage = (StageActivity) context;
        }
        if (stage == null) {
            return false;
        }

        View view1 = mSuperDisappearingChildren.get(0);
        View view2 = getChildAt(1); // dump view
        String tag1 = getViewTag(view1);
        String tag2 = getViewTag(view2);
        try {
            return stage.compareScene(tag1, tag2) < 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mDoTrick = beforeDispatchDraw();
        super.dispatchDraw(canvas);
        mDoTrick = false;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        ArrayList<View> disappearingChildren = mSuperDisappearingChildren;

        if (mDoTrick) {
            if (child == mDumpView) {
                boolean more = false;
                for (int i = disappearingChildren.size() - 1; i >= 0; i--) {
                    more |= super.drawChild(canvas, disappearingChildren.get(i), drawingTime);
                }
                return more;
            } else if (disappearingChildren.contains(child)) {
                // Skip
                return false;
            }
        }

        return super.drawChild(canvas, child, drawingTime);
    }
}
