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

package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.GalleryListUrlBuilder;

public class SearchFragment extends Fragment {

    private OnSearchListener mOnSearchListener;

    public void setOnSearchListener(OnSearchListener listener) {
        mOnSearchListener = listener;
    }

    public View onCreateView(LayoutInflater inflater,ViewGroup container,
            Bundle savedInstanceState) {

        View mRootView = inflater.inflate(R.layout.fragment_search, container, false);
        return mRootView;
    }

    public interface OnSearchListener {
        public void onSearch(GalleryListUrlBuilder glub);
    }
}
