/*
 * Copyright (c) 2013 Etsy
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

package com.etsy.android.grid;

import java.util.Arrays;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.ehviewer.R;

/**
 * A staggered grid view which supports multiple columns with rows of varying sizes.
 * <p/>
 * Builds multiple columns on top of {@link ExtendableListView}
 * <p/>
 * Partly inspired by - https://github.com/huewu/PinterestLikeAdapterView
 */
public class StaggeredGridView extends ExtendableListView {

    private static final String TAG = "StaggeredGridView";
    private static final boolean DBG = false;

    private static final int DEFAULT_COLUMNS_PORTRAIT = 2;
    private static final int DEFAULT_COLUMNS_LANDSCAPE = 3;

    private int mColumnCount;
    private int mItemMargin;
    private int mColumnWidth;
    private boolean mNeedSync;

    private int mColumnCountPortrait = DEFAULT_COLUMNS_PORTRAIT;
    private int mColumnCountLandscape = DEFAULT_COLUMNS_LANDSCAPE;

    /**
     * A key-value collection where the key is the position and the
     * {@link GridItemRecord} with some info about that position
     * so we can maintain it's position - and reorg on orientation change.
     */
    private SparseArray<GridItemRecord> mPositionData;
    private int mGridPaddingLeft;
    private int mGridPaddingRight;
    private int mGridPaddingTop;
    private int mGridPaddingBottom;

    /***
     * Our grid item state record with {@link Parcelable} implementation
     * so we can persist them across the SGV lifecycle.
     */
    static class GridItemRecord implements Parcelable {
        int column;
        double heightRatio;
        boolean isHeaderFooter;

        GridItemRecord() { }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private GridItemRecord(Parcel in) {
            column = in.readInt();
            heightRatio = in.readDouble();
            isHeaderFooter = in.readByte() == 1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(column);
            out.writeDouble(heightRatio);
            out.writeByte((byte) (isHeaderFooter ? 1 : 0));
        }

        @Override
        public String toString() {
            return "GridItemRecord.ListSavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " column:" + column
                    + " heightRatio:" + heightRatio
                    + " isHeaderFooter:" + isHeaderFooter
                    + "}";
        }

        public static final Parcelable.Creator<GridItemRecord> CREATOR
                = new Parcelable.Creator<GridItemRecord>() {
            @Override
            public GridItemRecord createFromParcel(Parcel in) {
                return new GridItemRecord(in);
            }

            @Override
            public GridItemRecord[] newArray(int size) {
                return new GridItemRecord[size];
            }
        };
    }

    /**
     * The location of the top of each top item added in each column.
     */
    private int[] mColumnTops;

    /**
     * The location of the bottom of each bottom item added in each column.
     */
    private int[] mColumnBottoms;

    /**
     * The left location to put items for each column
     */
    private int[] mColumnLefts;

    /***
     * Tells us the distance we've offset from the top.
     * Can be slightly off on orientation change - TESTING
     */
    private int mDistanceToTop;

    public StaggeredGridView(final Context context) {
        this(context, null);
    }

