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

package com.hippo.scene.preference;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.effect.ripple.RippleSalon;
import com.hippo.ehviewer.R;
import com.hippo.scene.AppbarScene;
import com.hippo.util.ViewUtils;
import com.hippo.widget.recyclerview.EasyRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferenceScene extends AppbarScene {

    private static PreferenceDivider PREFERENCE_DIVIDER = new PreferenceDivider();

    private EasyRecyclerView mRecyclerView;

    private PreferenceAdapter mAdapter;
    private PreferenceViewHolderGetter mPreferenceViewHolderGetter = new PreferenceViewHolderGetter();

    private List<PreferenceSet> mPreferenceSetList = new ArrayList<>();
    private List<PreferenceBase> mPreferenceBaseList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRecyclerView = new EasyRecyclerView(getStageActivity());
        setContentView(mRecyclerView);

        mAdapter = new PreferenceAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getStageActivity()));
        mRecyclerView.setOnItemClickListener(new PreferenceItemClickListener());
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(getStageActivity(), false));
        mRecyclerView.setOnDrawSelectorListener(new PreferencesDrawSelectorListener());
        mRecyclerView.setClipToPadding(false);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(),
                mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight(), b);
    }

    public void setPreferenceSet(PreferenceSet[] preferenceSets) {
        // Update mPreferenceSetList
        List<PreferenceSet> preferenceSetList = mPreferenceSetList;
        preferenceSetList.clear();
        Collections.addAll(preferenceSetList, preferenceSets);
        // Update mPreferenceBasesList
        int position = 0;
        List<PreferenceBase> preferenceBaseList = mPreferenceBaseList;
        preferenceBaseList.clear();
        for (PreferenceSet ps : preferenceSets) {
            PreferenceCategory pc = ps.getPreferenceCategory();
            if (pc != null) {
                preferenceBaseList.add(pc);
                position++;
            }
            int pdCount = ps.getPreferenceCount();
            for (int i = 0; i < pdCount; i++) {
                if (i != 0) {
                    preferenceBaseList.add(PREFERENCE_DIVIDER);
                    position++;
                }
                Preference p = ps.getPreferenceData(i);
                p.setPosition(position);
                p.setViewHolderGetter(mPreferenceViewHolderGetter);
                preferenceBaseList.add(p);
                position++;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private class PreferenceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return PreferenceCenter.createViewHolder(viewType, getStageActivity(), parent);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            mPreferenceBaseList.get(position).bindViewHolder(getStageActivity(), holder);
        }

        @Override
        public int getItemCount() {
            return mPreferenceBaseList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mPreferenceBaseList.get(position).getItemViewType();
        }
    }

    private class PreferenceItemClickListener implements EasyRecyclerView.OnItemClickListener {

        private int[] mTemp = new int[2];

        @Override
        public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
            ViewUtils.getLocationInAncestor(parent, mTemp, R.id.stage);
            return mPreferenceBaseList.get(position).onClick(parent.getChildViewHolder(view),
                    (int) (mTemp[0] + parent.getTouchStartX()),
                    (int) (mTemp[1] + parent.getTouchStartY()));
        }
    }

    private class PreferencesDrawSelectorListener implements EasyRecyclerView.OnDrawSelectorListener {
        @Override
        public boolean beforeDrawSelector(int position) {
            PreferenceBase pb = mPreferenceBaseList.get(position);
            return !(pb instanceof PreferenceCategory) &&
                    !(pb instanceof PreferenceDivider);
        }
    }

    private class PreferenceViewHolderGetter implements Preference.ViewHolderGetter {

        @Override
        public RecyclerView.ViewHolder getViewHolder(int position) {
            return mRecyclerView.findViewHolderForAdapterPosition(position);
        }
    }
}
