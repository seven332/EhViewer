/*
 * Copyright 2017 Hippo Seven
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

package com.hippo.ehviewer.component;

/*
 * Created by Hippo on 3/4/2017.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.widget.CoverView;
import com.hippo.ehviewer.widget.NumberRatingView;

/**
 * {@code GalleryInfoBriefHolder} shows brief of a {@link GalleryInfo}.
 */
public abstract class GalleryInfoBriefHolder<T> extends BindingViewHolder<T> {

  private static final int CATEGORY_ALPHA = 0x8a000000;

  private CoverView cover;
  private View category;
  private NumberRatingView rating;
  private TextView language;

  protected GalleryInfoBriefHolder(View view) {
    super(view);
    cover = (CoverView) view.findViewById(R.id.cover);
    category = view.findViewById(R.id.category);
    rating = (NumberRatingView) view.findViewById(R.id.rating);
    language = (TextView) view.findViewById(R.id.language);
  }

  /**
   * Binds {@link GalleryInfo} to itself.
   */
  protected void bindGalleryInfo(GalleryInfo info) {
    Context context = cover.getContext();
    cover.load(info);
    category.setBackgroundColor(setAlpha(EhUtils.getColor(info.category), CATEGORY_ALPHA));
    rating.setRating(info.rating);
    language.setText(EhUtils.getLangAbbr(context, info.language));
  }

  private static int setAlpha(int color, int alpha) {
    return (color & 0x00ffffff) | alpha;
  }

  /**
   * Creates actual view.
   */
  protected static View createView(LayoutInflater inflater, ViewGroup container) {
    return inflater.inflate(R.layout.item_gallery_info_brief, container, false);
  }
}
