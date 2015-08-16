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

package com.h6ah4i.android.widget.advrecyclerview.swipeable;

class SwipeReactionUtils {
    public static final int REACTION_CAN_NOT_SWIPE = 0;
    public static final int REACTION_CAN_NOT_SWIPE_WITH_RUBBER_EFFECT = 1;
    public static final int REACTION_CAN_SWIPE = 2;

    public static int extractLeftReaction(int type) {
        return (type & 0x3);
    }

    public static int extractRightReaction(int type) {
        return ((type >>> 16) & 0x3);
    }

    public static boolean canSwipeLeft(int reactionType) {
        return (extractLeftReaction(reactionType) == REACTION_CAN_SWIPE);
    }

    public static boolean canSwipeRight(int reactionType) {
        return (extractRightReaction(reactionType) == REACTION_CAN_SWIPE);
    }
}
