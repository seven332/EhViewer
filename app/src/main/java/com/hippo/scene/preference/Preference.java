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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hippo.ehviewer.R;

public class Preference extends PreferenceBase {

    private int mPosition;

    private @NonNull
    String mKey;
    private String mTitle;
    private String mSummary;

    private OnClickListener mOnClickListener;
    private OnValueChangeListener mOnValueChangeListener;
    private ViewHolderGetter mViewHolderGetter;

    public Preference(@NonNull String key, String title, String summary) {
        mKey = key;
        mTitle = title;
        mSummary = summary;
    }

    public @NonNull String getKey() {
        return mKey;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSummary() {
        return mSummary;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    /**
     * @return the text display below title
     */
    public String getDisplaySummary() {
        return mSummary;
    }

    /**
     * Call it when you want to save new value
     */
    public void setValue(Object newValue) {
        if (mOnValueChangeListener == null || mOnValueChangeListener.OnValueChange(this, newValue)) {
            storeValue(newValue);
            RecyclerView.ViewHolder viewHolder = getViewHolder();
            if (viewHolder != null) {
                onUpdateViewByNewValue(viewHolder, newValue);
            }
        }
    }

    /**
     * Save value to file
     */
    protected void storeValue(Object newValue) {
    }

    /**
     * Update view because value change
     */
    protected void onUpdateViewByNewValue(@NonNull RecyclerView.ViewHolder viewHolder, Object newValue) {
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

        updateSummary(viewHolder);

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

    protected void updateSummary(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder == null) {
            viewHolder = getViewHolder();
        }
        if (viewHolder != null) {
            PreferenceHolder pvh = (PreferenceHolder) viewHolder;
            String displaySummary = getDisplaySummary();
            if (displaySummary == null) {
                pvh.summary.setVisibility(View.GONE);
            } else {
                pvh.summary.setVisibility(View.VISIBLE);
                pvh.summary.setText(displaySummary);
            }
        }
    }

    @Override
    public boolean onClick(RecyclerView.ViewHolder viewHolder, int x, int y) {
        return mOnClickListener != null && mOnClickListener.onClick(this);
    }

    protected boolean stableWidgetFrame() {
        return false;
    }

    private RecyclerView.ViewHolder getViewHolder() {
        if (mViewHolderGetter != null) {
            return mViewHolderGetter.getViewHolder(mPosition);
        } else {
            return null;
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        mOnValueChangeListener = listener;
    }

    void setViewHolderGetter(ViewHolderGetter viewHolderGetter) {
        mViewHolderGetter = viewHolderGetter;
    }

    public interface OnClickListener {
        boolean onClick(Preference preferenc);
    }

    public interface OnValueChangeListener {
        boolean OnValueChange(Preference preference, Object newValue);
    }

    interface ViewHolderGetter {
        RecyclerView.ViewHolder getViewHolder(int position);
    }
}
