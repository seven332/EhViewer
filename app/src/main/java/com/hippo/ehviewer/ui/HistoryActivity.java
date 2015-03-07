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
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable.Stroke;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.widget.FitWindowView;
import com.hippo.ehviewer.widget.GalleryListView;
import com.hippo.ehviewer.widget.GalleryListView.OnGetListListener;
import com.hippo.ehviewer.widget.recyclerview.EasyRecyclerView;

public class HistoryActivity extends AbsTranslucentActivity
        implements EasyRecyclerView.OnItemClickListener, MaterialAlertDialog.OnClickListener,
        GalleryListView.GalleryListViewHelper, FitWindowView.OnFitSystemWindowsListener {

    private static final String TAG = HistoryActivity.class.getSimpleName();

    public static final int LIST_MODE_DETAIL = 0;
    public static final int LIST_MODE_THUMB = 1;

    private Data mData;
    private int mThemeColor;

    private FitWindowView mStandard;
    private GalleryListView mGalleryListView;

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
                    mGalleryListView.refresh();
                }
            }
            return true;

        } else if (dialog == mClearDialog) {
            switch (which) {
            case MaterialAlertDialog.POSITIVE:
                Data.getInstance().clearHistory();
                mGalleryListView.refresh();
            }
            return true;

        }
        return true;
    }

    @SuppressLint("InflateParams")
    private void createFilterDialog() {
        View view = ViewUtils.inflateDialogView(R.layout.history_filter, false);
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
    public void onFitSystemWindows(int l, int t, int r, int b) {
        mGalleryListView.setPaddingTopAndBottom(t, b);
        Ui.colorStatusBarKK(this, mThemeColor, t - Ui.ACTION_BAR_HEIGHT);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGalleryListView.refresh();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);

        mData = Data.getInstance();
        mFilterMode = Config.getInt(KEY_HISTORY_FILTER, Data.BROWSE | Data.READ);

        // Menu
        MaterialIndicatorDrawable materialIndicator = new MaterialIndicatorDrawable(this, Color.WHITE, Stroke.THIN);
        materialIndicator.setIconState(MaterialIndicatorDrawable.IconState.ARROW);
        Ui.setMaterialIndicator(getActionBar(), materialIndicator);

        // Theme
        mThemeColor = Config.getCustomThemeColor() ? Config.getThemeColor() : Ui.THEME_COLOR;
        getActionBar().setBackgroundDrawable(new ColorDrawable(mThemeColor));
        Ui.colorStatusBarL(this, mThemeColor);

        mStandard = (FitWindowView) findViewById(R.id.standard);
        mGalleryListView = (GalleryListView) findViewById(R.id.gallery_list);

        mGalleryListView.setGalleryListViewHelper(this);
        mStandard.addOnFitSystemWindowsListener(this);

        // Do not need header an footer
        mGalleryListView.setEnabledHeader(false);
        mGalleryListView.setEnabledFooter(false);
        mGalleryListView.setOnItemClickListener(this);

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
    public String getTargetUrl(int targetPage) {
        return HISTORY_URL;
    }

    @Override
    public void doGetGallerys(String url, long taskStamp,
            OnGetListListener listener) {
        if (url.equals(HISTORY_URL)) {
            List<GalleryInfo> giList = new ArrayList<GalleryInfo>(mData.getHistory(mFilterMode, true));
            if (giList == null || giList.size() == 0)
                listener.onSuccess(taskStamp, giList, 0);
            else
                listener.onSuccess(taskStamp, giList, 1);
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view,
            int position, long id) {
        Intent intent = new Intent(this, GalleryDetailActivity.class);
        GalleryInfo gi = mGalleryListView.getGalleryInfo(position);
        intent.putExtra(GalleryDetailActivity.KEY_G_INFO, gi);
        startActivity(intent);
        return true;
    }
}
