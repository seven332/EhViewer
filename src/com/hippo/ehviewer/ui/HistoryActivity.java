/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.ui;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.cardview.CardViewSalon;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.PullViewGroup;
import com.hippo.ehviewer.widget.RatingView;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

public class HistoryActivity extends AbsGalleryActivity
        implements AdapterView.OnItemClickListener, MaterialAlertDialog.OnClickListener {

    private static final String TAG = HistoryActivity.class.getSimpleName();

    private Data mData;

    private PullViewGroup mPullViewGroup;
    private ListView mList;

    private BaseAdapter mAdapter;
    private int mFilterMode;

    private Dialog mFilterDialog;
    private CheckBox mJustBrowse;
    private CheckBox mHaveRead;

    private Dialog mClearDialog;

    private static final String HISTORY_URL = "ehviewer://history";
    private static final String KEY_HISTORY_FILTER = "history_filter";

    private void setFilterDialogMode(int filterMode) {
        if ((filterMode & Data.BROWSE) == 0)
            mJustBrowse.setChecked(false);
        else
            mJustBrowse.setChecked(true);
        if ((filterMode & Data.READ) == 0)
            mHaveRead.setChecked(false);
        else
            mHaveRead.setChecked(true);
    }

    private int getFilterDialogMode() {
        int filterMode = 0;
        filterMode |= mJustBrowse.isChecked() ? Data.BROWSE : 0;
        filterMode |= mHaveRead.isChecked() ? Data.READ : 0;
        return filterMode;
    }

    @Override
    public boolean onClick(MaterialAlertDialog dialog, int which) {
        if (dialog == mFilterDialog) {
            switch (which) {
            case MaterialAlertDialog.POSITIVE:
                int newFilterMode = getFilterDialogMode();
                if (newFilterMode != mFilterMode) {
                    mFilterMode = newFilterMode;
                    Config.setInt(KEY_HISTORY_FILTER, mFilterMode);
                    refresh();
                }
            }
            return true;

        } else if (dialog == mClearDialog) {
            switch (which) {
            case MaterialAlertDialog.POSITIVE:
                Data.getInstance().clearHistory();
                refresh();
            }
            return true;

        }
        return true;
    }

    @SuppressLint("InflateParams")
    private void createFilterDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.history_filter, null);
        mJustBrowse = (CheckBox) view.findViewById(R.id.just_browse);
        mHaveRead = (CheckBox) view.findViewById(R.id.have_read);

        mFilterDialog = new MaterialAlertDialog.Builder(this).setTitle(R.string.filter)
                .setView(view, true)
                .setDefaultButton(MaterialAlertDialog.POSITIVE | MaterialAlertDialog.NEGATIVE)
                .setButtonListener(this).create();
    }

    private void createClearDialog() {
        mClearDialog = new MaterialAlertDialog.Builder(this).setTitle(R.string.attention)
                .setMessage(R.string.clear_history_message)
                .setDefaultButton(MaterialAlertDialog.POSITIVE | MaterialAlertDialog.NEGATIVE)
                .setButtonListener(this).create();
    }

    @Override
    public void onOrientationChanged(int paddingTop, int paddingBottom) {
        mList.setPadding(mList.getPaddingLeft(), paddingTop,
                mList.getPaddingRight(), paddingBottom);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.favorite_list;
    }

    @Override
    protected void onResume() {
        super.onResume();

        refresh();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mData = Data.getInstance();

        mFilterMode = Config.getInt(KEY_HISTORY_FILTER, Data.BROWSE | Data.READ);

        // Just avoid error
        setBehindContentView(new View(this));
        setSlidingActionBarEnabled(false);
        getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
        getSlidingMenu().setEnabled(false);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        int color = Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor();
        color = color & 0x00ffffff | 0xdd000000;
        Drawable drawable = new ColorDrawable(color);
        actionBar.setBackgroundDrawable(drawable);
        Ui.translucent(this, color);

        mPullViewGroup = getPullViewGroup();
        mList = (ListView) getContentView();

        mPullViewGroup.setEnabledHeader(false);
        mPullViewGroup.setEnabledFooter(false);

        mAdapter = new ListAdapter(getGalleryList());
        mList.setAdapter(mAdapter);
        mList.setClipToPadding(false);
        mList.setDivider(null);
        mList.setOnItemClickListener(this);

        createFilterDialog();
        createClearDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;

        case R.id.action_filter:
            if (!mFilterDialog.isShowing()) {
                setFilterDialogMode(mFilterMode);
                mFilterDialog.show();
            }
            return true;

        case R.id.action_clear:
            if (!mClearDialog.isShowing())
                mClearDialog.show();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected String getTargetUrl(int targetPage) {
        return HISTORY_URL;
    }

    @Override
    protected void doGetGallerys(String url, long taskStamp,
            OnGetListListener listener) {
        if (url.equals(HISTORY_URL)) {
            List<GalleryInfo> giList = new ArrayList<GalleryInfo>(mData.getHistory(mFilterMode, true));
            if (giList == null || giList.size() == 0)
                listener.onSuccess(mAdapter, taskStamp, giList, 0);
            else
                listener.onSuccess(mAdapter, taskStamp, giList, 1);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        Intent intent = new Intent(this, GalleryDetailActivity.class);
        GalleryInfo gi = getGalleryInfo(position);
        intent.putExtra(GalleryDetailActivity.KEY_G_INFO, gi);
        startActivity(intent);
    }

    public class ListAdapter extends BaseAdapter {
        private final List<GalleryInfo> mGiList;
        private final ImageLoader mImageLoader;

        public ListAdapter(List<GalleryInfo> gilist) {
            mGiList = gilist;
            mImageLoader =ImageLoader.getInstance(HistoryActivity.this);
        }

        @Override
        public int getCount() {
            return mGiList.size();
        }

        @Override
        public Object getItem(int position) {
            return mGiList == null ? 0 : mGiList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GalleryInfo gi= mGiList.get(position);
            if (convertView == null || !(convertView instanceof LinearLayout)) {
                convertView = LayoutInflater.from(HistoryActivity.this)
                        .inflate(R.layout.favorite_list_item, parent, false);
                CardViewSalon.reformWithShadow(((ViewGroup)convertView).getChildAt(0), new int[][]{
                                new int[]{android.R.attr.state_pressed},
                                new int[]{android.R.attr.state_activated},
                                new int[]{}},
                                new int[]{0xff84cae4, 0xff33b5e5, 0xFFFAFAFA}, null, false);
            }
            final LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.thumb);
            if (!String.valueOf(gi.gid).equals(thumb.getKey())) {
                // Set margin top 8dp if position is 0, otherwise 4dp
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)
                        convertView.findViewById(R.id.card_view).getLayoutParams();
                if (position == 0)
                    lp.topMargin = Ui.dp2pix(8);
                else
                    lp.topMargin = Ui.dp2pix(4);

                // Set new thumb
                thumb.setImageDrawable(null);
                thumb.setLoadInfo(gi.thumb, String.valueOf(gi.gid));
                mImageLoader.add(gi.thumb, String.valueOf(gi.gid),
                        new LoadImageView.SimpleImageGetListener(thumb).setFixScaleType(true));
            }
            // Set manga name
            TextView name = (TextView) convertView.findViewById(R.id.title);
            name.setText(gi.title);
            // Set uploder
            TextView uploader = (TextView) convertView.findViewById(R.id.uploader);
            uploader.setText(gi.uploader);
            // Set category
            TextView category = (TextView) convertView.findViewById(R.id.category);
            String newText = Ui.getCategoryText(gi.category);
            if (!newText.equals(category.getText())) {
                category.setText(newText);
                category.setBackgroundColor(Ui.getCategoryColor(gi.category));
            }
            // Set star
            RatingView rate = (RatingView) convertView
                    .findViewById(R.id.rate);
            rate.setRating(gi.rating);
            // set posted
            TextView posted = (TextView)convertView.findViewById(R.id.posted);
            posted.setText(gi.posted);
            // Set simple language
            TextView simpleLanguage = (TextView)convertView.findViewById(R.id.simple_language);
            if (gi.simpleLanguage == null) {
                simpleLanguage.setVisibility(View.GONE);
            } else {
                simpleLanguage.setVisibility(View.VISIBLE);
                simpleLanguage.setText(gi.simpleLanguage);
            }
            return convertView;
        }
    }
}
