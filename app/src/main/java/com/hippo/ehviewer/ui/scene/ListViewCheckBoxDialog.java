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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import com.hippo.ehviewer.R;
import com.hippo.scene.SimpleDialog;

public class ListViewCheckBoxDialog extends SimpleDialog {

    private ListView mListView;
    private CheckBox mCheckBox;

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        ViewGroup customView = getCustomView();
        mListView = (ListView) customView.findViewById(R.id.list);
        mCheckBox = (CheckBox) customView.findViewById(R.id.check_box);

        Builder builder = (Builder) getBuilder();
        mListView.setAdapter(new ArrayAdapter<>(getStageActivity(),
                builder.mListItemLayoutResId, android.R.id.text1, builder.mListeItems));
        mListView.setOnItemClickListener(new ListItemClickListener());
        mCheckBox.setText(builder.mCheckBoxText);
    }

    public boolean isCheckBoxChecked() {
        return mCheckBox.isChecked();
    }

    public class ListItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ((Builder) getBuilder()).mOnListItemClickListener.onClick(ListViewCheckBoxDialog.this, position);
        }
    }

    public static class Builder extends SimpleDialog.Builder {

        private CharSequence[] mListeItems;
        private int mListItemLayoutResId;
        private OnClickListener mOnListItemClickListener;

        private CharSequence mCheckBoxText;

        public Builder(Context context) {
            super(context);
            setCustomView(R.layout.dialog_list_view_check_box, null);
        }

        public Builder setListItems(CharSequence[] items, final OnClickListener listener) {
            mListeItems = items;
            mListItemLayoutResId = R.layout.select_dialog_item;
            mOnListItemClickListener = listener;
            return this;
        }

        public Builder setCheckBoxText(CharSequence text) {
            mCheckBoxText = text;
            return this;
        }

        @Override
        public Class getDialogClass() {
            return ListViewCheckBoxDialog.class;
        }
    }
}
