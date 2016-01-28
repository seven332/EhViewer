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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.ehviewer.R;
import com.hippo.widget.OffsetLayout;
import com.hippo.yorozuya.IntList;
import com.hippo.yorozuya.SimpleHandler;

import java.util.ArrayList;
import java.util.List;

public class FitPaddingLayout extends OffsetLayout {

    private static final String KEY_SUPER = "super";
    private static final String KEY_PADDING_TOP_1 = "padding_top_1";
    private static final String KEY_PADDING_TOP_2 = "padding_top_2";
    private static final String KEY_PADDING_TOP_3 = "padding_top_3";
    private static final String KEY_PADDING_BOTTOM_1 = "padding_bottom_1";
    private static final String KEY_PADDING_BOTTOM_2 = "padding_bottom_2";
    private static final String KEY_PADDING_BOTTOM_3 = "padding_bottom_3";

    private int mFitTop1;
    private int mFitTop2;
    private int mFitTop3;
    private int mContentId1;
    private int mContentId2;
    private int mContentId3;
    private int mFitBottom1;
    private int mFitBottom2;
    private int mFitBottom3;

    private boolean mOnce;

    private int mOriginalPaddingTop1 = -1;
    private int mOriginalPaddingTop2 = -1;
    private int mOriginalPaddingTop3 = -1;
    private int mOriginalPaddingBottom1 = -1;
    private int mOriginalPaddingBottom2 = -1;
    private int mOriginalPaddingBottom3 = -1;

    private boolean mEnableUpdatePaddingTop = true;

    private boolean mUpdated = false;

    private List<View> mContentList = new ArrayList<>();
    private IntList mPaddingTopList = new IntList();
    private IntList mPaddingBottomList = new IntList();

    private Runnable mUpdatePaddingTask = new Runnable() {
        @Override
        public void run() {
            for (int i = 0, size = mContentList.size(); i < size; i++) {
                View content = mContentList.get(i);
                int paddingTop = mPaddingTopList.get(i);
                int paddingBottom = mPaddingBottomList.get(i);
                content.setPadding(content.getPaddingLeft(),
                        paddingTop == -1 ? content.getPaddingTop() : paddingTop,
                        content.getPaddingRight(),
                        paddingBottom == -1 ? content.getPaddingBottom() : paddingBottom);
            }
            mContentList.clear();
            mPaddingTopList.clear();
            mPaddingBottomList.clear();
        }
    };

