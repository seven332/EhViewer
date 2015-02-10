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

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.GalleryListUrlBuilder;

public class SearchFragment extends Fragment {

    private static final String VIEW_STATE_TAG = "android:view_state";

    private Activity mActivity;

    private OnSearchListener mOnSearchListener;

    private View mRootView;

    public void setOnSearchListener(OnSearchListener listener) {
        mOnSearchListener = listener;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null && mRootView != null) {
            SparseArray<Parcelable> savedStates = savedInstanceState.getSparseParcelableArray(VIEW_STATE_TAG);
            if (savedStates != null) {
                mRootView.restoreHierarchyState(savedStates);
            }
        }
    }

    public View onCreateView(LayoutInflater inflater,ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = getActivity();

        mRootView = inflater.inflate(R.layout.fragment_search, container, false);

        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mRootView = null;
    }

    public interface OnSearchListener {
        public void onSearch(GalleryListUrlBuilder glub);
    }
}
