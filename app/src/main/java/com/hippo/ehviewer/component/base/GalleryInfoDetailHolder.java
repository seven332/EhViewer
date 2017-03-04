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

package com.hippo.ehviewer.component.base;

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
import com.hippo.ehviewer.util.ReadableTime;
import com.hippo.ehviewer.widget.CoverView;
import com.hippo.ehviewer.widget.SmallRatingView;

/**
 * {@code GalleryInfoDetailHolder} shows detail of a {@link GalleryInfo}.
 */
public abstract class GalleryInfoDetailHolder<T> extends BindingViewHolder<T> {

  private CoverView cover;
  private TextView title;
  private TextView uploader;
  private SmallRatingView rating;
  private TextView category;
  private TextView date;
  private TextView language;
  private View favourite;
  private View download;
  private View invalid;
  private TextView note;

  protected GalleryInfoDetailHolder(View view) {
    super(view);
    cover = (CoverView) view.findViewById(R.id.cover);
    title = (TextView) view.findViewById(R.id.title);
    uploader = (TextView) view.findViewById(R.id.uploader);
    rating = (SmallRatingView) view.findViewById(R.id.rating);
    category = (TextView) view.findViewById(R.id.category);
    date = (TextView) view.findViewById(R.id.date);
    language = (TextView) view.findViewById(R.id.language);
    favourite = view.findViewById(R.id.favourite);
    download = view.findViewById(R.id.download);
    invalid = view.findViewById(R.id.invalid);
    note = (TextView) view.findViewById(R.id.note);
  }

  /**
   * Binds {@link GalleryInfo} to itself.
   */
  protected void bindGalleryInfo(GalleryInfo info) {
    Context context = cover.getContext();
    cover.load(info);
    // TODO titleJpn
    title.setText(info.title);
    uploader.setText(info.uploader);
    rating.setRating(info.rating);
    category.setText(EhUtils.getCategory(info.category));
    category.setBackgroundColor(EhUtils.getColor(info.category));
    date.setText(ReadableTime.getTimeAgo(context, info.date));
    language.setText(EhUtils.getLangAbbr(context, info.language));
    favourite.setVisibility(info.favouriteSlot != EhUtils.FAV_CAT_UNKNOWN ? View.VISIBLE : View.GONE);
    download.setVisibility(View.GONE);
    invalid.setVisibility(info.invalid ? View.VISIBLE : View.GONE);
  }

  /**
   * Binds note to itself.
   */
  protected void bindNote(String note) {
    this.note.setText(note);
  }

  /**
   * Creates actual view.
   */
  protected static View createView(LayoutInflater inflater, ViewGroup container) {
    return inflater.inflate(R.layout.item_gallery_info_detail, container, false);
  }
}
