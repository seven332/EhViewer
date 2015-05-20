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

package com.hippo.ehviewer.ui.scene;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.effect.ripple.RippleSalon;
import com.hippo.ehviewer.R;
import com.hippo.scene.Scene;
import com.hippo.scene.StageActivity;
import com.hippo.util.ViewUtils;
import com.hippo.widget.recyclerview.EasyRecyclerView;

public final class SettingsHeaderScene extends Scene implements EasyRecyclerView.OnItemClickListener {

    private static final int[] HEADER_ICON_ARRAY = {
            R.drawable.ic_cellphone_android_theme_primary,
            R.drawable.ic_eh_theme_primary
    };

    private static final int[] HEADER_TITLE_ARRAY = {
            R.string._goto,
            R.string.username
    };

    private StageActivity mActivity;

    private EasyRecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getStageActivity();

        mRecyclerView = new EasyRecyclerView(mActivity);
        setContentView(mRecyclerView);

        mRecyclerView.setAdapter(new HeaderAdapter());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(mActivity, false));
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(),
                mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight(), b);
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        return false;
    }

    private static class HeaderHolder extends RecyclerView.ViewHolder {

        public final ImageView icon;
        public final TextView title;
        public final View tileDivider;

        public HeaderHolder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.title);
            tileDivider = itemView.findViewById(R.id.tile_divider);
        }
    }

    private class HeaderAdapter extends RecyclerView.Adapter<HeaderHolder> {

        @Override
        public HeaderHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mActivity).inflate(R.layout.settings_header, parent, false);
            return new HeaderHolder(view);
        }

        @Override
        public void onBindViewHolder(HeaderHolder holder, int position) {
            holder.icon.setImageResource(HEADER_ICON_ARRAY[position]);
            holder.title.setText(HEADER_TITLE_ARRAY[position]);
            ViewUtils.setVisibility(holder.tileDivider,
                    position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return HEADER_ICON_ARRAY.length;
        }
    }
}
