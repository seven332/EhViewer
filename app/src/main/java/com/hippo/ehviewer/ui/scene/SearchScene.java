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

package com.hippo.ehviewer.ui.scene;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.ListUrlBuilder;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Scene;

public class SearchScene extends Scene implements SearchLayout.SearhLayoutHelper,
        Scene.ActivityResultListener{

    private ContentActivity mActivity;
    private Resources mResources;

    private SearchLayout mSearchLayout;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scene_search);

        mActivity = (ContentActivity) getStageActivity();
        mResources = mActivity.getResources();

        mSearchLayout = (SearchLayout) findViewById(R.id.search_layout);

        mSearchLayout.setHelper(this);
    }

    protected void onGetFitPaddingBottom(int b) {
        mSearchLayout.setFitPaddingBottom(b);
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
}
