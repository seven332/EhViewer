/*
 * Copyright 2016 Hippo Seven
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.R;
import com.hippo.view.ViewTransition;
import com.hippo.yorozuya.ViewUtils;

public class DictActivity extends ToolbarActivity {

    private static final int REQUEST_CODE_SELECT_DICT_FILE = 0;

    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private ViewTransition mViewTransition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dict);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);

        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(this, R.id.recycler_view);
        TextView tip = (TextView) ViewUtils.$$(this, R.id.tip);
        mViewTransition = new ViewTransition(mRecyclerView, tip);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRecyclerView = null;
        mViewTransition = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_dict, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_import:
                Intent intent = new Intent();
                intent.setType("application/octet-stream");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.select_dict_file)), REQUEST_CODE_SELECT_DICT_FILE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CODE_SELECT_DICT_FILE == requestCode) {
            if (Activity.RESULT_OK != resultCode) {
                return;
            }
            Uri uri = data.getData();
            if (null == uri) {
                return;
            }
            Intent intent = new Intent(this, DictImportActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            startActivity(intent);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
