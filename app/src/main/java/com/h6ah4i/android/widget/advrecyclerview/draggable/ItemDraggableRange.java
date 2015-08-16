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

package com.h6ah4i.android.widget.advrecyclerview.draggable;

public class ItemDraggableRange {
    private final int mStart;
    private final int mEnd;

    public ItemDraggableRange(int start, int end) {
        if (!(start <= end)) {
            throw new IllegalArgumentException("end position (= " + end + ") is smaller than start position (=" + start + ")");
        }

        mStart = start;
        mEnd = end;
    }

    public int getStart() {
        return mStart;
    }

    public int getEnd() {
        return mEnd;
    }

    public boolean checkInRange(int position) {
        return ((position >= mStart) && (position <= mEnd));
    }

    protected String getClassName() {
        return "ItemDraggableRange";
    }

    @Override
    public String toString() {
        return getClassName() + "{" +
                "mStart=" + mStart +
                ", mEnd=" + mEnd +
                '}';
    }
}

