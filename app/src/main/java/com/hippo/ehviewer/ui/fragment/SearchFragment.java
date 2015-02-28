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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.ListUrlBuilder;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Scene;

public class SearchFragment extends Fragment implements SearchLayout.SearhLayoutHelper,
        Scene.ActivityResultListener {

    private static final String VIEW_STATE_TAG = "android:view_state";

    private ContentActivity mActivity;

    private View mRootView;
    private SearchLayout mSearchLayout;

    private static Scene sScene;
    private static OnSearchListener sOnSearchListener;


    public static void setScene(Scene scene) {
        sScene = scene;
    }

    public static void setOnSearchListener(OnSearchListener listener) {
        sOnSearchListener = listener;
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
        mActivity = (ContentActivity) getActivity();

        mRootView = inflater.inflate(R.layout.fragment_search, container, false);
        mSearchLayout = (SearchLayout) mRootView.findViewById(R.id.search_layout);

        mSearchLayout.setHelper(this);

        // Set fit padding bottom
        int fixPaddingBottom = mActivity.getFitPaddingBottom();
        if (fixPaddingBottom != -1) {
            mSearchLayout.setFitPaddingBottom(mActivity.getFitPaddingBottom());
        }

        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mRootView = null;
    }

    @Override
    public void onRequestSelectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        sScene.startActivityForResult(intent, this);
    }

    @Override
    public void onRequestSearch(ListUrlBuilder lub) {
        if (sOnSearchListener != null) {
            sOnSearchListener.onRequestSearch(lub);
        }
    }

    @Override
    public void onGetResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = mActivity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();

            if (imagePath != null) {
                mSearchLayout.onSelectImage(imagePath);
            }
        }
    }

    public interface OnSearchListener {
        public void onRequestSearch(ListUrlBuilder lub);
    }
}
