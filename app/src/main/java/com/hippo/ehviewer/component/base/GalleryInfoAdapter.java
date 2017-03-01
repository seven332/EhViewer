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
 * Created by Hippo on 2/24/2017.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.hippo.easyrecyclerview.EasyAdapter;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.widget.ContentContract;

/**
 * Abstract {@code Adapter} for gallery info list.
 * <p>
 * Implements {@link #createDetailHolder(LayoutInflater, ViewGroup)}
 * and {@link #createBriefHolder(LayoutInflater, ViewGroup)}
 * to create actual {@code ViewHolder}.
 */
public abstract class GalleryInfoAdapter extends EasyAdapter<GalleryInfoHolder> {

  private static final int TYPE_DETAIL = 0;
  private static final int TYPE_BRIEF = 1;

  public LayoutInflater inflater;
  public ContentContract.DataPresenter<GalleryInfo> data;
  public int type = TYPE_DETAIL;

  public GalleryInfoAdapter(Context context, ContentContract.DataPresenter<GalleryInfo> data) {
    this.inflater = LayoutInflater.from(context);
    this.data = data;
  }

  @Override
  public final GalleryInfoHolder onCreateViewHolder2(ViewGroup parent, int viewType) {
    switch (viewType) {
      case TYPE_DETAIL:
        return createDetailHolder(inflater, parent);
      case TYPE_BRIEF:
        return createBriefHolder(inflater, parent);
      default:
        throw new IllegalStateException("Unknown view type: " + viewType);
    }
  }

  /**
   * Creates detail gallery info {@code ViewHolder}.
   */
  protected abstract GalleryInfoHolder createDetailHolder(LayoutInflater inflater,
      ViewGroup parent);

  /**
   * Creates brief gallery info {@code ViewHolder}.
   */
  protected abstract GalleryInfoHolder createBriefHolder(LayoutInflater inflater,
      ViewGroup parent);

  @Override
  public final void onBindViewHolder(GalleryInfoHolder holder, int position) {
    holder.bind(data.get(position));
  }

  @Override
  public final int getItemCount() {
    return data.size();
  }

  @Override
  public final long getItemId(int position) {
    return data.get(position).gid;
  }

  @Override
  public final int getItemViewType(int position) {
    return type;
  }

  /**
   * Switches to detail mode.
   */
  public final void showDetail() {
    if (type != TYPE_DETAIL) {
      type = TYPE_DETAIL;
      notifyDataSetChanged();
    }
  }

  /**
   * Switches to brief mode.
   */
  public final void showBrief() {
    if (type != TYPE_BRIEF) {
      type = TYPE_BRIEF;
      notifyDataSetChanged();
    }
  }

  /**
   * Solidify the data to release the actual data.
   */
  public final void solidify() {
    data = data.solidify();
  }
}
