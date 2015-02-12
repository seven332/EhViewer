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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.GalleryListUrlBuilder;
import com.hippo.ehviewer.ui.fragment.GalleryListFragment;
import com.hippo.ehviewer.ui.fragment.SearchFragment;
import com.hippo.scene.Scene;
import com.hippo.widget.Appbar;

public class GalleryListScene extends Scene implements SearchFragment.OnSearchListener {

    private Appbar mAppbar;
    private ViewPager mViewPager;

    private GalleryListPagerAdapter mAdapter;

    @Override
    public void onCreate() {
        setContentView(R.layout.scene_gallery_list);

        mAppbar = (Appbar) findViewById(R.id.appbar);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);

        mAdapter = new GalleryListPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mAdapter);

        mAppbar.setTitle("EhViewer");
    }

    @Override
    public void onResume() {
        // Empty
    }

    @Override
    public void onPause() {
        dispatchRemove();
    }

    @Override
    public void onDestroy() {
        dispatchRemove();
    }

    @Override
    public void onSearch(GalleryListUrlBuilder glub) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    public class GalleryListPagerAdapter extends FragmentPagerAdapter {

        public GalleryListPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int i) {
            if (i == 0) {
                SearchFragment fragment = new SearchFragment();
                fragment.setOnSearchListener(GalleryListScene.this);
                return fragment;
            } else {
                return new GalleryListFragment();
            }
        }
    }
}
