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

public interface ExpandableSwipeableItemAdapter<GVH extends RecyclerView.ViewHolder, CVH extends RecyclerView.ViewHolder> {
    /**
     * Called when user is attempt to swipe the group item.
     *
     * @param holder The ViewHolder which is associated to item user is attempt to start swiping.
     * @param groupPosition Group position.
     * @param x Touched X position. Relative from the itemView's top-left.
     * @param y Touched Y position. Relative from the itemView's top-left.

     * @return Reaction type. Bitwise OR of these flags;
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_NOT_SWIPE_LEFT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_NOT_SWIPE_LEFT_WITH_RUBBER_BAND_EFFECT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_SWIPE_LEFT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_NOT_SWIPE_RIGHT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_SWIPE_RIGHT}
     */
    int onGetGroupItemSwipeReactionType(GVH holder, int groupPosition, int x, int y);

    /**
     * Called when user is attempt to swipe the child item.
     *
     * @param holder The ViewHolder which is associated to item user is attempt to start swiping.
     * @param groupPosition Group position.
     * @param childPosition Child position.
     * @param x Touched X position. Relative from the itemView's top-left.
     * @param y Touched Y position. Relative from the itemView's top-left.

     * @return Reaction type. Bitwise OR of these flags;
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_NOT_SWIPE_LEFT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_NOT_SWIPE_LEFT_WITH_RUBBER_BAND_EFFECT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_SWIPE_LEFT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_NOT_SWIPE_RIGHT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT}
     *         - {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#REACTION_CAN_SWIPE_RIGHT}
     */
    int onGetChildItemSwipeReactionType(CVH holder, int groupPosition, int childPosition, int x, int y);

    /**
     * Called when sets background of the swiping group item.
     *
     * @param holder The ViewHolder which is associated to the swiping item.
     * @param groupPosition Group position.
     * @param type Background type. One of the
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#DRAWABLE_SWIPE_NEUTRAL_BACKGROUND},
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#DRAWABLE_SWIPE_LEFT_BACKGROUND} or
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#DRAWABLE_SWIPE_RIGHT_BACKGROUND}.
     */
    void onSetGroupItemSwipeBackground(GVH holder, int groupPosition, int type);


    /**
     * Called when sets background of the swiping child item.
     *
     * @param holder The ViewHolder which is associated to the swiping item.
     * @param groupPosition Group position.
     * @param childPosition Child position.
     * @param type Background type. One of the
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#DRAWABLE_SWIPE_NEUTRAL_BACKGROUND},
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#DRAWABLE_SWIPE_LEFT_BACKGROUND} or
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#DRAWABLE_SWIPE_RIGHT_BACKGROUND}.
     */
    void onSetChildItemSwipeBackground(CVH holder, int groupPosition, int childPosition, int type);

    /**
     * Called when group item is swiped.
     *
     * *Note that do not change data set and do not call notifyDataXXX() methods inside of this method.*
     *
     * @param holder The ViewHolder which is associated to the swiped item.
     * @param groupPosition Group position.
     * @param result The result code of user's swipe operation.
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_CANCELED},
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_SWIPED_LEFT} or
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_SWIPED_RIGHT}
     *
     * @return Reaction type of after swiping.
     *          One of the {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_DEFAULT},
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION} or
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_REMOVE_ITEM}.
     */
    int onSwipeGroupItem(GVH holder, int groupPosition, int result);

    /**
     * Called when child item is swiped.
     *
     * *Note that do not change data set and do not call notifyDataXXX() methods inside of this method.*
     *
     * @param holder The ViewHolder which is associated to the swiped item.
     * @param groupPosition Group position.
     * @param childPosition Child position.
     * @param result The result code of user's swipe operation.
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_CANCELED},
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_SWIPED_LEFT} or
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_SWIPED_RIGHT}
     *
     * @return Reaction type of after swiping.
     *          One of the {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_DEFAULT},
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION} or
     *          {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_REMOVE_ITEM}.
     */
    int onSwipeChildItem(CVH holder, int groupPosition, int childPosition, int result);

    /**
     * Called after {@link #onSwipeGroupItem(android.support.v7.widget.RecyclerView.ViewHolder, int, int)} method.
     *
     * You can update data set and call notifyDataXXX() methods inside of this method.
     *
     * @param holder The ViewHolder which is associated to the swiped item.
     * @param groupPosition Group position.
     * @param result The result code of user's swipe operation.
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_CANCELED},
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_SWIPED_LEFT} or
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_SWIPED_RIGHT}
     * @param reaction Reaction type. One of the {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_DEFAULT},
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION} or
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_REMOVE_ITEM}.
     */
    void onPerformAfterSwipeGroupReaction(GVH holder, int groupPosition, int result, int reaction);


    /**
     * Called after {@link #onSwipeChildItem(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * You can update data set and call notifyDataXXX() methods inside of this method.
     *
     * @param holder The ViewHolder which is associated to the swiped item.
     * @param groupPosition Group position.
     * @param childPosition Child position.
     * @param result The result code of user's swipe operation.
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_CANCELED},
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_SWIPED_LEFT} or
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#RESULT_SWIPED_RIGHT}
     * @param reaction Reaction type. One of the {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_DEFAULT},
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION} or
     *              {@link com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager#AFTER_SWIPE_REACTION_REMOVE_ITEM}.
     */
    void onPerformAfterSwipeChildReaction(CVH holder, int groupPosition, int childPosition, int result, int reaction);
}