    public FitPaddingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FitPaddingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FitPaddingLayout);
        mFitTop1 = a.getResourceId(R.styleable.FitPaddingLayout_fitTop1, 0);
        mFitTop2 = a.getResourceId(R.styleable.FitPaddingLayout_fitTop2, 0);
        mFitTop3 = a.getResourceId(R.styleable.FitPaddingLayout_fitTop3, 0);
        mContentId1 = a.getResourceId(R.styleable.FitPaddingLayout_content1, 0);
        mContentId2 = a.getResourceId(R.styleable.FitPaddingLayout_content2, 0);
        mContentId3 = a.getResourceId(R.styleable.FitPaddingLayout_content3, 0);
        mFitBottom1 = a.getResourceId(R.styleable.FitPaddingLayout_fitBottom1, 0);
        mFitBottom2 = a.getResourceId(R.styleable.FitPaddingLayout_fitBottom2, 0);
        mFitBottom3 = a.getResourceId(R.styleable.FitPaddingLayout_fitBottom3, 0);
        mOnce = a.getBoolean(R.styleable.FitPaddingLayout_once, true);
        a.recycle();
    }

    public void setEnableUpdatePaddingTop(boolean enableUpdatePaddingTop) {
        mEnableUpdatePaddingTop = enableUpdatePaddingTop;
    }

    @Nullable
    private View findViewByIdEx(int id) {
        return id == 0 ? null : findViewById(id);
    }

    private boolean updatePadding(View content, View fitTop, View fitBottom,
            int originalPaddingTop, int originalPaddingBottom, int[] paddings, int[] newPaddins) {
        if (content == null || (fitTop == null && fitBottom == null)) {
            return false;
        }
        if (originalPaddingTop == -1) {
            originalPaddingTop = content.getPaddingTop();
        }
        if (originalPaddingBottom == -1) {
            originalPaddingBottom = content.getPaddingBottom();
        }
        paddings[0] = originalPaddingTop;
        paddings[1] = originalPaddingBottom;

        int newPaddingTop = -1;
        int newPaddingBottom = -1;
        if (fitTop != null) {
            newPaddingTop = originalPaddingTop + fitTop.getBottom() -
                    ((LayoutParams) fitTop.getLayoutParams()).offsetY;
        }
        if (fitBottom != null) {
            newPaddingBottom = originalPaddingBottom + getHeight() - fitBottom.getTop() -
                    ((LayoutParams) fitBottom.getLayoutParams()).offsetY;
        }
        newPaddins[0] = newPaddingTop;
        newPaddins[1] = newPaddingBottom;

        return true;
    }

    private boolean isNeedUpdatePadding(View content, int paddingTop, int paddingBottom) {
        return (paddingTop != -1 || paddingTop != content.getPaddingTop()) ||
                (paddingBottom != -1 || paddingBottom != content.getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!mEnableUpdatePaddingTop || (mOnce && mUpdated)) {
            return;
        }

        View fitTop1 = findViewById(mFitTop1);
        View fitTop2 = findViewById(mFitTop2);
        View fitTop3 = findViewById(mFitTop3);
        View content1 = findViewById(mContentId1);
        View content2 = findViewById(mContentId2);
        View content3 = findViewById(mContentId3);
        View fitBottom1 = findViewById(mFitBottom1);
        View fitBottom2 = findViewById(mFitBottom2);
        View fitBottom3 = findViewById(mFitBottom3);

        mContentList.clear();
        mPaddingTopList.clear();
        mPaddingBottomList.clear();

        int[] paddings = new int[2];
        int[] newPaddings = new int[2];
        if (updatePadding(content1, fitTop1, fitBottom1, mOriginalPaddingTop1,
                mOriginalPaddingBottom1, paddings, newPaddings)) {
            mOriginalPaddingTop1 = paddings[0];
            mOriginalPaddingBottom1 = paddings[1];
            if (isNeedUpdatePadding(content1, newPaddings[0], newPaddings[1])) {
                mContentList.add(content1);
                mPaddingTopList.add(newPaddings[0]);
                mPaddingBottomList.add(newPaddings[1]);
            }
        }
        if (updatePadding(content2, fitTop2, fitBottom2, mOriginalPaddingTop2,
                mOriginalPaddingBottom2, paddings, newPaddings)) {
            mOriginalPaddingTop2 = paddings[0];
            mOriginalPaddingBottom2 = paddings[1];
            if (isNeedUpdatePadding(content2, newPaddings[0], newPaddings[1])) {
                mContentList.add(content2);
                mPaddingTopList.add(newPaddings[0]);
                mPaddingBottomList.add(newPaddings[1]);
            }
        }
        if (updatePadding(content3, fitTop3, fitBottom3, mOriginalPaddingTop3,
                mOriginalPaddingBottom3, paddings, newPaddings)) {
            mOriginalPaddingTop3 = paddings[0];
            mOriginalPaddingBottom3 = paddings[1];
            if (isNeedUpdatePadding(content3, newPaddings[0], newPaddings[1])) {
                mContentList.add(content3);
                mPaddingTopList.add(newPaddings[0]);
                mPaddingBottomList.add(newPaddings[1]);
            }
        }

        if (mContentList.size() > 0) {
            SimpleHandler.getInstance().post(mUpdatePaddingTask);
        }

        mUpdated = true;
    }

    private void saveContentPadding(Bundle state, int id, String topKey, String bottomKey) {
        View content = findViewByIdEx(id);
        if (content != null) {
            state.putInt(topKey, content.getPaddingTop());
            state.putInt(bottomKey, content.getPaddingBottom());
        } else {
            state.putInt(topKey, -1);
            state.putInt(bottomKey, -1);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(KEY_SUPER, super.onSaveInstanceState());
        saveContentPadding(state, mContentId1, KEY_PADDING_TOP_1, KEY_PADDING_BOTTOM_1);
        saveContentPadding(state, mContentId2, KEY_PADDING_TOP_2, KEY_PADDING_BOTTOM_2);
        saveContentPadding(state, mContentId3, KEY_PADDING_TOP_3, KEY_PADDING_BOTTOM_3);
        return state;
    }

    private void restoreContentPadding(Bundle state, int id, String topKey, String bottomKey, int[] paddings) {
        View content = findViewByIdEx(id);
        if (content != null) {
            paddings[0] = content.getPaddingTop();
            paddings[1] = content.getPaddingBottom();
            int paddingTop = state.getInt(topKey);
            int paddingBottom = state.getInt(bottomKey);
            if (paddingTop != -1 || paddingBottom != -1) {
                content.setPadding(content.getPaddingLeft(),
                        paddingTop == -1 ? content.getPaddingTop() : paddingTop,
                        content.getPaddingRight(),
                        paddingBottom == -1 ? content.getPaddingBottom() : paddingBottom);
            }
        } else {
            paddings[0] = -1;
            paddings[1] = -1;
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(KEY_SUPER));
            int[] ints = new int[2];
            restoreContentPadding(savedState, mContentId1, KEY_PADDING_TOP_1, KEY_PADDING_BOTTOM_1, ints);
            mOriginalPaddingTop1 = ints[0];
            mOriginalPaddingBottom1 = ints[1];
            restoreContentPadding(savedState, mContentId2, KEY_PADDING_TOP_2, KEY_PADDING_BOTTOM_2, ints);
            mOriginalPaddingTop2 = ints[0];
            mOriginalPaddingBottom2 = ints[1];
            restoreContentPadding(savedState, mContentId3, KEY_PADDING_TOP_3, KEY_PADDING_BOTTOM_3, ints);
            mOriginalPaddingTop3 = ints[0];
            mOriginalPaddingBottom3 = ints[1];
        }
    }
}
