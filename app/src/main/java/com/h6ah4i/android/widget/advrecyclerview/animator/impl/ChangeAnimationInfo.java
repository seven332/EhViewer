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

package com.h6ah4i.android.widget.advrecyclerview.animator.impl;

import android.support.v7.widget.RecyclerView;

public class ChangeAnimationInfo extends ItemAnimationInfo {
    public RecyclerView.ViewHolder newHolder, oldHolder;
    public int fromX, fromY, toX, toY;

    public ChangeAnimationInfo(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder,
                               int fromX, int fromY, int toX, int toY) {
        this.oldHolder = oldHolder;
        this.newHolder = newHolder;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }

    @Override
    public RecyclerView.ViewHolder getAvailableViewHolder() {
        return (oldHolder != null) ? oldHolder : newHolder;
    }

    @Override
    public void clear(RecyclerView.ViewHolder item) {
        if (oldHolder == item) {
            oldHolder = null;
        }
        if (newHolder == item) {
            newHolder = null;
        }
        if (oldHolder == null && newHolder == null) {
            fromX = 0;
            fromY = 0;
            toX = 0;
            toY = 0;
        }
    }

    @Override
    public String toString() {
        return "ChangeInfo{" +
                ", oldHolder=" + oldHolder +
                ", newHolder=" + newHolder +
                ", fromX=" + fromX +
                ", fromY=" + fromY +
                ", toX=" + toX +
                ", toY=" + toY +
                '}';
    }
}
