/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.widget.LoadImageView;

class GalleryHolder extends RecyclerView.ViewHolder {

    public final LoadImageView thumb;
    public final TextView title;
    public final TextView uploader;
    public final SimpleRatingView rating;
    public final TextView category;
    public final TextView posted;
    public final TextView simpleLanguage;

    public GalleryHolder(View itemView) {
        super(itemView);

        thumb = (LoadImageView) itemView.findViewById(R.id.thumb);
        title = (TextView) itemView.findViewById(R.id.title);
        uploader = (TextView) itemView.findViewById(R.id.uploader);
        rating = (SimpleRatingView) itemView.findViewById(R.id.rating);
        category = (TextView) itemView.findViewById(R.id.category);
        posted = (TextView) itemView.findViewById(R.id.posted);
        simpleLanguage = (TextView) itemView.findViewById(R.id.simple_language);
    }
}
