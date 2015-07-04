/*
 * Copyright 2015 Hippo Seven
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
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hippo.conaco.Conaco;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.rippleold.RippleSalon;
import com.hippo.widget.SimpleGridLayout;

public final class PreviewLayout extends LinearLayout implements View.OnClickListener {

    private GestureDetector mGestureDetector;
    private PreviewSet[] mPreviewSets;
    private Conaco mConaco;
    private LayoutInflater mInflater;
    private int mPreviewSetCount;
    private int mCurrentPreviewIndex = -1;

    private SimpleGridLayout mSimpleGridLayout;
    private View mTopPrevious;
    private TextView mTopSelection;
    private View mTopNext;
    private View mBottomPrevious;
    private TextView mBottomSelection;
    private View mBottomNext;

    private int mWidth;

    public PreviewLayout(Context context) {
        super(context);
        init(context);
    }

    public PreviewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PreviewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(LinearLayout.VERTICAL);

        LayoutInflater.from(context).inflate(R.layout.widget_preview_layout, this);

        ViewGroup viewGroup = (ViewGroup) getChildAt(0);
        mTopPrevious = viewGroup.getChildAt(0);
        mTopSelection = (TextView) viewGroup.getChildAt(2);
        mTopNext = viewGroup.getChildAt(4);
        mSimpleGridLayout = (SimpleGridLayout) getChildAt(1);
        viewGroup = (ViewGroup) getChildAt(2);
        mBottomPrevious = viewGroup.getChildAt(0);
        mBottomSelection = (TextView) viewGroup.getChildAt(2);
        mBottomNext = viewGroup.getChildAt(4);

        RippleSalon.addRipple(mTopPrevious, false);
        RippleSalon.addRipple(mTopSelection, false);
        RippleSalon.addRipple(mTopNext, false);
        RippleSalon.addRipple(mBottomPrevious, false);
        RippleSalon.addRipple(mBottomSelection, false);
        RippleSalon.addRipple(mBottomNext, false);

        mTopPrevious.setOnClickListener(this);
        mTopSelection.setOnClickListener(this);
        mTopNext.setOnClickListener(this);
        mBottomPrevious.setOnClickListener(this);
        mBottomSelection.setOnClickListener(this);
        mBottomNext.setOnClickListener(this);

        GestureListener gestureListener = new GestureListener();
        mGestureDetector = new GestureDetector(context, gestureListener);
        mGestureDetector.setIsLongpressEnabled(false);
    }

    public void setData(PreviewSet[] previewSets, LayoutInflater inflater, Conaco conaco) {
        mPreviewSets = previewSets;
        mPreviewSetCount = previewSets.length;
        mInflater = inflater;
        mConaco = conaco;
    }

    private boolean isLargePreview() {
        if (mPreviewSets != null) {
            for (PreviewSet previewSet : mPreviewSets) {
                if (previewSet != null) {
                    return previewSet instanceof LargePreviewSet;
                }
            }
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int width = r - l;
        if (mWidth != width) {
            mWidth = width;
            int itemWidth = getResources().getDimensionPixelOffset(
                    isLargePreview() ? R.dimen.gallery_large_preview_item_width :
                            R.dimen.gallery_normal_preview_item_width);
            int columnCount = Math.round(
                    (width - getPaddingLeft() - getPaddingRight()) / (float) itemWidth);
            columnCount = Math.max(columnCount, 1);
            mSimpleGridLayout.setColumnCount(columnCount);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return handleTouchEvent(event);
    }

    private boolean handleTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    public int getPreviewPageCount() {
        return mPreviewSetCount;
    }

    public void selectPreviewAt(int index) {
        if (index < 0 || index >= mPreviewSetCount) {
            throw new IndexOutOfBoundsException("Invalid index " + index + ", size is " + mPreviewSetCount);
        }

        mCurrentPreviewIndex = index;

        PreviewSet previewSet = mPreviewSets[index];
        if (previewSet != null) {
            previewSet.bindView(mSimpleGridLayout, mInflater, mConaco);
        } else {
            // TODO
        }

        String showText = (index + 1) + "/" + mPreviewSetCount;
        mTopSelection.setText(showText);
        mBottomSelection.setText(showText);
    }

    public void tryNextPreview() {
        if (mCurrentPreviewIndex < mPreviewSetCount - 1) {
            selectPreviewAt(mCurrentPreviewIndex + 1);
        }
    }

    public void tryPreviousPreview() {
        if (mCurrentPreviewIndex > 0) {
            selectPreviewAt(mCurrentPreviewIndex - 1);
        }
    }

    @Override
    public void onClick(View v) {

    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private boolean mHasScrollX = false;

        @Override
        public boolean onDown(MotionEvent e) {
            mHasScrollX = false;
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            if (Math.abs(distanceX) > Math.abs(distanceY)) {
               if (!mHasScrollX) {
                   mHasScrollX = true;
                   if (distanceX > 0) {
                       tryNextPreview();
                   } else {
                       tryPreviousPreview();
                   }
               }
                return true;
            } else {
                return false;
            }
        }
    }
}
