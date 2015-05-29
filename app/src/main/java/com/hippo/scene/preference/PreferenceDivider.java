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
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.hippo.ehviewer.R;
import com.hippo.widget.recyclerview.SimpleHolder;

public class PreferenceDivider extends PreferenceBase {

    @Override
    int getItemViewType() {
        return PreferenceCenter.TYPE_PEFERENCE_DIVIDER;
    }

    public static RecyclerView.ViewHolder createViewHolder(Context content, ViewGroup parent) {
        return new SimpleHolder(LayoutInflater.from(content)
                .inflate(R.layout.preference_divider, parent, false));
    }

    @Override
    void bindViewHolder(Context context, RecyclerView.ViewHolder viewHolder) {
        // Empty
    }
}