    public StaggeredGridView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaggeredGridView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            // get the number of columns in portrait and landscape
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StaggeredGridView, defStyle, 0);

            mColumnCount = typedArray.getInteger(
                    R.styleable.StaggeredGridView_column_count, 0);

            if (mColumnCount > 0) {
                mColumnCountPortrait = mColumnCount;
                mColumnCountLandscape = mColumnCount;
            }
            else {
                mColumnCountPortrait = typedArray.getInteger(
                        R.styleable.StaggeredGridView_column_count_portrait,
                        DEFAULT_COLUMNS_PORTRAIT);
                mColumnCountLandscape = typedArray.getInteger(
                        R.styleable.StaggeredGridView_column_count_landscape,
                        DEFAULT_COLUMNS_LANDSCAPE);
            }

            mItemMargin = typedArray.getDimensionPixelSize(
                    R.styleable.StaggeredGridView_item_margin, 0);
            mGridPaddingLeft = typedArray.getDimensionPixelSize(
                    R.styleable.StaggeredGridView_grid_paddingLeft, 0);
            mGridPaddingRight = typedArray.getDimensionPixelSize(
                    R.styleable.StaggeredGridView_grid_paddingRight, 0);
            mGridPaddingTop = typedArray.getDimensionPixelSize(
                    R.styleable.StaggeredGridView_grid_paddingTop, 0);
            mGridPaddingBottom = typedArray.getDimensionPixelSize(
                    R.styleable.StaggeredGridView_grid_paddingBottom, 0);

            typedArray.recycle();
        }

        mColumnCount = 0; // determined onMeasure
        // Creating these empty arrays to avoid saving null states
        mColumnTops = new int[0];
        mColumnBottoms = new int[0];
        mColumnLefts = new int[0];
        mPositionData = new SparseArray<GridItemRecord>();
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // PROPERTIES
    //

    // Grid padding is applied to the list item rows but not the header and footer
    public int getRowPaddingLeft() {
        return getListPaddingLeft() + mGridPaddingLeft;
    }

    public int getRowPaddingRight() {
        return getListPaddingRight() + mGridPaddingRight;
    }

    public int getRowPaddingTop() {
        return getListPaddingTop() + mGridPaddingTop;
    }

    public int getRowPaddingBottom() {
        return getListPaddingBottom() + mGridPaddingBottom;
    }

    public void setGridPadding(int left, int top, int right, int bottom) {
        mGridPaddingLeft = left;
        mGridPaddingTop = top;
        mGridPaddingRight = right;
        mGridPaddingBottom = bottom;
    }

    public void setColumnCountPortrait(int columnCountPortrait) {
        if (columnCountPortrait > 0 &&
                mColumnCountPortrait != columnCountPortrait) {
            mColumnCountPortrait = columnCountPortrait;
            if (!isLandscape() && getWidth() != 0) {
                onSizeChanged(getWidth(), getHeight());
                requestLayoutChildren();
            }
        }
    }

    public void setColumnCountLandscape(int columnCountLandscape) {
        if (columnCountLandscape > 0 &&
                mColumnCountLandscape != columnCountLandscape) {
            mColumnCountLandscape = columnCountLandscape;
            if (isLandscape() && getWidth() != 0) {
                onSizeChanged(getWidth(), getHeight());
                requestLayoutChildren();
            }
        }
    }

    public void setColumnCount(int columnCount) {
        if (columnCount > 0) {
            boolean needRequestLayout =
                    isLandscape() ? (mColumnCountLandscape != columnCount ? true : false) :
                        (mColumnCountPortrait != columnCount ? true : false);

            mColumnCountPortrait = columnCount;
            mColumnCountLandscape = columnCount;
            // mColumnCount set onSizeChanged();
            if (needRequestLayout && getWidth() != 0) {
                onSizeChanged(getWidth(), getHeight());
                requestLayoutChildren();
            }
        }
    }

    public void invalidateChildren() {
        onSizeChanged(getWidth(), getHeight());
        requestLayoutChildren();
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // MEASUREMENT
    //
    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mColumnCount <= 0) {
            boolean isLandscape = isLandscape();
            mColumnCount = isLandscape ? mColumnCountLandscape : mColumnCountPortrait;
        }

        // our column width is the width of the listview
        // minus it's padding
        // minus the total items margin
        // divided by the number of columns
        mColumnWidth = calculateColumnWidth(getMeasuredWidth());

        if (mColumnTops == null || mColumnTops.length != mColumnCount) {
            mColumnTops = new int[mColumnCount];
            initColumnTops();
        }
        if (mColumnBottoms == null || mColumnBottoms.length != mColumnCount) {
            mColumnBottoms = new int[mColumnCount];
            initColumnBottoms();
        }
        if (mColumnLefts == null || mColumnLefts.length != mColumnCount) {
            mColumnLefts = new int[mColumnCount];
            initColumnLefts();
        }
    }

    @Override
    protected void onMeasureChild(final View child, final LayoutParams layoutParams) {
        final int viewType = layoutParams.viewType;
        final int position = layoutParams.position;

        if (viewType == ITEM_VIEW_TYPE_HEADER_OR_FOOTER ||
                viewType == ITEM_VIEW_TYPE_IGNORE) {
            // for headers and weird ignored views
            super.onMeasureChild(child, layoutParams);
        }
        else {
            if (DBG) Log.d(TAG, "onMeasureChild BEFORE position:" + position +
                    " h:" + getMeasuredHeight());
            // measure it to the width of our column.
            int childWidthSpec = MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY);
            int childHeightSpec;
            if (layoutParams.height > 0) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY);
            }
            else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
            }
            child.measure(childWidthSpec, childHeightSpec);
        }

        final int childHeight = getChildHeight(child);
        setPositionHeightRatio(position, childHeight);

        if (DBG) Log.d(TAG, "onMeasureChild AFTER position:" + position +
                " h:" + childHeight);
    }

    public int getColumnWidth() {
        return mColumnWidth;
    }

    @Override
    public void resetToTop() {
        if (mColumnCount > 0) {

            if (mColumnTops == null) {
                mColumnTops = new int[mColumnCount];
            }
            if (mColumnBottoms == null) {
                mColumnBottoms = new int[mColumnCount];
            }
            initColumnTopsAndBottoms();

            mPositionData.clear();
            mNeedSync = false;
            mDistanceToTop = 0;
            setSelection(0);
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // POSITIONING
    //

    @Override
    protected void onChildCreated(final int position, final boolean flowDown) {
        super.onChildCreated(position, flowDown);
        if (!isHeaderOrFooter(position)) {
            // do we already have a column for this position?
            final int column = getChildColumn(position, flowDown);
            setPositionColumn(position, column);
            if (DBG) Log.d(TAG, "onChildCreated position:" + position +
                                " is in column:" + column);
        }
        else {
            setPositionIsHeaderFooter(position);
        }
    }

    private void requestLayoutChildren() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View v = getChildAt(i);
            if (v != null) v.requestLayout();
        }
    }

    @Override
    protected void layoutChildren() {
        preLayoutChildren();
        super.layoutChildren();
    }

    private void preLayoutChildren() {
        // on a major re-layout reset for our next layout pass
        if (!mNeedSync) {
            Arrays.fill(mColumnBottoms, 0);
        }
        else {
            mNeedSync = false;
        }
        // copy the tops into the bottom
        // since we're going to redo a layout pass that will draw down from
        // the top
        System.arraycopy(mColumnTops, 0, mColumnBottoms, 0, mColumnCount);
    }

    // NOTE : Views will either be layout out via onLayoutChild
    // OR
    // Views will be offset if they are active but offscreen so that we can recycle!
    // Both onLayoutChild() and onOffsetChild are called after we measure our view
    // see ExtensibleListView.setupChild();

    @Override
    protected void onLayoutChild(final View child,
                                 final int position,
                                 final boolean flowDown,
                                 final int childrenLeft, final int childTop,
                                 final int childRight, final int childBottom) {
        if (isHeaderOrFooter(position)) {
            layoutGridHeaderFooter(child, position, flowDown, childrenLeft, childTop, childRight, childBottom);
        }
        else {
            layoutGridChild(child, position, flowDown, childrenLeft, childRight);
        }
    }

    private void layoutGridHeaderFooter(final View child, final int position, final boolean flowDown, final int childrenLeft, final int childTop, final int childRight, final int childBottom) {
        // offset the top and bottom of all our columns
        // if it's the footer we want it below the lowest child bottom
        int gridChildTop;
        int gridChildBottom;

        if (flowDown) {
            gridChildTop = getLowestPositionedBottom();
            gridChildBottom = gridChildTop + getChildHeight(child);
        }
        else {
            gridChildBottom = getHighestPositionedTop();
            gridChildTop = gridChildBottom - getChildHeight(child);
        }

        for (int i = 0; i < mColumnCount; i++) {
            updateColumnTopIfNeeded(i, gridChildTop);
            updateColumnBottomIfNeeded(i, gridChildBottom);
        }

        super.onLayoutChild(child, position, flowDown,
                childrenLeft, gridChildTop, childRight, gridChildBottom);
    }

    private void layoutGridChild(final View child, final int position,
                                 final boolean flowDown,
                                 final int childrenLeft, final int childRight) {
        // stash the bottom and the top if it's higher positioned
        int column = getPositionColumn(position);

        int gridChildTop;
        int gridChildBottom;

        int childTopMargin = getChildTopMargin(position);
        int childBottomMargin = getChildBottomMargin();
        int verticalMargins = childTopMargin + childBottomMargin;

        if (flowDown) {
            gridChildTop = mColumnBottoms[column]; // the next items top is the last items bottom
            gridChildBottom = gridChildTop + (getChildHeight(child) + verticalMargins);
        }
        else {
            gridChildBottom = mColumnTops[column]; // the bottom of the next column up is our top
            gridChildTop = gridChildBottom - (getChildHeight(child) + verticalMargins);
        }

        if (DBG) Log.d(TAG, "onLayoutChild position:" + position +
                " column:" + column +
                " gridChildTop:" + gridChildTop +
                " gridChildBottom:" + gridChildBottom);

        // we also know the column of this view so let's stash it in the
        // view's layout params
        GridLayoutParams layoutParams = (GridLayoutParams) child.getLayoutParams();
        layoutParams.column = column;

        updateColumnBottomIfNeeded(column, gridChildBottom);
        updateColumnTopIfNeeded(column, gridChildTop);

        // subtract the margins before layout
        gridChildTop += childTopMargin;
        gridChildBottom -= childBottomMargin;

        child.layout(childrenLeft, gridChildTop, childRight, gridChildBottom);
    }

    @Override
    protected void onOffsetChild(final View child, final int position,
                                 final boolean flowDown, final int childrenLeft, final int childTop) {
        // if the child is recycled and is just offset
        // we still want to add its deets into our store
        if (isHeaderOrFooter(position)) {

            offsetGridHeaderFooter(child, position, flowDown, childrenLeft, childTop);
        }
        else {
            offsetGridChild(child, position, flowDown, childrenLeft, childTop);
        }
    }

    private void offsetGridHeaderFooter(final View child, final int position, final boolean flowDown, final int childrenLeft, final int childTop) {
        // offset the top and bottom of all our columns
        // if it's the footer we want it below the lowest child bottom
        int gridChildTop;
        int gridChildBottom;

        if (flowDown) {
            gridChildTop = getLowestPositionedBottom();
            gridChildBottom = gridChildTop + getChildHeight(child);
        }
        else {
            gridChildBottom = getHighestPositionedTop();
            gridChildTop = gridChildBottom - getChildHeight(child);
        }

        for (int i = 0; i < mColumnCount; i++) {
            updateColumnTopIfNeeded(i, gridChildTop);
            updateColumnBottomIfNeeded(i, gridChildBottom);
        }

        super.onOffsetChild(child, position, flowDown, childrenLeft, gridChildTop);
    }

    private void offsetGridChild(final View child, final int position, final boolean flowDown, final int childrenLeft, final int childTop) {
        // stash the bottom and the top if it's higher positioned
        int column = getPositionColumn(position);

        int gridChildTop;
        int gridChildBottom;

        int childTopMargin = getChildTopMargin(position);
        int childBottomMargin = getChildBottomMargin();
        int verticalMargins = childTopMargin + childBottomMargin;

        if (flowDown) {
            gridChildTop = mColumnBottoms[column]; // the next items top is the last items bottom
            gridChildBottom = gridChildTop + (getChildHeight(child) + verticalMargins);
        }
        else {
            gridChildBottom = mColumnTops[column]; // the bottom of the next column up is our top
            gridChildTop = gridChildBottom - (getChildHeight(child) + verticalMargins);
        }

        if (DBG) Log.d(TAG, "onOffsetChild position:" + position +
                " column:" + column +
                " childTop:" + childTop +
                " gridChildTop:" + gridChildTop +
                " gridChildBottom:" + gridChildBottom);

        // we also know the column of this view so let's stash it in the
        // view's layout params
        GridLayoutParams layoutParams = (GridLayoutParams) child.getLayoutParams();
        layoutParams.column = column;

        updateColumnBottomIfNeeded(column, gridChildBottom);
        updateColumnTopIfNeeded(column, gridChildTop);

        super.onOffsetChild(child, position, flowDown, childrenLeft, gridChildTop + childTopMargin);
    }

    private int getChildHeight(final View child) {
        return child.getMeasuredHeight();
    }

    private int getChildTopMargin(final int position) {
        boolean isFirstRow = position < (getHeaderViewsCount() + mColumnCount);
        return isFirstRow ? mItemMargin : 0;
    }

    private int getChildBottomMargin() {
        return mItemMargin;
    }

    @Override
    protected LayoutParams generateChildLayoutParams(final View child) {
        GridLayoutParams layoutParams = null;

        final ViewGroup.LayoutParams childParams = child.getLayoutParams();
        if (childParams != null) {
            if (childParams instanceof GridLayoutParams) {
                layoutParams = (GridLayoutParams) childParams;
            }
            else {
                layoutParams = new GridLayoutParams(childParams);
            }
        }
        if (layoutParams == null) {
            layoutParams = new GridLayoutParams(
                    mColumnWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return layoutParams;
    }

    private void updateColumnTopIfNeeded(int column, int childTop) {
        if (childTop < mColumnTops[column]) {
            mColumnTops[column] = childTop;
        }
    }

    private void updateColumnBottomIfNeeded(int column, int childBottom) {
        if (childBottom > mColumnBottoms[column]) {
            mColumnBottoms[column] = childBottom;
        }
    }

    @Override
    protected int getChildLeft(final int position) {
        if (isHeaderOrFooter(position)) {
            return super.getChildLeft(position);
        }
        else {
            final int column = getPositionColumn(position);
            return mColumnLefts[column];
        }
    }

    @Override
    protected int getChildTop(final int position) {
        if (isHeaderOrFooter(position)) {
            return super.getChildTop(position);
        }
        else {
            final int column = getPositionColumn(position);
            if (column == -1) {
                return getHighestPositionedBottom();
            }
            return mColumnBottoms[column];
        }
    }

    /**
     * Get the top for the next child down in our view
     * (maybe a column across) so we can fill down.
     */
    @Override
    protected int getNextChildDownsTop(final int position) {
        if (isHeaderOrFooter(position)) {
            return super.getNextChildDownsTop(position);
        }
        else {
            return getHighestPositionedBottom();
        }
    }

    @Override
    protected int getChildBottom(final int position) {
        if (isHeaderOrFooter(position)) {
            return super.getChildBottom(position);
        }
        else {
            final int column = getPositionColumn(position);
            if (column == -1) {
                return getLowestPositionedTop();
            }
            return mColumnTops[column];
        }
    }

    /**
     * Get the bottom for the next child up in our view
     * (maybe a column across) so we can fill up.
     */
    @Override
    protected int getNextChildUpsBottom(final int position) {
        if (isHeaderOrFooter(position)) {
            return super.getNextChildUpsBottom(position);
        }
        else {
            return getLowestPositionedTop();
        }
    }

    @Override
    protected int getLastChildBottom() {
        final int lastPosition = mFirstPosition + (getChildCount() - 1);
        if (isHeaderOrFooter(lastPosition)) {
            return super.getLastChildBottom();
        }
        return getHighestPositionedBottom();
    }

    @Override
    protected int getFirstChildTop() {
        if (isHeaderOrFooter(mFirstPosition)) {
            return super.getFirstChildTop();
        }
        return getLowestPositionedTop();
    }

    @Override
    protected int getHighestChildTop() {
        if (isHeaderOrFooter(mFirstPosition)) {
            return super.getHighestChildTop();
        }
        return getHighestPositionedTop();
    }

    @Override
    protected int getLowestChildBottom() {
        final int lastPosition = mFirstPosition + (getChildCount() - 1);
        if (isHeaderOrFooter(lastPosition)) {
            return super.getLowestChildBottom();
        }
        return getLowestPositionedBottom();
    }

    @Override
    protected void offsetChildrenTopAndBottom(final int offset) {
        super.offsetChildrenTopAndBottom(offset);
        offsetAllColumnsTopAndBottom(offset);
        offsetDistanceToTop(offset);
    }

    protected void offsetChildrenTopAndBottom(final int offset, final int column) {
        if (DBG) Log.d(TAG, "offsetChildrenTopAndBottom: " + offset + " column:" + column);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View v = getChildAt(i);
            if (v != null &&
                    v.getLayoutParams() != null &&
                    v.getLayoutParams() instanceof GridLayoutParams) {
                GridLayoutParams lp = (GridLayoutParams) v.getLayoutParams();
                if (lp.column == column) {
                    v.offsetTopAndBottom(offset);
                }
            }
        }
        offsetColumnTopAndBottom(offset, column);
    }

    private void offsetDistanceToTop(final int offset) {
        mDistanceToTop += offset;
        if (DBG) Log.d(TAG, "offset mDistanceToTop:" + mDistanceToTop);
    }

    public int getDistanceToTop() {
        return mDistanceToTop;
    }

    private void offsetAllColumnsTopAndBottom(final int offset) {
        if (offset != 0) {
            for (int i = 0; i < mColumnCount; i++) {
                offsetColumnTopAndBottom(offset, i);
            }
        }
    }

    private void offsetColumnTopAndBottom(final int offset, final int column) {
        if (offset != 0) {
            mColumnTops[column] += offset;
            mColumnBottoms[column] += offset;
        }
    }

    @Override
    protected void adjustViewsAfterFillGap(final boolean down) {
        super.adjustViewsAfterFillGap(down);
        // fix vertical gaps when hitting the top after a rotate
        // only when scrolling back up!
        if (!down) {
            alignTops();
        }
    }

    private void alignTops() {
        if (mFirstPosition == getHeaderViewsCount()) {
            // we're showing all the views before the header views
            int[] nonHeaderTops = getHighestNonHeaderTops();
            // we should now have our non header tops
            // align them
            boolean isAligned = true;
            int highestColumn = -1;
            int highestTop = Integer.MAX_VALUE;
            for (int i = 0; i < nonHeaderTops.length; i++) {
                // are they all aligned
                if (isAligned && i > 0 && nonHeaderTops[i] != highestTop) {
                    isAligned = false; // not all the tops are aligned
                }
                // what's the highest
                if (nonHeaderTops[i] < highestTop) {
                    highestTop = nonHeaderTops[i];
                    highestColumn = i;
                }
            }

            // skip the rest.
            if (isAligned) return;

            // we've got the highest column - lets align the others
            for (int i = 0; i < nonHeaderTops.length; i++) {
                if (i != highestColumn) {
                    // there's a gap in this column
                    int offset = highestTop - nonHeaderTops[i];
                    offsetChildrenTopAndBottom(offset, i);
                }
            }
            invalidate();
        }
    }

    private int[] getHighestNonHeaderTops() {
        int[] nonHeaderTops = new int[mColumnCount];
        int childCount = getChildCount();
        if (childCount > 0) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child != null &&
                        child.getLayoutParams() != null &&
                        child.getLayoutParams() instanceof GridLayoutParams) {
                    // is this child's top the highest non
                    GridLayoutParams lp = (GridLayoutParams) child.getLayoutParams();
                    // is it a child that isn't a header
                    if (lp.viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER &&
                            child.getTop() < nonHeaderTops[lp.column]) {
                        nonHeaderTops[lp.column] = child.getTop();
                    }
                }
            }
        }
        return nonHeaderTops;
    }

    @Override
    protected void onChildrenDetached(final int start, final int count) {
        super.onChildrenDetached(start, count);
        // go through our remaining views and sync the top and bottom stash.

        // Repair the top and bottom column boundaries from the views we still have
        Arrays.fill(mColumnTops, Integer.MAX_VALUE);
        Arrays.fill(mColumnBottoms, 0);

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child != null) {
                final LayoutParams childParams = (LayoutParams) child.getLayoutParams();
                if (childParams.viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER &&
                        childParams instanceof GridLayoutParams) {
                    GridLayoutParams layoutParams = (GridLayoutParams) childParams;
                    int column = layoutParams.column;
                    int position = layoutParams.position;
                    final int childTop = child.getTop();
                    if (childTop < mColumnTops[column]) {
                        mColumnTops[column] = childTop - getChildTopMargin(position);
                    }
                    final int childBottom = child.getBottom();
                    if (childBottom > mColumnBottoms[column]) {
                        mColumnBottoms[column] = childBottom + getChildBottomMargin();
                    }
                }
                else {
                    // the header and footer here
                    final int childTop = child.getTop();
                    final int childBottom = child.getBottom();

                    for (int col = 0; col < mColumnCount; col++) {
                        if (childTop < mColumnTops[col]) {
                            mColumnTops[col] = childTop;
                        }
                        if (childBottom > mColumnBottoms[col]) {
                            mColumnBottoms[col] = childBottom;
                        }
                    }

                }
            }
        }
    }

    @Override
    protected boolean hasSpaceUp() {
        int end = mClipToPadding ? getRowPaddingTop() : 0;
        return getLowestPositionedTop() > end;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // SYNCING ACROSS ROTATION
    //

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        onSizeChanged(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h) {
        super.onSizeChanged(w, h);
        boolean isLandscape = isLandscape();
        int newColumnCount = isLandscape ? mColumnCountLandscape : mColumnCountPortrait;
        if (mColumnCount != newColumnCount) {
            mColumnCount = newColumnCount;

            mColumnWidth = calculateColumnWidth(w);

            mColumnTops = new int[mColumnCount];
            mColumnBottoms = new int[mColumnCount];
            mColumnLefts = new int[mColumnCount];

            mDistanceToTop = 0;

            // rebuild the columns
            initColumnTopsAndBottoms();
            initColumnLefts();

            // if we have data
            if (getCount() > 0 && mPositionData.size() > 0) {
                onColumnSync();
            }

            requestLayout();
        }
    }

    private int calculateColumnWidth(final int gridWidth) {
        final int listPadding = getRowPaddingLeft() + getRowPaddingRight();
        return (gridWidth - listPadding - mItemMargin * (mColumnCount + 1)) / mColumnCount;
    }

    private int calculateColumnLeft(final int colIndex) {
        return getRowPaddingLeft() + mItemMargin + ((mItemMargin + mColumnWidth) * colIndex);
    }

    /***
     * Our mColumnTops and mColumnBottoms need to be re-built up to the
     * mSyncPosition - the following layout request will then
     * layout the that position and then fillUp and fillDown appropriately.
     */
    private void onColumnSync() {
        // re-calc tops for new column count!
        int syncPosition = Math.min(mSyncPosition, getCount() - 1);

        SparseArray<Double> positionHeightRatios = new SparseArray<Double>(syncPosition);
        for (int pos = 0; pos < syncPosition; pos++) {
            // check for weirdness
            final GridItemRecord rec = mPositionData.get(pos);
            if (rec == null) break;

            Log.d(TAG, "onColumnSync:" + pos + " ratio:" + rec.heightRatio);
            positionHeightRatios.append(pos, rec.heightRatio);
        }

        mPositionData.clear();

        // re-calc our relative position while at the same time
        // rebuilding our GridItemRecord collection

        if (DBG) Log.d(TAG, "onColumnSync column width:" + mColumnWidth);

        for (int pos = 0; pos < syncPosition; pos++) {
            //Check for weirdness again
            final Double heightRatio = positionHeightRatios.get(pos);
            if(heightRatio == null){
                break;
            }

            final GridItemRecord rec = getOrCreateRecord(pos);
            final int height = (int) (mColumnWidth * heightRatio);
            rec.heightRatio = heightRatio;

            int top;
            int bottom;
            // check for headers
            if (isHeaderOrFooter(pos)) {
                // the next top is the bottom for that column
                top = getLowestPositionedBottom();
                bottom = top + height;

                for (int i = 0; i < mColumnCount; i++) {
                    mColumnTops[i] = top;
                    mColumnBottoms[i] = bottom;
                }
            }
            else {
                // what's the next column down ?
                final int column = getHighestPositionedBottomColumn();
                // the next top is the bottom for that column
                top = mColumnBottoms[column];
                bottom = top + height + getChildTopMargin(pos) + getChildBottomMargin();

                mColumnTops[column] = top;
                mColumnBottoms[column] = bottom;

                rec.column = column;
            }


            if (DBG) Log.d(TAG, "onColumnSync position:" + pos +
                                " top:" + top +
                                " bottom:" + bottom +
                                " height:" + height +
                                " heightRatio:" + heightRatio);
        }

        // our sync position will be displayed in this column
        final int syncColumn = getHighestPositionedBottomColumn();
        setPositionColumn(syncPosition, syncColumn);

        // we want to offset from height of the sync position
        // minus the offset
        int syncToBottom = mColumnBottoms[syncColumn];
        int offset = -syncToBottom + mSpecificTop;
        // offset all columns by
        offsetAllColumnsTopAndBottom(offset);

        // sync the distance to top
        mDistanceToTop = -syncToBottom;

        // stash our bottoms in our tops - though these will be copied back to the bottoms
        System.arraycopy(mColumnBottoms, 0, mColumnTops, 0, mColumnCount);
    }


    // //////////////////////////////////////////////////////////////////////////////////////////
    // GridItemRecord UTILS
    //

    private void setPositionColumn(final int position, final int column) {
        GridItemRecord rec = getOrCreateRecord(position);
        rec.column = column;
    }

    private void setPositionHeightRatio(final int position, final int height) {
        GridItemRecord rec = getOrCreateRecord(position);
        rec.heightRatio = (double)  height / (double) mColumnWidth;
        if (DBG) Log.d(TAG, "position:" + position +
                            " width:" + mColumnWidth +
                            " height:" + height +
                            " heightRatio:" + rec.heightRatio);
    }

    private void setPositionIsHeaderFooter(final int position) {
        GridItemRecord rec = getOrCreateRecord(position);
        rec.isHeaderFooter = true;
    }

    private GridItemRecord getOrCreateRecord(final int position) {
        GridItemRecord rec = mPositionData.get(position, null);
        if (rec == null) {
            rec = new GridItemRecord();
            mPositionData.append(position, rec);
        }
        return rec;
    }

    private int getPositionColumn(final int position) {
        GridItemRecord rec = mPositionData.get(position, null);
        return rec != null ? rec.column : -1;
    }


    // //////////////////////////////////////////////////////////////////////////////////////////
    // HELPERS
    //

    private boolean isHeaderOrFooter(final int position) {
        final int viewType = mAdapter.getItemViewType(position);
        return viewType == ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
    }

    private int getChildColumn(final int position, final boolean flowDown) {

        // do we already have a column for this child position?
        int column = getPositionColumn(position);
        // we don't have the column or it no longer fits in our grid
        final int columnCount = mColumnCount;
        if (column < 0 || column >= columnCount) {
            // if we're going down -
            // get the highest positioned (lowest value)
            // column bottom
            if (flowDown) {
                column = getHighestPositionedBottomColumn();
            }
            else {
                column = getLowestPositionedTopColumn();

            }
        }
        return column;
    }

    private void initColumnTopsAndBottoms() {
        initColumnTops();
        initColumnBottoms();
    }

    private void initColumnTops() {
        Arrays.fill(mColumnTops, getPaddingTop() + mGridPaddingTop);
    }

    private void initColumnBottoms() {
        Arrays.fill(mColumnBottoms, getPaddingTop() + mGridPaddingTop);
    }

    private void initColumnLefts() {
        for (int i = 0; i < mColumnCount; i++) {
            mColumnLefts[i] = calculateColumnLeft(i);
        }
    }


    // //////////////////////////////////////////////////////////////////////////////////////////
    // BOTTOM
    //

    private int getHighestPositionedBottom() {
        final int column = getHighestPositionedBottomColumn();
        return mColumnBottoms[column];
    }

    private int getHighestPositionedBottomColumn() {
        int columnFound = 0;
        int highestPositionedBottom = Integer.MAX_VALUE;
        // the highest positioned bottom is the one with the lowest value :D
        for (int i = 0; i < mColumnCount; i++) {
            int bottom = mColumnBottoms[i];
            if (bottom < highestPositionedBottom) {
                highestPositionedBottom = bottom;
                columnFound = i;
            }
        }
        return columnFound;
    }

    private int getLowestPositionedBottom() {
        final int column = getLowestPositionedBottomColumn();
        return mColumnBottoms[column];
    }

    private int getLowestPositionedBottomColumn() {
        int columnFound = 0;
        int lowestPositionedBottom = Integer.MIN_VALUE;
        // the lowest positioned bottom is the one with the highest value :D
        for (int i = 0; i < mColumnCount; i++) {
            int bottom = mColumnBottoms[i];
            if (bottom > lowestPositionedBottom) {
                lowestPositionedBottom = bottom;
                columnFound = i;
            }
        }
        return columnFound;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // TOP
    //

    private int getLowestPositionedTop() {
        final int column = getLowestPositionedTopColumn();
        return mColumnTops[column];
    }

    private int getLowestPositionedTopColumn() {
        int columnFound = 0;
        // we'll go backwards through since the right most
        // will likely be the lowest positioned Top
        int lowestPositionedTop = Integer.MIN_VALUE;
        // the lowest positioned top is the one with the highest value :D
        for (int i = 0; i < mColumnCount; i++) {
            int top = mColumnTops[i];
            if (top > lowestPositionedTop) {
                lowestPositionedTop = top;
                columnFound = i;
            }
        }
        return columnFound;
    }

    private int getHighestPositionedTop() {
        final int column = getHighestPositionedTopColumn();
        return mColumnTops[column];
    }

    private int getHighestPositionedTopColumn() {
        int columnFound = 0;
        int highestPositionedTop = Integer.MAX_VALUE;
        // the highest positioned top is the one with the lowest value :D
        for (int i = 0; i < mColumnCount; i++) {
            int top = mColumnTops[i];
            if (top < highestPositionedTop) {
                highestPositionedTop = top;
                columnFound = i;
            }
        }
        return columnFound;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // LAYOUT PARAMS
    //

    /**
     * Extended LayoutParams to column position and anything else we may been for the grid
     */
    public static class GridLayoutParams extends LayoutParams {

        // The column the view is displayed in
        int column;

        public GridLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            enforceStaggeredLayout();
        }

        public GridLayoutParams(int w, int h) {
            super(w, h);
            enforceStaggeredLayout();
        }

        public GridLayoutParams(int w, int h, int viewType) {
            super(w, h);
            enforceStaggeredLayout();
        }

        public GridLayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            enforceStaggeredLayout();
        }

        /**
         * Here we're making sure that all grid view items
         * are width MATCH_PARENT and height WRAP_CONTENT.
         * That's what this grid is designed for
         */
        private void enforceStaggeredLayout() {
            if (width != MATCH_PARENT) {
                width = MATCH_PARENT;
            }
            if (height == MATCH_PARENT) {
                height = WRAP_CONTENT;
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // SAVED STATE


    public static class GridListSavedState extends ListSavedState {
        int columnCount;
        int[] columnTops;
        SparseArray positionData;

        public GridListSavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        public GridListSavedState(Parcel in) {
            super(in);
            columnCount = in.readInt();
            columnTops = new int[columnCount >= 0 ? columnCount : 0];
            in.readIntArray(columnTops);
            positionData = in.readSparseArray(GridItemRecord.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(columnCount);
            out.writeIntArray(columnTops);
            out.writeSparseArray(positionData);
        }

        @Override
        public String toString() {
            return "StaggeredGridView.GridListSavedState{"
                    + Integer.toHexString(System.identityHashCode(this)) + "}";
        }

        public static final Creator<GridListSavedState> CREATOR
                = new Creator<GridListSavedState>() {
            @Override
            public GridListSavedState createFromParcel(Parcel in) {
                return new GridListSavedState(in);
            }

            @Override
            public GridListSavedState[] newArray(int size) {
                return new GridListSavedState[size];
            }
        };
    }


    @Override
    public Parcelable onSaveInstanceState() {
        ListSavedState listState = (ListSavedState) super.onSaveInstanceState();
        GridListSavedState ss = new GridListSavedState(listState.getSuperState());

        // from the list state
        ss.selectedId = listState.selectedId;
        ss.firstId = listState.firstId;
        ss.viewTop = listState.viewTop;
        ss.position = listState.position;
        ss.height = listState.height;

        // our state

        boolean haveChildren = getChildCount() > 0 && getCount() > 0;

        if (haveChildren && mFirstPosition > 0) {
            ss.columnCount = mColumnCount;
            ss.columnTops = mColumnTops;
            ss.positionData = mPositionData;
        }
        else {
            ss.columnCount = mColumnCount >= 0 ? mColumnCount : 0;
            ss.columnTops = new int[ss.columnCount];
            ss.positionData = new SparseArray<Object>();
        }

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        GridListSavedState ss = (GridListSavedState) state;
        mColumnCount = ss.columnCount;
        mColumnTops = ss.columnTops;
        mColumnBottoms = new int[mColumnCount];
        mPositionData = ss.positionData;
        mNeedSync = true;
        super.onRestoreInstanceState(ss);
    }
}
