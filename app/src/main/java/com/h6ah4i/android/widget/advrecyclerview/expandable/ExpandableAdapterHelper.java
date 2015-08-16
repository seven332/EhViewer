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

class ExpandableAdapterHelper {
    public static final long NO_EXPANDABLE_POSITION = 0xffffffffffffffffl;

    private static final long LOWER_32BIT_MASK = 0x00000000ffffffffl;
    private static final long LOWER_31BIT_MASK = 0x000000007fffffffl;

    /*package*/ static final int VIEW_TYPE_FLAG_IS_GROUP = 0x80000000;

    public static long getPackedPositionForChild(int groupPosition, int childPosition) {
        return ((long) childPosition << 32) | (groupPosition & LOWER_32BIT_MASK);
    }

    public static long getPackedPositionForGroup(int groupPosition) {
        return ((long) RecyclerView.NO_POSITION << 32) | (groupPosition & LOWER_32BIT_MASK);
    }

    public static int getPackedPositionChild(long packedPosition) {
        return (int) (packedPosition >>> 32);
    }

    public static int getPackedPositionGroup(long packedPosition) {
        return (int) (packedPosition & LOWER_32BIT_MASK);
    }

    public static long getCombinedChildId(long groupId, long childId) {
        return ((groupId & LOWER_31BIT_MASK) << 32) | (childId & LOWER_32BIT_MASK);
    }

    public static long getCombinedGroupId(long groupId) {
        //noinspection PointlessBitwiseExpression
        return ((groupId & LOWER_31BIT_MASK) << 32) | (RecyclerView.NO_ID & LOWER_32BIT_MASK);
    }

    public static boolean isGroupViewType(int rawViewType) {
        return ((rawViewType & VIEW_TYPE_FLAG_IS_GROUP) != 0);
    }

    public static int getGroupViewType(int rawViewType) {
        return (rawViewType & (~VIEW_TYPE_FLAG_IS_GROUP));
    }

    public static int getChildViewType(int rawViewType) {
        return (rawViewType & (~VIEW_TYPE_FLAG_IS_GROUP));
    }

    private ExpandableAdapterHelper() {
    }
}
