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

import android.view.View;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.QuickSearch;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.scene.Announcer;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.yorozuya.Messenger;

import java.util.List;

public class QuickSearchScene extends AbsDragSortScene implements Messenger.Receiver {

    private boolean mChanged = false;

    @Override
    protected void onInit() {
        super.onInit();

        Messenger.getInstance().register(Constants.MESSENGER_ID_UPDATE_QUICK_SEARCH_NAME, this);
    }

    @Override
    protected void onDie() {
        super.onDie();

        Messenger.getInstance().unregister(Constants.MESSENGER_ID_UPDATE_QUICK_SEARCH_NAME, this);
    }

    @Override
    protected void onDestroy(boolean die) {
        super.onDestroy(die);

        // notify update
        if (mChanged) {
            Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_QUICK_SEARCH, null);
        }
    }

    @Override
    protected String getTitle() {
        return getStageActivity().getResources().getString(R.string.quick_search);
    }

    @Override
    protected SortAdapter getAdapter() {
        return new QuickSearchAdapter(DBUtils.getAllQuickSearch());
    }

    @Override
    public void onReceive(int id, Object obj) {
        if (Constants.MESSENGER_ID_UPDATE_QUICK_SEARCH_NAME == id) {
            notifyDataSetChanged();
        }
    }

    private class QuickSearchAdapter extends SortAdapter<QuickSearch> {

        public QuickSearchAdapter(List<QuickSearch> list) {
            super(list);
        }

        @Override
        public String getString(QuickSearch quickSearch, int position) {
            return quickSearch.name;
        }

        @Override
        public long getId(QuickSearch quickSearch) {
            return quickSearch.id;
        }

        @Override
        protected void onMove(int fromPosition, int toPosition) {
            mChanged = true;
            DBUtils.moveQuickSearch(getData(fromPosition).id, getData(toPosition).id);
        }

        @Override
        protected void onRemove(int position) {
            mChanged = true;
            DBUtils.removeQuickSearch(getData(position).id);
        }

        @Override
        public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
            Announcer announcer = new Announcer();
            announcer.putExtra(ModifyQuickSearchScene.KEY_QUICK_SEARCH, getData(position));
            startScene(ModifyQuickSearchScene.class, announcer);
            return true;
        }
    }
}
