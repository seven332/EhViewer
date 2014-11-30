/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.SearchRecentSuggestions;

public class SuggestionHelper extends SearchRecentSuggestions {

    private static final String QUERY = "query";
    private static final String ORDER_BY = "date DESC";

    private static final int MAX_HISTORY_COUNT = 250;

    private final List<String> mQueryList = new ArrayList<String>();

    private static Map<String, SuggestionHelper> sInstances = new HashMap<String, SuggestionHelper>();

    public static SuggestionHelper getInstance(Context context, String authority, int mode) {
        SuggestionHelper s = sInstances.get(authority);
        if (s == null) {
            s = new SuggestionHelper(context, authority, mode);
            sInstances.put(authority, s);
        }
        return s;
    }

    private SuggestionHelper(Context context, String authority, int mode) {
        super(context, authority, mode);

        Uri suggestionsUri = Uri.parse("content://" + authority + "/suggestions");

        // Get all suggestion
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(suggestionsUri, new String[]{QUERY}, null, null, ORDER_BY);
        while(cursor.moveToNext())
            mQueryList.add(cursor.getString(cursor.getColumnIndex(QUERY)));
        cursor.close();
    }

    public List<String> getQueries() {
        return mQueryList;
    }

    @Override
    public void saveRecentQuery(final String queryString, final String line2) {
        super.saveRecentQuery(queryString, line2);
        if (!mQueryList.contains(queryString))
            mQueryList.add(0, queryString);
        truncateHistory(MAX_HISTORY_COUNT);
    }

    @Override
    public void clearHistory() {
        super.clearHistory();
        truncateHistory(0);
    }

    private void truncateHistory(int maxEntries) {
        if (maxEntries == 0) {
            mQueryList.clear();
        } else {
            for (int i = mQueryList.size() - 1; i >= maxEntries; i--) {
                mQueryList.remove(i);
            }
        }
    }
}
