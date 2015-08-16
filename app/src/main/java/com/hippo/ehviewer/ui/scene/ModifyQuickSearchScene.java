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

import android.content.res.Resources;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.data.QuickSearch;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Announcer;
import com.hippo.scene.AppbarScene;
import com.hippo.scene.Scene;
import com.hippo.scene.SimpleDialog;
import com.hippo.yorozuya.Messenger;
import com.hippo.yorozuya.ViewUtils;

public class ModifyQuickSearchScene extends AppbarScene implements SearchLayout.SearhLayoutHelper,
            SearchBar.Helper {

    public static final String KEY_QUICK_SEARCH = "quick_search";

    private SearchLayout mSearchLayout;

    private QuickSearch mQuickSearch;
    private ListUrlBuilder mListUrlBuilder;
    private SearchBar mSearchBar;

    private String mNewName;

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        setContentView(R.layout.scene_modify_quick_search);
        setIcon(R.drawable.ic_arrow_left_dark);

        Resources resources = getStageActivity().getResources();

        mSearchLayout = (SearchLayout) findViewById(R.id.search_layout);
        mSearchLayout.setSearchMode(SearchLayout.SEARCH_MODE_NORMAL, false);
        mSearchLayout.setAction1Text(resources.getString(R.string.rename));
        mSearchLayout.setAction2Text(resources.getString(android.R.string.ok));
        mSearchLayout.setHelper(this);

        mSearchBar = (SearchBar) findViewById(R.id.search_bar);
        mSearchBar.setState(SearchBar.STATE_SEARCH, false);
        mSearchBar.setHelper(this);
        mSearchBar.setEditTextHint(resources.getString(R.string.keyword));

        ViewUtils.measureView(mSearchBar, 200, ViewGroup.LayoutParams.WRAP_CONTENT);
        mSearchLayout.setFitPaddingTop(mSearchBar.getMeasuredHeight() +
                ((ViewGroup.MarginLayoutParams) mSearchBar.getLayoutParams()).topMargin);
    }

    private String getName() {
        if (!TextUtils.isEmpty(mNewName)) {
            return mNewName;
        } else {
            return mQuickSearch.name;
        }
    }

    @Override
    protected void onBind() {
        super.onBind();

        Announcer announcer = getAnnouncer();
        if (announcer != null) {
            QuickSearch quickSearch = announcer.getExtra(KEY_QUICK_SEARCH, QuickSearch.class);
            if (quickSearch != null) {
                mQuickSearch = quickSearch;
                mListUrlBuilder = new ListUrlBuilder();
                mListUrlBuilder.set(quickSearch);
                mSearchLayout.bind(mListUrlBuilder);
                mSearchBar.setText(quickSearch.keyword);
                setTitle(getName());
                return;
            }
        }

        finish();
    }

    @Override
    protected void onRestore() {
        super.onRestore();

        setTitle(getName());
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        super.onGetFitPaddingBottom(b);

        mSearchLayout.setFitPaddingBottom(b);
    }

    @Override
    public void onBackPressed() {
        if (mSearchBar.getState() == SearchBar.STATE_SEARCH_LIST) {
            mSearchBar.setState(SearchBar.STATE_SEARCH, true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onIconClick() {
        finish();
    }

    private class RenameHelper implements SimpleDialog.OnCreateCustomViewListener,
            SimpleDialog.OnClickListener {

        private EditText mEditText;

        @Override
        public void onCreateCustomView(final SimpleDialog dialog, View view) {
            mEditText = (EditText) view.findViewById(R.id.edit_text);
            mEditText.setText(getName());
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

        @Override
        public boolean onClick(SimpleDialog dialog, int which) {
            if (which == SimpleDialog.POSITIVE) {
                String displayname = mEditText.getText().toString();
                if (!TextUtils.isEmpty(displayname)) {
                    mNewName = displayname;
                    // Update title
                    setTitle(getName());
                    return true;
                } else {
                    Toast.makeText(getStageActivity(), "Empty displayname", Toast.LENGTH_SHORT).show(); // TODO hardcode
                    return false;
                }
            } else {
                return true;
            }
        }
    }


    @Override
    public void onAction1() {
        RenameHelper helper = new RenameHelper();
        new SimpleDialog.Builder(getStageActivity()).setTitle(R.string.rename)
                .setCustomView(R.layout.dialog_add_to_quick_search, helper)
                .setPositiveButton(android.R.string.ok)
                .setOnButtonClickListener(helper).show(this);
    }

    @Override
    public void onAction2() {
        mSearchLayout.formatListUrlBuilder(mListUrlBuilder);
        mListUrlBuilder.setKeyword(mSearchBar.getText());
        mQuickSearch.set(mListUrlBuilder);

        boolean newName = !TextUtils.isEmpty(mNewName);
        if (newName) {
            // New name
            mQuickSearch.name = mNewName;
        }
        DBUtils.modifyQuickSearch(mQuickSearch);

        Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_QUICK_SEARCH, null);
        if (newName) {
            // Notify
            Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_QUICK_SEARCH_NAME, null);
        }

        finish();
    }

    @Override
    public void onChangeSearchMode() {
        // No change
    }

    @Override
    public void onRequestSelectImage() {
        // No image search
    }

    @Override
    public void onSpecifyGallery(int gid, String token) {
        // No specify gallery
    }

    @Override
    public Scene getScene() {
        return this;
    }


    @Override
    public void onClickTitle() {

    }

    @Override
    public void onClickMenu() {

    }

    @Override
    public void onClickArrow() {

    }

    @Override
    public void onClickAdvanceSearch() {

    }

    @Override
    public void onSearchEditTextClick() {
        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, true);
    }

    @Override
    public void onApplySearch(String query) {

    }
}
