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

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.AppbarScene;
import com.hippo.scene.SimpleCurtain;
import com.hippo.scene.StageActivity;
import com.hippo.util.ViewUtils;
import com.hippo.widget.recyclerview.EasyRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferenceHeaderScene extends AppbarScene {

    private StageActivity mActivity;

    private EasyRecyclerView mRecyclerView;

    private HeaderAdapter mHeaderAdapter;

    private List<PreferenceHeader> mHeaderList = new ArrayList<>();

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        setContentView(R.layout.scene_preference_header);

        mActivity = getStageActivity();

        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);

        mHeaderAdapter = new HeaderAdapter();
        mRecyclerView.setAdapter(mHeaderAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(new EasyRecyclerView.OnItemClickListener() {
            @Override
            public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
                Class clazz = mHeaderList.get(position).getPreferenceClazz();
                if (clazz == null) {
                    return false;
                } else {
                    startScene(clazz, null, new SimpleCurtain(SimpleCurtain.DIRECTION_BOTTOM));
                    return true;
                }
            }
        });
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(),
                mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight(), b);
    }

    public void setPreferenceHeaders(@NonNull PreferenceHeader[] preferenceHeaders) {
        mHeaderList.clear();
        Collections.addAll(mHeaderList, preferenceHeaders);
        mHeaderAdapter.notifyDataSetChanged();
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
            View view = LayoutInflater.from(mActivity).inflate(R.layout.preference_header, parent, false);
            return new HeaderHolder(view);
        }

        @Override
        public void onBindViewHolder(HeaderHolder holder, int position) {
            holder.icon.setImageDrawable(mHeaderList.get(position).getIcon());
            holder.title.setText(mHeaderList.get(position).getTitle());
            ViewUtils.setVisibility(holder.tileDivider, position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return mHeaderList.size();
        }
    }
}
