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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.LinearDividerItemDecoration;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.rippleold.RippleSalon;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ViewUtils;

import java.util.ArrayList;

public final class GalleryInfoScene extends ToolbarScene implements EasyRecyclerView.OnItemClickListener {

    public static final String KEY_GALLERY_DETAIL = "gallery_detail";
    public static final String KEY_KEYS = "keys";
    public static final String KEY_VALUES = "values";

    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private ArrayList<String> mKeys;
    @Nullable
    private ArrayList<String> mValues;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void handlerArgs(Bundle args) {
        if (args == null) {
            return;
        }
        GalleryDetail gd = args.getParcelable(KEY_GALLERY_DETAIL);
        if (gd == null) {
            return;
        }
        if (mKeys == null || mValues == null) {
            return;
        }

        Resources resources = getResources();
        mKeys.add(resources.getString(R.string.header_key));
        mValues.add(resources.getString(R.string.header_value));
        mKeys.add(resources.getString(R.string.key_gid));
        mValues.add(Long.toString(gd.gid));
        mKeys.add(resources.getString(R.string.key_token));
        mValues.add(gd.token);
        mKeys.add(resources.getString(R.string.key_url));
        mValues.add(EhUrl.getGalleryDetailUrl(gd.gid, gd.token));
        mKeys.add(resources.getString(R.string.key_title));
        mValues.add(gd.title);
        mKeys.add(resources.getString(R.string.key_title_jpn));
        mValues.add(gd.titleJpn);
        mKeys.add(resources.getString(R.string.key_thumb));
        mValues.add(gd.thumb);
        mKeys.add(resources.getString(R.string.key_category));
        mValues.add(EhUtils.getCategory(gd.category));
        mKeys.add(resources.getString(R.string.key_uploader));
        mValues.add(gd.uploader);
        mKeys.add(resources.getString(R.string.key_posted));
        mValues.add(gd.posted);
        mKeys.add(resources.getString(R.string.key_parent));
        mValues.add(gd.parent);
        mKeys.add(resources.getString(R.string.key_visible));
        mValues.add(gd.visible);
        mKeys.add(resources.getString(R.string.key_language));
        mValues.add(gd.language);
        mKeys.add(resources.getString(R.string.key_pages));
        mValues.add(Integer.toString(gd.pages));
        mKeys.add(resources.getString(R.string.key_size));
        mValues.add(gd.size);
        mKeys.add(resources.getString(R.string.key_resize));
        mValues.add(gd.resize);
        mKeys.add(resources.getString(R.string.key_favorites));
        mValues.add(Integer.toString(gd.favoredTimes));
        mKeys.add(resources.getString(R.string.key_favorited));
        mValues.add(Boolean.toString(gd.isFavored));
        mKeys.add(resources.getString(R.string.key_rates));
        mValues.add(Integer.toString(gd.ratedTimes));
        mKeys.add(resources.getString(R.string.key_rating));
        mValues.add(Float.toString(gd.rating));
        mKeys.add(resources.getString(R.string.key_torrents));
        mValues.add(Integer.toString(gd.torrentCount));
        mKeys.add(resources.getString(R.string.key_torrent_url));
        mValues.add(gd.torrentUrl);
    }

    protected void onInit() {
        mKeys = new ArrayList<>();
        mValues = new ArrayList<>();
        handlerArgs(getArguments());
    }

    protected void onRestore(@NonNull Bundle savedInstanceState) {
        mKeys = savedInstanceState.getStringArrayList(KEY_KEYS);
        mValues = savedInstanceState.getStringArrayList(KEY_VALUES);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(KEY_KEYS, mKeys);
        outState.putStringArrayList(KEY_VALUES, mValues);
    }

    @SuppressWarnings("deprecation")
    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_gallery_info, container, false);

        EasyRecyclerView recyclerView = (EasyRecyclerView) ViewUtils.$$(view, R.id.recycler_view);
        InfoAdapter adapter = new InfoAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL, getResources().getColor(R.color.divider),
                LayoutUtils.dp2pix(getContext(), 1));
        decoration.setPadding(getResources().getDimensionPixelOffset(R.dimen.keyline_margin));
        recyclerView.addItemDecoration(decoration);
        recyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        recyclerView.setClipToPadding(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.gallery_info);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        if (position != 0 && mValues != null) {
            ClipboardManager cmb = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cmb.setPrimaryClip(ClipData.newPlainText(null, mValues.get(position)));
            showTip(R.string.copied_to_clipboard, LENGTH_SHORT);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onNavigationClick() {
        onBackPressed();
    }

    private static class InfoHolder extends RecyclerView.ViewHolder {

        private final TextView key;
        private final TextView value;

        public InfoHolder(View itemView) {
            super(itemView);

            key = (TextView) itemView.findViewById(R.id.key);
            value = (TextView) itemView.findViewById(R.id.value);
        }
    }

    private class InfoAdapter extends RecyclerView.Adapter<InfoHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_DATA = 1;

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_HEADER;
            } else {
                return TYPE_DATA;
            }
        }

        @Override
        public InfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new InfoHolder(getActivity().getLayoutInflater().inflate(viewType == TYPE_HEADER ?
                    R.layout.item_gallery_info_header : R.layout.item_gallery_info_data, parent, false));
        }

        @Override
        public void onBindViewHolder(InfoHolder holder, int position) {
            if (mKeys != null && mValues != null) {
                holder.key.setText(mKeys.get(position));
                holder.value.setText(mValues.get(position));
                holder.itemView.setEnabled(position != 0);
            }
        }

        @Override
        public int getItemCount() {
            return mKeys == null || mValues == null ? 0 : Math.min(mKeys.size(), mValues.size());
        }
    }
}
