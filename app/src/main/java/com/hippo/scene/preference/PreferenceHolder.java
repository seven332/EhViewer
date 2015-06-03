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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene.preference;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hippo.ehviewer.R;

public class PreferenceHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public TextView summary;
    public FrameLayout widgetFrame;
    public boolean hasSetWidgetFrame;

    public PreferenceHolder(View itemView) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.title);
        summary = (TextView) itemView.findViewById(R.id.summary);
        widgetFrame = (FrameLayout) itemView.findViewById(R.id.widget_frame);
    }
}
