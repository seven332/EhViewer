/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene.preference;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

abstract class PreferenceBase {

    abstract int getItemViewType();

    abstract void bindViewHolde(Context context, RecyclerView.ViewHolder viewHolder);

    /**
     * @param viewHolder the view holder of the view
     * @param x x position in scene
     * @param y y position in scene
     * @return true if handle the click action
     */
    public boolean onClick(RecyclerView.ViewHolder viewHolder, int x, int y) {
        return false;
    }
}
