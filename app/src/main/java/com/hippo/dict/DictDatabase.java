package com.hippo.dict;

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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.JsonReader;

import com.hippo.util.SqlUtils;
import com.hippo.yorozuya.Say;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;


public class DictDatabase {

    private static final String TAG = DictDatabase.class.getSimpleName();

    public static final String COLUMN_PARENT = "parent";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_DICT = "dict";

    private static final String DATABASE_NAME = "search_database.db";
    private static final String TABLE_DICT = "dict";
    private static final String SEPARATOR = "@@@";
    private SQLiteDatabase mDatabase;

    private static DictDatabase sInstance;

    public static DictDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DictDatabase(context.getApplicationContext());
        }
        return sInstance;
    }

    private DictDatabase(Context context) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        mDatabase = databaseHelper.getWritableDatabase();
    }

    public String[] getSuggestions(String prefix) {
        List<String> queryList = new LinkedList<>();

        // TODO add limit
        if (TextUtils.isEmpty(prefix)) {
            return queryList.toArray(new String[queryList.size()]);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_DICT);
        sb.append(" WHERE ").append(COLUMN_DATA).append(" LIKE '")
                .append("%").append(SEPARATOR)
                .append(SqlUtils.sqlEscapeString(prefix))
                .append(SEPARATOR).append("%'")
                .append(" LIMIT 5");
        Cursor cursor = mDatabase.rawQuery(sb.toString(), null);

        int queryIndex = cursor.getColumnIndex(COLUMN_DATA);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String data = cursor.getString(queryIndex);
                String datas[] = data.split(SEPARATOR);
                queryList.add(data);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return queryList.toArray(new String[queryList.size()]);
    }

    public void importDict(final String dictPath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(dictPath);
        JsonReader jsonReader = new JsonReader(new InputStreamReader(
                fileInputStream, "UTF-8"));
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            Say.d(TAG, jsonReader.nextString());
        }
        jsonReader.endObject();
        jsonReader.close();
    }

    public void addQuery(String data, String parent, String dict) {
        // Delete old first
        deleteQuery(data);
        // Add it to database
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA, data);
        values.put(COLUMN_PARENT, parent);
        values.put(COLUMN_DICT, dict);
        mDatabase.insert(TABLE_DICT, null, values);
    }

    public void deleteQuery(final String query) {
        // mDatabase.delete(TABLE_DICT, COLUMN_QUERY + "=?", new String[]{query});
    }

    public void clearQuery() {
        truncateHistory(0);
    }

    /**
     * Reduces the length of the history table, to prevent it from growing too large.
     *
     * @param maxEntries Max entries to leave in the table. 0 means remove all entries.
     */
    protected void truncateHistory(int maxEntries) {
        if (maxEntries < 0) {
            throw new IllegalArgumentException();
        }

        try {
            // null means "delete all".  otherwise "delete but leave n newest"
            String selection = null;
            if (maxEntries > 0) {
                selection = "_id IN " +
                        "(SELECT _id FROM " + TABLE_DICT +
                        " ORDER BY " + COLUMN_DATA + " DESC" +
                        " LIMIT -1 OFFSET " + String.valueOf(maxEntries) + ")";
            }
            mDatabase.delete(TABLE_DICT, selection, null);
        } catch (RuntimeException e) {
            Say.e(TAG, "truncateHistory", e);
        }
    }


    /**
     * Builds the database.  This version has extra support for using the version field
     * as a mode flags field, and configures the database columns depending on the mode bits
     * (features) requested by the extending class.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_DICT + " (" +
                    "_id INTEGER PRIMARY KEY" +
                    "," + COLUMN_DATA + " TEXT" +
                    "," + COLUMN_PARENT + " TEXT" +
                    "," + COLUMN_DICT + " TEXT" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DICT);
            onCreate(db);
        }
    }
}
