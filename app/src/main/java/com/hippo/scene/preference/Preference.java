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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hippo.ehviewer.R;

public class Preference extends PreferenceBase {

    private int mPosition;

    private String mKey;
    private String mTitle;
    private String mSummary;

    private OnClickListener mOnClickListener;
    private ViewHolderGetter mViewHolderGetter;

    public Preference(String key, String title, String summary) {
        mKey = key;
        mTitle = title;
        mSummary = summary;
    }

    public String getKey() {
        return mKey;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    @Override
    int getItemViewType() {
        return PreferenceCenter.TYPE_PEFERENCE;
    }

    public static RecyclerView.ViewHolder createViewHolder(Context content, ViewGroup parent) {
        return new PreferenceHolder(LayoutInflater.from(content).inflate(R.layout.preference, parent, false));
    }

    @Override
    void bindViewHolder(Context context, RecyclerView.ViewHolder viewHolder) {
        PreferenceHolder pvh = (PreferenceHolder) viewHolder;
        pvh.title.setText(mTitle);
        pvh.summary.setText(mSummary);
        if (!stableWidgetFrame() || !pvh.hasSetWidgetFrame) {
            pvh.widgetFrame.removeAllViews();
            onBind(context, pvh.widgetFrame);
            pvh.hasSetWidgetFrame = true;
        }

        onUpdateView(viewHolder);
    }

    protected void onBind(Context context, FrameLayout widgetFrame) {
    }

    protected void onUpdateView(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public boolean onClick(RecyclerView.ViewHolder viewHolder, int x, int y) {
        return mOnClickListener != null && mOnClickListener.onClick();
    }

    protected boolean stableWidgetFrame() {
        return false;
    }

    public String getSummary() {
        return mSummary;
    }

    public void setSummary(String summary) {
        mSummary = summary;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public RecyclerView.ViewHolder getViewHolder() {
        if (mViewHolderGetter != null) {
            return mViewHolderGetter.getViewHolder(mPosition);
        } else {
            return null;
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    public void setViewHolderGetter(ViewHolderGetter viewHolderGetter) {
        mViewHolderGetter = viewHolderGetter;
    }

    public interface OnClickListener {
        boolean onClick();
    }

    public interface ViewHolderGetter {
        RecyclerView.ViewHolder getViewHolder(int position);
    }
}
