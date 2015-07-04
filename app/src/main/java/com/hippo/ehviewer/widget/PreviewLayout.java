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
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.rippleold.RippleSalon;
import com.hippo.util.ViewUtils;
import com.hippo.widget.AccurateClick;
import com.hippo.widget.SimpleGridLayout;

public final class PreviewLayout extends LinearLayout implements View.OnClickListener,
        AccurateClick.OnAccurateClickListener {

    private GestureDetector mGestureDetector;
    private PreviewSet[] mPreviewSets;
    private int mGid;
    private Conaco mConaco;
    private LayoutInflater mInflater;
    private int mPreviewSetCount;
    private int mCurrentPreviewPage = -1;

    private SimpleGridLayout mSimpleGridLayout;
    private View mProgressBar;
    private View mTopPrevious;
    private TextView mTopSelection;
    private View mTopNext;
    private View mBottomPrevious;
    private TextView mBottomSelection;
    private View mBottomNext;

    private ViewTransition mViewTransition;

    private int mWidth;
    private int mPreviewSetSize = -1;

    private PreviewHelper mPreviewHelper;

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
        mProgressBar = getChildAt(2);
        viewGroup = (ViewGroup) getChildAt(3);
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
        mTopNext.setOnClickListener(this);
        mBottomPrevious.setOnClickListener(this);
        mBottomNext.setOnClickListener(this);

        AccurateClick.setOnAccurateClickListener(mTopSelection, this);
        AccurateClick.setOnAccurateClickListener(mBottomSelection, this);

        GestureListener gestureListener = new GestureListener();
        mGestureDetector = new GestureDetector(context, gestureListener);
        mGestureDetector.setIsLongpressEnabled(false);

        mViewTransition = new ViewTransition(mSimpleGridLayout, mProgressBar);
    }

    public void setData(PreviewSet[] previewSets, int gid, LayoutInflater inflater, Conaco conaco) {
        mPreviewSets = previewSets;
        mPreviewSetCount = previewSets.length;
        mGid = gid;
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

    public void setPreviewHelper(PreviewHelper previewHelper) {
        mPreviewHelper = previewHelper;
    }

    public int getPreviewPageCount() {
        return mPreviewSetCount;
    }

    public int getPreviewSetStartIndex(int page) {
        if (mPreviewSetSize == -1) {
            for (PreviewSet previewSet : mPreviewSets) {
                if (previewSet != null) {
                    mPreviewSetSize = previewSet.size();
                    break;
                }
            }
        }

        if (mPreviewSetSize == -1) {
            return 0;
        } else {
            return mPreviewSetSize * page;
        }
    }

    public void onGetPreview(PreviewSet previewSet, int page) {
        previewSet.setStartIndex(getPreviewSetStartIndex(page));
        previewSet.setGid(mGid);
        mPreviewSets[page] = previewSet;

        if (mCurrentPreviewPage == page) {
            mViewTransition.showView(0);

            previewSet.bindView(mSimpleGridLayout, mInflater, mConaco);
        }
    }

    public void selectPreviewAt(int page) {
        if (page < 0 || page >= mPreviewSetCount) {
            throw new IndexOutOfBoundsException("Invalid page " + page + ", size is " + mPreviewSetCount);
        }

        int oldPage = mCurrentPreviewPage;
        mCurrentPreviewPage = page;

        if (oldPage != -1) {
            PreviewSet oldPreviewSet = mPreviewSets[oldPage];
            if (oldPreviewSet != null) {
                oldPreviewSet.cancelLoadTask(mSimpleGridLayout, mConaco);
            }
        }

        PreviewSet previewSet = mPreviewSets[page];
        if (previewSet != null) {
            mViewTransition.showView(0);
            previewSet.bindView(mSimpleGridLayout, mInflater, mConaco);
        } else {
            mViewTransition.showView(1);
            mPreviewHelper.onRequstPreview(this, page);
        }

        String showText = (page + 1) + "/" + mPreviewSetCount;
        mTopSelection.setText(showText);
        mBottomSelection.setText(showText);
    }

    public void tryNextPreview() {
        if (mCurrentPreviewPage < mPreviewSetCount - 1) {
            selectPreviewAt(mCurrentPreviewPage + 1);
        }
    }

    public void tryPreviousPreview() {
        if (mCurrentPreviewPage > 0) {
            selectPreviewAt(mCurrentPreviewPage - 1);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mTopPrevious || v == mBottomPrevious) {
            tryPreviousPreview();
        } else if (v == mTopNext || v == mBottomNext) {
            tryNextPreview();
        }
    }

    @Override
    public void onAccurateClick(View v, float x, float y) {
        if (v == mTopSelection || v == mBottomSelection) {
            int[] position = new int[2];
            ViewUtils.getLocationInAncestor(v, position, this);
            mPreviewHelper.onRequstPreviewIndex(this, x + position[0], y + position[1]);
        }
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

    public interface PreviewHelper {

        void onRequstPreview(PreviewLayout previewLayout, int index);

        void onRequstPreviewIndex(PreviewLayout previewLayout, float x, float y);
    }
}
