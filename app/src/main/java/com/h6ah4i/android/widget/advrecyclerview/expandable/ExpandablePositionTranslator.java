/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.widget.advrecyclerview.expandable;

import android.support.v7.widget.RecyclerView;

import java.util.Arrays;

class ExpandablePositionTranslator {
    private final int ALLOCATE_UNIT = 256;

    private final static long FLAG_EXPANDED = 0x0000000080000000l;
    private final static long LOWER_31BIT_MASK = 0x000000007fffffffl;
    private final static long LOWER_32BIT_MASK = 0x00000000ffffffffl;
    private final static long UPPER_32BIT_MASK = 0xffffffff00000000l;

    /*
     * bit 64-32: offset  (use for caching purpose)
     * bit 31:    expanded or not
     * bit 30-0:  child count
     */
    private long[] mCachedGroupPosInfo;

    /*
     * bit 31: reserved
     * bit 30-0: group id
     */
    private int[] mCachedGroupId;
    private int mGroupCount;
    private int mExpandedGroupCount;
    private int mExpandedChildCount;
    private int mEndOfCalculatedOffsetGroupPosition = RecyclerView.NO_POSITION;
    private ExpandableItemAdapter mAdapter;

    public ExpandablePositionTranslator() {
    }

    public void build(ExpandableItemAdapter adapter) {
        final int groupCount = adapter.getGroupCount();

        enlargeArraysIfNeeded(groupCount, false);

        final long[] info = mCachedGroupPosInfo;
        final int[] ids = mCachedGroupId;
        for (int i = 0; i < groupCount; i++) {
            final long groupId = adapter.getGroupId(i);
            final int childCount = adapter.getChildCount(i);

            info[i] = (((long) i << 32) | childCount);
            ids[i] = (int) (groupId & LOWER_32BIT_MASK);
        }

        mAdapter = adapter;
        mGroupCount = groupCount;
        mExpandedGroupCount = 0;
        mExpandedChildCount = 0;
        mEndOfCalculatedOffsetGroupPosition = Math.max(0, groupCount - 1);
    }

    public void restoreExpandedGroupItems(
            int[] restoreGroupIds,
            ExpandableItemAdapter adapter,
            RecyclerViewExpandableItemManager.OnGroupExpandListener expandListener,
            RecyclerViewExpandableItemManager.OnGroupCollapseListener collapseListener) {
        if (restoreGroupIds == null || restoreGroupIds.length == 0) {
            return;
        }

        if (mCachedGroupPosInfo == null) {
            return;
        }

        // make ID + position packed array
        final long[] idAndPos = new long[mGroupCount];

        for (int i = 0; i < mGroupCount; i++) {
            idAndPos[i] = ((long) mCachedGroupId[i] << 32) | i;
        }

        // sort both arrays
        Arrays.sort(idAndPos);

        final boolean fromUser = false;

        // find matched items & apply
        int index = 0;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < restoreGroupIds.length; i++) {
            final int id1 = restoreGroupIds[i];

            for (int j = index; j < idAndPos.length; j++) {
                final int id2 = (int) (idAndPos[j] >> 32);
                final int position = (int) (idAndPos[j] & LOWER_31BIT_MASK);

                if (id2 < id1) {
                    index = j;

                    if (adapter == null || adapter.onHookGroupCollapse(position, fromUser)) {
                        if (collapseGroup(position)) {
                            if (collapseListener != null) {
                                collapseListener.onGroupCollapse(position, fromUser);
                            }
                        }
                    }
                } else if (id2 == id1) {
                    // matched
                    index = j + 1;

                    if (adapter == null || adapter.onHookGroupExpand(position, fromUser)) {
                        if (expandGroup(position)) {
                            if (expandListener != null) {
                                expandListener.onGroupExpand(position, fromUser);
                            }
                        }
                    }
                } else { // id2 > id1
                    break;
                }
            }
        }

