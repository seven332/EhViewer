/*
 * Copyright 2019 Hippo Seven
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

package com.hippo.ehviewer.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.dao.Filter;
import com.hippo.util.DrawableManager;
import com.hippo.view.ViewTransition;
import com.hippo.yorozuya.ViewUtils;
import java.util.List;

public class FilterActivity extends ToolbarActivity {

    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private FilterAdapter mAdapter;
    @Nullable
    private List<Filter> mFilterList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);

        mFilterList = EhDB.getAllFilter();

        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(this, R.id.recycler_view);
        TextView tip = (TextView) ViewUtils.$$(this, R.id.tip);
        mViewTransition = new ViewTransition(mRecyclerView, tip);

        Drawable drawable = DrawableManager.getVectorDrawable(this, R.drawable.big_filter);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        mAdapter = new FilterAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setClipChildren(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.hasFixedSize();
        mRecyclerView.setItemAnimator(null);

        updateView(false);
    }

    private void updateView(boolean animation) {
        if (null == mViewTransition) {
            return;
        }

        if (null == mFilterList || 0 == mFilterList.size()) {
            mViewTransition.showView(1, animation);
        } else {
            mViewTransition.showView(0, animation);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRecyclerView = null;
        mViewTransition = null;
        mAdapter = null;
        mFilterList = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class FilterHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView text;
        private final ImageView icon;

        public FilterHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            icon = itemView.findViewById(R.id.icon);
            icon.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("filter", text.getText().toString()));
                Snackbar.make(v, R.string.copied_to_clipboard, 3000).show();
            }
        }
    }

    private class FilterAdapter extends RecyclerView.Adapter<FilterHolder> {

        @NonNull
        @Override
        public FilterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FilterHolder(getLayoutInflater().inflate(R.layout.item_filter, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull FilterHolder holder, int position) {
            if (null == mFilterList) {
                return;
            }

            Filter filter = mFilterList.get(position);
            holder.text.setText(filter.text);
        }

        @Override
        public int getItemCount() {
            return null != mFilterList ? mFilterList.size() : 0;
        }
    }
}
