/*
 * Copyright (C) 2014-2015 Hippo Seven
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.ListUrlBuilder;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Scene;
import com.hippo.scene.TransitionCurtain;
import com.hippo.widget.Appbar;

public class GalleryListScene extends Scene implements SearchLayout.SearhLayoutHelper,
        Scene.ActivityResultListener, ContentLayout.OnGetFitPaddingListener {

    private final static int PAGE_INDEX_SEARCH = 0;
    private final static int PAGE_INDEX_LIST = 1;
    private final static int PAGE_NUMBER = 2;

    private ContentActivity mActivity;
    private Resources mResources;

    private View mSearchView;
    private SearchLayout mSearchLayout;

    private View mListView;

    private Appbar mAppbar;
    private ViewPager mViewPager;

    private PagerAdapter mPagerAdapter;

    @SuppressLint("InflateParams")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = (ContentActivity) getStageActivity();
        mResources = mActivity.getResources();

        LayoutInflater inflater = mActivity.getLayoutInflater();

        // Search View
        mSearchView = inflater.inflate(R.layout.fragment_search, null);
        mSearchLayout = (SearchLayout) mSearchView.findViewById(R.id.search_layout);

        mSearchLayout.setHelper(this);

        // List View
        // TODO
        mListView = inflater.inflate(R.layout.fragment_list, null);

        Button bb = (Button) mListView.findViewById(R.id.test_button);
        bb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScene(TestScene.class, null, new TransitionCurtain(new TransitionCurtain.ViewPair[] {
                        new TransitionCurtain.ViewPair(R.id.haha, R.id.haha),
                        new TransitionCurtain.ViewPair(R.id.bbbbbb, R.id.bbbbbb)
                }));
            }
        });

        // Main View
        setContentView(R.layout.scene_gallery_list);
        mAppbar = (Appbar) findViewById(R.id.appbar);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);

        mAppbar.setTitle(mResources.getString(R.string.app_name));

        mPagerAdapter = new SimplePagerAdapter();
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(PAGE_INDEX_LIST);

        // Fit Padding Bottom
        mActivity.setOnGetFitPaddingListener(this);
        int fitPaddingBottom = mActivity.getFitPaddingBottom();
        if (fitPaddingBottom != -1) {
            setFitPaddingBottom(fitPaddingBottom);
        }
    }

    private void setFitPaddingBottom(int fitPaddingBottom) {
        mSearchLayout.setFitPaddingBottom(fitPaddingBottom);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull SparseArray<Parcelable> savedStates) {
        super.onRestoreInstanceState(savedStates);

        // mSearchView and mListView may not in hierarchy
        mSearchView.restoreHierarchyState(savedStates);
        mListView.restoreHierarchyState(savedStates);
    }

    @Override
    public void onRequestSearch(ListUrlBuilder lub) {
        // TODO
    }

    @Override
    public void onRequestSelectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, this);
    }

    @Override
    public void onGetResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = mActivity.getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();

            if (imagePath != null) {
                mSearchLayout.onSelectImage(imagePath);
            }
        }
    }

    @Override
    public void onGetFitPadding(int l, int t, int r, int b) {
        setFitPaddingBottom(b);
    }

    private class SimplePagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return PAGE_NUMBER;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup view, int position) {
            View v;
            switch (position) {
                default:
                case PAGE_INDEX_SEARCH:
                    v = mSearchView;
                    break;
                case PAGE_INDEX_LIST:
                    v = mListView;
                    break;
            }
            view.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup view, int position, Object object) {
            view.removeView((View) object);
        }
    }
}