        if (adapter != null || collapseListener != null) {
            for (int i = index; i < idAndPos.length; i++) {
                final int id2 = (int) (idAndPos[i] >> 32);
                final int position = (int) (idAndPos[i] & LOWER_31BIT_MASK);

                if (adapter == null || adapter.onHookGroupCollapse(position, fromUser)) {
                    if (collapseGroup(position)) {
                        if (collapseListener != null) {
                            collapseListener.onGroupCollapse(position, fromUser);
                        }
                    }
                }
            }
        }
    }

    public int[] getSavedStateArray() {
        int[] expandedGroups = new int[mExpandedGroupCount];

        int index = 0;
        for (int i = 0; i < mGroupCount; i++) {
            final long t = mCachedGroupPosInfo[i];
            if ((t & FLAG_EXPANDED) != 0) {
                expandedGroups[index] = mCachedGroupId[i];
                index += 1;
            }
        }

        if (index != mExpandedGroupCount) {
            throw new IllegalStateException("may be a bug  (index = " + index + ", mExpandedGroupCount = " + mExpandedGroupCount + ")");
        }

        Arrays.sort(expandedGroups);

        return expandedGroups;
    }

    public int getItemCount() {
        return mGroupCount + mExpandedChildCount;
    }

    public boolean isGroupExpanded(int groupPosition) {
        return ((mCachedGroupPosInfo[groupPosition] & FLAG_EXPANDED) != 0);
    }

    public int getChildCount(int groupPosition) {
        return (int) (mCachedGroupPosInfo[groupPosition] & LOWER_31BIT_MASK);
    }

    public int getVisibleChildCount(int groupPosition) {
        if (isGroupExpanded(groupPosition)) {
            return getChildCount(groupPosition);
        } else {
            return 0;
        }
    }

    public boolean collapseGroup(int groupPosition) {
        if ((mCachedGroupPosInfo[groupPosition] & FLAG_EXPANDED) == 0) {
            return false;
        }

        final int childCount = (int) (mCachedGroupPosInfo[groupPosition] & LOWER_31BIT_MASK);

        mCachedGroupPosInfo[groupPosition] &= (~FLAG_EXPANDED);
        mExpandedGroupCount -= 1;

        mExpandedChildCount -= childCount;
        mEndOfCalculatedOffsetGroupPosition = Math.min(mEndOfCalculatedOffsetGroupPosition, groupPosition);

        // requires notifyItemRangeRemoved()
        return true;
    }

    public boolean expandGroup(int groupPosition) {
        if ((mCachedGroupPosInfo[groupPosition] & FLAG_EXPANDED) != 0) {
            return false;
        }

        final int childCount = (int) (mCachedGroupPosInfo[groupPosition] & LOWER_31BIT_MASK);

        mCachedGroupPosInfo[groupPosition] |= FLAG_EXPANDED;
        mExpandedGroupCount += 1;

        mExpandedChildCount += childCount;
        mEndOfCalculatedOffsetGroupPosition = Math.min(mEndOfCalculatedOffsetGroupPosition, groupPosition);

        // requires notifyItemRangeInserted()
        return true;
    }

    public void moveGroupItem(int fromGroupPosition, int toGroupPosition) {
        if (fromGroupPosition == toGroupPosition) {
            return;
        }

        final long tmp1 = mCachedGroupPosInfo[fromGroupPosition];
        final int tmp2 = mCachedGroupId[fromGroupPosition];

        if (toGroupPosition < fromGroupPosition) {
            // shift to backward
            for (int i = fromGroupPosition; i > toGroupPosition; i--) {
                mCachedGroupPosInfo[i] = mCachedGroupPosInfo[i - 1];
                mCachedGroupId[i] = mCachedGroupId[i - 1];
            }
        } else {
            // shift to forward
            for (int i = fromGroupPosition; i < toGroupPosition; i++) {
                mCachedGroupPosInfo[i] = mCachedGroupPosInfo[i + 1];
                mCachedGroupId[i] = mCachedGroupId[i + 1];
            }
        }

        mCachedGroupPosInfo[toGroupPosition] = tmp1;
        mCachedGroupId[toGroupPosition] = tmp2;

        final int minPosition = Math.min(fromGroupPosition, toGroupPosition);

        if (minPosition > 0) {
            mEndOfCalculatedOffsetGroupPosition = Math.min(mEndOfCalculatedOffsetGroupPosition, minPosition - 1);
        } else {
            mEndOfCalculatedOffsetGroupPosition = RecyclerView.NO_POSITION;
        }
    }

    public void moveChildItem(int fromGroupPosition, int fromChildPosition, int toGroupPosition, int toChildPosition) {
        if (fromGroupPosition == toGroupPosition) {
            return;
        }

        final int fromChildCount = (int) (mCachedGroupPosInfo[fromGroupPosition] & LOWER_31BIT_MASK);
        final int toChildCount = (int) (mCachedGroupPosInfo[toGroupPosition] & LOWER_31BIT_MASK);

        if (fromChildCount == 0) {
            throw new IllegalStateException("moveChildItem(" +
                    "fromGroupPosition = " + fromGroupPosition +
                    ", fromChildPosition = " + fromChildPosition +
                    ", toGroupPosition = " + toGroupPosition +
                    ", toChildPosition = " + toChildPosition + ")  --- may be a bug.");
        }

        mCachedGroupPosInfo[fromGroupPosition] = (mCachedGroupPosInfo[fromGroupPosition] & (UPPER_32BIT_MASK | FLAG_EXPANDED)) | (fromChildCount - 1);
        mCachedGroupPosInfo[toGroupPosition] = (mCachedGroupPosInfo[toGroupPosition] & (UPPER_32BIT_MASK | FLAG_EXPANDED)) | (toChildCount + 1);

        if ((mCachedGroupPosInfo[fromGroupPosition] & FLAG_EXPANDED) != 0) {
            mExpandedChildCount -= 1;
        }
        if ((mCachedGroupPosInfo[toGroupPosition] & FLAG_EXPANDED) != 0) {
            mExpandedChildCount += 1;
        }

        final int minPosition = Math.min(fromGroupPosition, toGroupPosition);

        if (minPosition > 0) {
            mEndOfCalculatedOffsetGroupPosition = Math.min(mEndOfCalculatedOffsetGroupPosition, minPosition - 1);
        } else {
            mEndOfCalculatedOffsetGroupPosition = RecyclerView.NO_POSITION;
        }
    }

    public long getExpandablePosition(int flatPosition) {
        if (flatPosition == RecyclerView.NO_POSITION) {
            return ExpandableAdapterHelper.NO_EXPANDABLE_POSITION;
        }

        final int groupCount = mGroupCount;

        // final int startIndex = 0;
        final int startIndex = binarySearchGroupPositionByFlatPosition(mCachedGroupPosInfo, mEndOfCalculatedOffsetGroupPosition, flatPosition);
        long expandablePosition = ExpandableAdapterHelper.NO_EXPANDABLE_POSITION;
        int endOfCalculatedOffsetGroupPosition = mEndOfCalculatedOffsetGroupPosition;
        int offset = (startIndex == 0) ? 0 : (int) (mCachedGroupPosInfo[startIndex] >>> 32);

        for (int i = startIndex; i < groupCount; i++) {
            final long t = mCachedGroupPosInfo[i];

            // update offset info
            mCachedGroupPosInfo[i] = (((long) offset << 32) | (t & LOWER_32BIT_MASK));
            endOfCalculatedOffsetGroupPosition = i;

            if (offset >= flatPosition) {
                // found (group item)
                expandablePosition = ExpandableAdapterHelper.getPackedPositionForGroup(i);
                break;
            } else {
                offset += 1;
            }

            if ((t & FLAG_EXPANDED) != 0) {
                final int childCount = (int) (t & LOWER_31BIT_MASK);

                if ((childCount > 0) && (offset + childCount - 1) >= flatPosition) {
                    // found (child item)
                    expandablePosition = ExpandableAdapterHelper.getPackedPositionForChild(i, (flatPosition - offset));
                    break;
                } else {
                    offset += childCount;
                }
            }
        }

        mEndOfCalculatedOffsetGroupPosition = Math.max(mEndOfCalculatedOffsetGroupPosition, endOfCalculatedOffsetGroupPosition);

        return expandablePosition;
    }

    public int getFlatPosition(long packedPosition) {
        if (packedPosition == ExpandableAdapterHelper.NO_EXPANDABLE_POSITION) {
            return RecyclerView.NO_POSITION;
        }

        final int groupPosition = ExpandableAdapterHelper.getPackedPositionGroup(packedPosition);
        final int childPosition = ExpandableAdapterHelper.getPackedPositionChild(packedPosition);
        final int groupCount = mGroupCount;

        if (!(groupPosition >= 0 && groupPosition < groupCount)) {
            return RecyclerView.NO_POSITION;
        }

        if (childPosition != RecyclerView.NO_POSITION) {
            if (!isGroupExpanded(groupPosition)) {
                return RecyclerView.NO_POSITION;
            }
        }

        // final int startIndex = 0;
        final int startIndex = Math.max(0, Math.min(groupPosition, mEndOfCalculatedOffsetGroupPosition));
        int endOfCalculatedOffsetGroupPosition = mEndOfCalculatedOffsetGroupPosition;
        int offset = (int) (mCachedGroupPosInfo[startIndex] >>> 32);
        int flatPosition = RecyclerView.NO_POSITION;

        for (int i = startIndex; i < groupCount; i++) {
            final long t = mCachedGroupPosInfo[i];

            // update offset info
            mCachedGroupPosInfo[i] = (((long) offset << 32) | (t & LOWER_32BIT_MASK));
            endOfCalculatedOffsetGroupPosition = i;

            final int childCount = (int) (t & LOWER_31BIT_MASK);

            if (i == groupPosition) {
                if (childPosition == RecyclerView.NO_POSITION) {
                    flatPosition = offset;
                } else if (childPosition < childCount) {
                    flatPosition = (offset + 1) + childPosition;
                }
                break;
            } else {
                offset += 1;

                if ((t & FLAG_EXPANDED) != 0) {
                    offset += childCount;
                }
            }
        }

        mEndOfCalculatedOffsetGroupPosition = Math.max(mEndOfCalculatedOffsetGroupPosition, endOfCalculatedOffsetGroupPosition);

        return flatPosition;
    }


    private static int binarySearchGroupPositionByFlatPosition(long[] array, int endArrayPosition, int flatPosition) {
        if (endArrayPosition <= 0) {
            return 0;
        }

        final int v1 = (int) (array[0] >>> 32);
        final int v2 = (int) (array[endArrayPosition] >>> 32);

        if (flatPosition <= v1) {
            return 0;
        } else if (flatPosition >= v2) {
            return endArrayPosition;
        }

        int lastS = 0;
        int s = 0;
        int e = endArrayPosition;

        while (s < e) {
            final int mid = (s + e) >>> 1;
            final int v = (int) (array[mid] >>> 32);

            if (v < flatPosition) {
                lastS = s;
                s = mid + 1;
            } else {
                e = mid;
            }
        }

        return lastS;
    }

    public void removeChildItem(int groupPosition, int childPosition) {
        removeChildItems(groupPosition, childPosition, 1);
    }

    public void removeChildItems(int groupPosition, int childPositionStart, int count) {
        final long t = mCachedGroupPosInfo[groupPosition];
        final int curCount = (int) (t & LOWER_31BIT_MASK);

        if (!((childPositionStart >= 0) && ((childPositionStart + count) <= curCount))) {
            throw new IllegalStateException(
                    "Invalid child position " +
                            "removeChildItems(groupPosition = " + groupPosition + ", childPosition = " + childPositionStart + ", count = " + count + ")");
        }

        if ((t & FLAG_EXPANDED) != 0) {
            mExpandedChildCount -= count;
        }

        mCachedGroupPosInfo[groupPosition] = (t & (UPPER_32BIT_MASK | FLAG_EXPANDED)) | (curCount - count);
        mEndOfCalculatedOffsetGroupPosition = Math.min(mEndOfCalculatedOffsetGroupPosition, groupPosition - 1);
    }

    public void insertChildItem(int groupPosition, int childPosition) {
        insertChildItems(groupPosition, childPosition, 1);
    }

    public void insertChildItems(int groupPosition, int childPositionStart, int count) {
        final long t = mCachedGroupPosInfo[groupPosition];
        final int curCount = (int) (t & LOWER_31BIT_MASK);

        if (!((childPositionStart >= 0) && (childPositionStart <= curCount))) {
            throw new IllegalStateException(
                    "Invalid child position " +
                            "insertChildItems(groupPosition = " + groupPosition + ", childPositionStart = " + childPositionStart + ", count = " + count + ")");
        }

        if ((t & FLAG_EXPANDED) != 0) {
            mExpandedChildCount += count;
        }

        mCachedGroupPosInfo[groupPosition] = (t & (UPPER_32BIT_MASK | FLAG_EXPANDED)) | (curCount + count);
        mEndOfCalculatedOffsetGroupPosition = Math.min(mEndOfCalculatedOffsetGroupPosition, groupPosition);
    }

    public int insertGroupItems(int groupPosition, int count) {
        if (count <= 0) {
            return 0;
        }

        final int n = count;

        enlargeArraysIfNeeded(mGroupCount + n, true);

        // shift to backward
        final ExpandableItemAdapter adapter = mAdapter;
        final long[] info = mCachedGroupPosInfo;
        final int[] ids = mCachedGroupId;

        int start = mGroupCount - 1 + n;
        int end = groupPosition - 1 + n;
        for (int i = start; i > end; i--) {
            info[i] = info[i - n];
            ids[i] = ids[i - n];
        }

        int end2 = groupPosition + n;
        for (int i = groupPosition; i < end2; i++) {
            final long groupId = adapter.getGroupId(i);
            final int childCount = adapter.getChildCount(i);

            info[i] = (((long) i << 32) | childCount);
            ids[i] = (int) (groupId & LOWER_32BIT_MASK);
        }

        mGroupCount += n;

        int calculatedOffset = (mGroupCount == 0) ? RecyclerView.NO_POSITION : (groupPosition - 1);
        mEndOfCalculatedOffsetGroupPosition = Math.min(mEndOfCalculatedOffsetGroupPosition, calculatedOffset);

        return n;
    }

    public int insertGroupItem(int groupPosition) {
        return insertGroupItems(groupPosition, 1);
    }

    public int removeGroupItems(int groupPosition, int count) {
        if (count <= 0) {
            return 0;
        }

        final int n = count;
        int removedVisibleItemCount = 0;

        for (int i = 0; i < n; i++) {
            final long t = mCachedGroupPosInfo[groupPosition + i];

            if ((t & FLAG_EXPANDED) != 0) {
                int visibleChildCount = (int) (t & LOWER_31BIT_MASK);
                removedVisibleItemCount += visibleChildCount;
                mExpandedChildCount -= visibleChildCount;
                mExpandedGroupCount -= 1;
            }
        }
        removedVisibleItemCount += n;
        mGroupCount -= n;

        // shift to forward
        for (int i = groupPosition; i < mGroupCount; i++) {
            mCachedGroupPosInfo[i] = mCachedGroupPosInfo[i + n];
            mCachedGroupId[i] = mCachedGroupId[i + n];
        }

        int calculatedOffset = (mGroupCount == 0) ? RecyclerView.NO_POSITION : (groupPosition - 1);
        mEndOfCalculatedOffsetGroupPosition = Math.min(mEndOfCalculatedOffsetGroupPosition, calculatedOffset);

        return removedVisibleItemCount;
    }

    public int removeGroupItem(int groupPosition) {
        return removeGroupItems(groupPosition, 1);
    }


    private void enlargeArraysIfNeeded(int size, boolean preserveData) {
        int allocSize = (size + (2 * ALLOCATE_UNIT - 1)) & ~(ALLOCATE_UNIT - 1);

        long[] curInfo = mCachedGroupPosInfo;
        int[] curId = mCachedGroupId;
        long[] newInfo = curInfo;
        int[] newId = curId;

        if (curInfo == null || curInfo.length < size) {
            newInfo = new long[allocSize];
        }
        if (curId == null || curId.length < size) {
            newId = new int[allocSize];
        }

        if (preserveData) {
            if (curInfo != null && curInfo != newInfo) {
                System.arraycopy(curInfo, 0, newInfo, 0, curInfo.length);
            }
            if (curId != null && curId != newId) {
                System.arraycopy(curId, 0, newId, 0, curId.length);
            }
        }

        mCachedGroupPosInfo = newInfo;
        mCachedGroupId = newId;
    }
}
