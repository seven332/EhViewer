/*
 * Copyright 2015 Hippo Seven
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
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.AppbarScene;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.widget.recyclerview.LinearDividerItemDecoration;
import com.hippo.yorozuya.LayoutUtils;

import java.util.ArrayList;
import java.util.List;

public class InfoScene extends AppbarScene implements EasyRecyclerView.OnItemLongClickListener {

    public static final String KEY_INFO = "key_info";

    private Resources mResources;

    private EasyRecyclerView mRecyclerView;

    private List<String[]> mInfo = new ArrayList<>();

    private int mRecyclerViewOriginalPaddingBottom;

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setContentView(R.layout.scene_info);
        setTitle(R.string.info);
        setIcon(R.drawable.ic_arrow_left_dark_x24);

        Context context = getStageActivity();
        mResources = context.getResources();

        EasyRecyclerView recyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);
        InfoAdapter adapter = new InfoAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL, mResources.getColor(R.color.divider_light),
                LayoutUtils.dp2pix(getStageActivity(), 1));
        decoration.setPadding(mResources.getDimensionPixelOffset(R.dimen.keyline_margin));
        recyclerView.addItemDecoration(decoration);
        recyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        recyclerView.setClipToPadding(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnItemLongClickListener(this);
        mRecyclerView = recyclerView;
        mRecyclerViewOriginalPaddingBottom = recyclerView.getPaddingBottom();
    }

    @Override
    protected void onBind() {
        super.onBind();

        handleAnnouncer(getAnnouncer());
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        super.onGetFitPaddingBottom(b);

        View recyclerView = mRecyclerView;
        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                mRecyclerViewOriginalPaddingBottom + b);
    }

    private void addData(@StringRes int key, String value) {
        mInfo.add(new String[]{mResources.getString(key), value});
    }

    private void handleAnnouncer(Announcer announcer) {
        if (announcer == null) {
            finish();
            return;
        }

        GalleryDetail gd = announcer.getExtra(KEY_INFO, GalleryDetail.class);
        if (gd != null) {
            addData(R.string.header_key, mResources.getString(R.string.header_value));
            addData(R.string.key_gid, Integer.toString(gd.gid));
            addData(R.string.key_token, gd.token);
            addData(R.string.key_title, gd.title);
            addData(R.string.key_title_jpn, gd.titleJpn);
            addData(R.string.key_thumb, gd.thumb);
            addData(R.string.key_category, EhUtils.getCategory(gd.category));
            addData(R.string.key_uploader, gd.uploader);
            addData(R.string.key_posted, gd.posted);
            addData(R.string.key_language, gd.language);
            addData(R.string.key_pages, Integer.toString(gd.pageCount));
            addData(R.string.key_size, gd.size);
            addData(R.string.key_resize, gd.resize);
            addData(R.string.key_favorited, Integer.toString(gd.favoredTimes));
            addData(R.string.key_rated, Integer.toString(gd.ratedTimes));
            addData(R.string.key_rating, Float.toString(gd.rating));
            addData(R.string.key_torrents, Integer.toString(gd.torrentCount));
        }
    }

    @Override
    public void onIconClick() {
        finish();
    }

    @Override
    public boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id) {
        if (position != 0) {
            ClipboardManager cmb = (ClipboardManager) getStageActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            cmb.setPrimaryClip(ClipData.newPlainText(null, mInfo.get(position)[1]));
            Toast.makeText(getStageActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return false;
        }
    }

    private static class InfoHolder extends RecyclerView.ViewHolder {

        private TextView key;
        private TextView value;

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
            return new InfoHolder(getStageActivity().getLayoutInflater().inflate(
                    viewType == TYPE_HEADER ? R.layout.item_info_header : R.layout.item_info_data, parent, false));
        }

        @Override
        public void onBindViewHolder(InfoHolder holder, int position) {
            String[] strings = mInfo.get(position);
            holder.key.setText(strings[0]);
            holder.value.setText(strings[1]);
            holder.itemView.setEnabled(position != 0);
        }

        @Override
        public int getItemCount() {
            return mInfo.size();
        }
    }
}
