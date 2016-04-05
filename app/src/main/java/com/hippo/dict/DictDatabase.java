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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;

import com.hippo.util.SqlUtils;
import com.hippo.yorozuya.ArrayUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class DictDatabase {

    private static final String TAG = DictDatabase.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String COLUMN_PARENT = "parent";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_DICT = "dict_name";

    private static final String DATABASE_NAME = "dict_database.db";
    private static final String TABLE_DICT = "dictionary";
    private static final String SEPARATOR = "@@@";
    private final SQLiteDatabase mDatabase;
    private static DictDatabase sInstance;

    private String mDictName;
    private boolean mAbortFlag = false;

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
        if (TextUtils.isEmpty(prefix)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        String command = "SELECT * FROM " + TABLE_DICT + " WHERE " + COLUMN_DATA +
                " MATCH '" + SqlUtils.sqlEscapeString(prefix) + "' LIMIT 5;";
        Cursor cursor = mDatabase.rawQuery(command, null);
        Set<String> queryList = new HashSet<>();
        int queryIndex = cursor.getColumnIndex(COLUMN_DATA);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String datum = cursor.getString(queryIndex);
                String data[] = datum.split(SEPARATOR);
                for (String item : data) {
                    if (TextUtils.isEmpty(item) || datum.equals(prefix)) {
                        continue;
                    }
                    queryList.add(item);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return queryList.toArray(new String[queryList.size()]);
    }

    public void importDict(final Uri dictUri, final DictImportService.ProcessListener listener) throws IOException, URISyntaxException {
        mAbortFlag = false;
        int itemNum = -1;
        File dictFile = new File(dictUri.getPath());
        FileInputStream fileInputStream = new FileInputStream(dictFile);
        JsonReader jsonReader = new JsonReader(new InputStreamReader(
                fileInputStream, "UTF-8"));

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String field = jsonReader.nextName();
            if (field.equals("dict")) {
                mDictName = jsonReader.nextString();
                if (DEBUG) {
                    Log.d(TAG, "[importDict] parse the dict name -- " + mDictName);
                }
            } else if (field.equals("num")) {
                if (TextUtils.isEmpty(mDictName)) {
                    break;
                }
                itemNum = jsonReader.nextInt();
                listener.processTotal(itemNum);
                if (DEBUG) {
                    Log.d(TAG, "[importDict] parse the item number -- " + itemNum);
                }
            } else if (field.equals("data")) {
                if (TextUtils.isEmpty(mDictName) || itemNum <= 0) {
                    break;
                }
                deleteDict(mDictName);
                parseData(jsonReader, listener);
            }
        }
        jsonReader.endObject();
        jsonReader.close();
        listener.processComplete();
    }

    public void deleteDict(String dict) {
        if (DEBUG) {
            Log.d(TAG, "[deleteDict] dict:" + dict);
        }
        mDatabase.delete(TABLE_DICT, COLUMN_DICT + "=?", new String[]{dict});
    }

    public void importAbort() {
        if (DEBUG) {
            Log.d(TAG, "[importAbort] set mAbortFlag true");
        }
        mAbortFlag = true;
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
            db.execSQL("CREATE VIRTUAL TABLE " + TABLE_DICT + " USING fts4 (" +
                    COLUMN_DATA +
                    ", " + COLUMN_PARENT +
                    ", " + COLUMN_DICT +
                    ", tokenize=icu th_ZH" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DICT);
            onCreate(db);
        }
    }

    private void parseData(JsonReader jsonReader, final DictImportService.ProcessListener listener) throws IOException {
        int process = 1;
        SQLiteStatement insStmt = mDatabase.compileStatement("INSERT INTO " + TABLE_DICT + " VALUES (?, ?, ?);");
        mDatabase.beginTransaction();
        jsonReader.beginArray();
        try {
            while (jsonReader.hasNext()) {
                synchronized (this) {
                    if (mAbortFlag) {
                        if (DEBUG) {
                            Log.d(TAG, "[parseData] import abort, delete the dict -- " + mDictName);
                        }
                        deleteDict(mDictName);
                        return;
                    }
                }

                parseSingleData(jsonReader, insStmt);
                listener.process(process);
                process++;
            }
            jsonReader.endArray();
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    private void parseSingleData(JsonReader jsonReader, SQLiteStatement insStmt) throws IOException {
        StringBuilder sb = new StringBuilder();
        String parent = "";
        sb.append(SEPARATOR);
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            String content = jsonReader.nextString();
            if (name.equals("parent")) {
                parent = content;
            } else {
                sb.append(content);
                sb.append(SEPARATOR);
            }
        }
        jsonReader.endObject();

        if (DEBUG) {
            Log.d(TAG, "[parseSingleData] item:" + parent + " " + sb.toString());
        }
        insStmt.bindString(1, sb.toString());
        insStmt.bindString(2, parent);
        insStmt.bindString(3, mDictName);
        insStmt.executeInsert();
    }
}
