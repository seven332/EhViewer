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
 * Created by Hippo on 2/3/2017.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.component.base.GalleryInfoHolder;
import com.hippo.ehviewer.util.ReadableTime;
import com.hippo.ehviewer.widget.CoverView;
import com.hippo.ehviewer.widget.SmallRatingView;

/**
 * Detail {@code ViewHolder} for {@link GalleryListAdapter}.
 */
public class GalleryListDetailHolder extends GalleryInfoHolder {

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

  protected GalleryListDetailHolder(View view) {
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

  @Override
  public void bind(GalleryInfo galleryInfo) {
    Context context = cover.getContext();
    cover.load(galleryInfo);
    // TODO titleJpn
    title.setText(galleryInfo.title);
    uploader.setText(galleryInfo.uploader);
    rating.setRating(galleryInfo.rating);
    category.setText(EhUtils.getCategory(galleryInfo.category));
    category.setBackgroundColor(EhUtils.getColor(galleryInfo.category));
    date.setText(ReadableTime.getTimeAgo(context, galleryInfo.date));
    language.setText(EhUtils.getLangAbbr(context, galleryInfo.language));
    favourite.setVisibility(galleryInfo.favouriteSlot != -1 ? View.VISIBLE : View.GONE);
    download.setVisibility(View.GONE);
    invalid.setVisibility(galleryInfo.invalid ? View.VISIBLE : View.GONE);
    note.setText(galleryInfo.note);
  }

  public static GalleryListDetailHolder create(LayoutInflater inflater, ViewGroup container) {
    return new GalleryListDetailHolder(
        inflater.inflate(R.layout.item_gallery_list_detail, container, false));
  }
}
