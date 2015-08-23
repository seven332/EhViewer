/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.ehviewer.ui.scene;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.DownloadLabel;
import com.hippo.ehviewer.service.DownloadManager;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.scene.AppbarScene;
import com.hippo.scene.SimpleDialog;
import com.hippo.widget.FloatLabelEditText;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.yorozuya.Messenger;

import java.util.List;

public class DownloadLabelScene extends AbsDragSortScene implements AppbarScene.OnClickActionListener {

    private List<DownloadLabel> mDownloadLabels;

    private boolean mChanged = false;

    private static final int POSITION_ADD = 0;

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setTipText("No download label"); // TODO hardcode

        addAction(R.drawable.ic_plus_dark_x24);

        setOnClickActionListener(this);
    }

    @Override
    protected void onDestroy(boolean die) {
        super.onDestroy(die);

        // notify update
        if (mChanged) {
            //Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD_LABEL, null);
            mChanged = false;
        }
    }

    @Override
    protected String getTitle() {
        return "Download label"; // TODO hardcode
    }

    @Override
    protected SortAdapter getAdapter() {
        mDownloadLabels = DBUtils.getAllDownloadLabelWithId();
        return new DownloadTagAdapter(mDownloadLabels);
    }


    private class AddDownloadTagHelper implements SimpleDialog.OnCreateCustomViewListener,
            SimpleDialog.OnClickListener {

        private EditText mEditText;

        @Override
        public void onCreateCustomView(final SimpleDialog dialog, View view) {
            FloatLabelEditText floatLabelEditText = (FloatLabelEditText) view.findViewById(R.id.float_label_edit_text);
            floatLabelEditText.setHint("Download label"); // TODO hardcode
            mEditText = (EditText) view.findViewById(R.id.edit_text);
            mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                        dialog.pressPositiveButton();
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        private boolean containLabel(String label) {
            for (DownloadLabel downloadLabel : mDownloadLabels) {
                if (downloadLabel.label.equals(label)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onClick(SimpleDialog dialog, int which) {
            if (which == SimpleDialog.POSITIVE) {
                String label = mEditText.getText().toString().trim();
                if (TextUtils.isEmpty(label)) {
                    Toast.makeText(getStageActivity(), "Empty label", Toast.LENGTH_SHORT).show(); // TODO hardcode
                    return false;
                } else if (containLabel(label)) {
                    Toast.makeText(getStageActivity(), "The label already exists", Toast.LENGTH_SHORT).show(); // TODO hardcode
                    return false;
                } else {
                    // Tell DownloadManager
                    DownloadManager.DownloadLabelModify modify = new DownloadManager.DownloadLabelModify();
                    modify.ops = DownloadManager.DownloadLabelModify.OPS_ADD;
                    modify.value = label;
                    Messenger.getInstance().notifyAtOnce(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_SCENE, modify);

                    // Update data
                    mDownloadLabels.clear();
                    mDownloadLabels.addAll(DBUtils.getAllDownloadLabelWithId());
                    notifyDataSetChanged();

                    mChanged = true;
                    return true;
                }
            } else {
                return true;
            }
        }
    }

    @Override
    public void onClickAction(int index) {
        switch (index) {
            case POSITION_ADD:
                AddDownloadTagHelper helper = new AddDownloadTagHelper();
                new SimpleDialog.Builder(getContext()).setTitle("Add download tag")
                        .setCustomView(R.layout.dialog_edit_text, helper)
                        .setPositiveButton(android.R.string.ok)
                        .setOnButtonClickListener(helper).show(this);
                break;
        }
    }

    private class DownloadTagAdapter extends SortAdapter<DownloadLabel> {

        public DownloadTagAdapter(List<DownloadLabel> list) {
            super(list);
        }

        @Override
        public String getString(DownloadLabel downloadLabel, int position) {
            return downloadLabel.label;
        }

        @Override
        public long getId(DownloadLabel downloadLabel) {
            return downloadLabel.id;
        }

        @Override
        protected void onMove(int fromPosition, int toPosition) {
            DownloadManager.DownloadLabelModify modify = new DownloadManager.DownloadLabelModify();
            modify.ops = DownloadManager.DownloadLabelModify.OPS_MOVE;
            modify.label = getData(fromPosition);
            modify.label2 = getData(toPosition);
            Messenger.getInstance().notify(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_SCENE, modify);

            mChanged = true;
        }

        @Override
        protected void onRemove(int position) {
            DownloadManager.DownloadLabelModify modify = new DownloadManager.DownloadLabelModify();
            modify.ops = DownloadManager.DownloadLabelModify.OPS_REMOVE;
            modify.label = getData(position);
            Messenger.getInstance().notify(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_SCENE, modify);

            mChanged = true;
        }

        @Override
        public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
            // TODO
            return false;
        }
    }
}
