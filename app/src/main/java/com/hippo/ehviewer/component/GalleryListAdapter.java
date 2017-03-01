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
import android.view.ViewGroup;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.component.base.GalleryInfoAdapter;
import com.hippo.ehviewer.component.base.GalleryInfoHolder;
import com.hippo.ehviewer.widget.ContentData;

/**
 * {@link android.support.v7.widget.RecyclerView.Adapter} for
 * {@link com.hippo.ehviewer.view.GalleryListView}.
 */
public class GalleryListAdapter extends GalleryInfoAdapter {

  public GalleryListAdapter(Context context, ContentData<GalleryInfo> data) {
    super(context, data);
  }

  @Override
  protected GalleryInfoHolder createDetailHolder(LayoutInflater inflater, ViewGroup parent) {
    return GalleryListDetailHolder.create(inflater, parent);
  }

  @Override
  protected GalleryInfoHolder createBriefHolder(LayoutInflater inflater, ViewGroup parent) {
    return GalleryListBriefHolder.create(inflater, parent);
  }
}
