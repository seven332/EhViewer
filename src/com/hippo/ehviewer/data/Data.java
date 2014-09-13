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

package com.hippo.ehviewer.data;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseArray;

import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.SqlUtils;


public class Data {
    private static final String TAG = "Data";

    private static final int VERSION = 2;
    private static final String DB_NAME = "data";

    private static final String TABLE_GALLERY = "gallery";
    private static final String TABLE_LOCAL_FAVOURITE = "local_favourite";
    private static final String TABLE_TAG = "tag";
    private static final String TABLE_DOWNLOAD = "download";

    private static final String COLUMN_GID = "gid";
    private static final String COLUMN_TOKEN = "token";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_POSTED = "posted";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_THUMB = "thumb";
    private static final String COLUMN_UPLOADER = "uploader";
    private static final String COLUMN_RATING = "rating";
    private static final String COLUMN_REFERENCE = "reference";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_MODE = "mode";
    private static final String COLUMN_SEARCH = "search";
    private static final String COLUMN_ADVANCE = "advance";
    private static final String COLUMN_MIN_RATING = "min_rating";
    private static final String COLUMN_TAG = "tag";

    private static final String COLUMN_STATE = "state";
    private static final String COLUMN_LEGACY = "legacy";

    private final SparseArray<GalleryInfo> mGallerys;

    private long mTagRowNum;
    private List<Tag> mTags;
    private List<GalleryInfo> mLocalFavourites;
    private List<DownloadInfo> mDownloads;

    private final Context mContext;

    private final DBHelper mDBHelper;
    private final SQLiteDatabase mDatabase;

    private static Data sInstance;

    public static void createInstance(Context context) {
        sInstance = new Data(context);
    }

    public static Data getInstance() {
        return sInstance;
    }

    public Data(Context context) {
        mContext = context;
        mDBHelper = new DBHelper(mContext);
        mDatabase = mDBHelper.getWritableDatabase();

        mGallerys = new SparseArray<GalleryInfo>();

        getTags();
        getLocalFavourites();
        getDownloads();
    }

    @Override
    public void finalize() {
        mDBHelper.close();
    }

    /****** gallery ******/

    /**
     * Get galleryInfo in table, if do not exits return null
     *
     * @param gid
     * @return
     */
    public GalleryInfo getGallery(int gid) {
        GalleryInfo galleryInfo = mGallerys.get(gid);
        if (galleryInfo == null) {
            Cursor cursor = mDatabase.rawQuery("select * from " + TABLE_GALLERY + " where " + COLUMN_GID + "=?",
                    new String[]{String.valueOf(gid)});
            if (cursor.moveToFirst()) {
                galleryInfo = new GalleryInfo();
                galleryInfo.gid = gid;
                galleryInfo.token = cursor.getString(1);
                galleryInfo.title = cursor.getString(2);
                galleryInfo.posted = cursor.getString(3);
                galleryInfo.category = cursor.getInt(4);
                galleryInfo.thumb = cursor.getString(5);
                galleryInfo.uploader = cursor.getString(6);
                galleryInfo.rating = cursor.getFloat(7);
                galleryInfo.generateSLang();

                // add to map
                mGallerys.put(gid, galleryInfo);
            }
            cursor.close();
        }
        return galleryInfo;
    }

    @SuppressWarnings("unused")
    private synchronized boolean containsGallery(int gid) {
        boolean re = false;
        Cursor cursor = mDatabase.rawQuery("select * from "
                + TABLE_GALLERY + " where " + COLUMN_GID + "=?",
                new String[]{String.valueOf(gid)});
        if (cursor.moveToFirst())
            re = true;
        cursor.close();
        return re;
    }

    /**
     * If do not contain the gallery return -1;
     * @param gid
     * @return
     */
    private synchronized int getGalleryReference(int gid) {
        int reference = -1;
        Cursor cursor = mDatabase.rawQuery("select " + COLUMN_REFERENCE + " from "
                + TABLE_GALLERY + " where " + COLUMN_GID + "=?",
                new String[]{String.valueOf(gid)});
        if (cursor.moveToFirst()) {
            reference = cursor.getInt(0);
        }
        cursor.close();
        return reference;
    }

    /**
     *
     * If exits same gid, add the reference by 1.
     * If exits no same gid, add it to table reference is 1
     *
     * @param galleryInfo
     */
    private void addGallery(GalleryInfo galleryInfo) {
        boolean isInsert;
        int gid = galleryInfo.gid;
        // add to map
        mGallerys.put(gid, galleryInfo);

        // add to sql or update and add reference by 1
        int reference;
        if ((reference = getGalleryReference(gid)) != -1) {
            isInsert = false;
            reference++;
        }
        else {
            isInsert = true;
            reference = 1;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_GID, gid);
        values.put(COLUMN_TOKEN, galleryInfo.token);
        values.put(COLUMN_TITLE, galleryInfo.title);
        values.put(COLUMN_POSTED, galleryInfo.posted);
        values.put(COLUMN_CATEGORY, galleryInfo.category);
        values.put(COLUMN_THUMB, galleryInfo.thumb);
        values.put(COLUMN_UPLOADER, galleryInfo.uploader);
        values.put(COLUMN_RATING, galleryInfo.rating);
        values.put(COLUMN_REFERENCE, reference);

        if (isInsert) { // Set
            mDatabase.insert(TABLE_GALLERY, null, values);
        } else { // Update
            mDatabase.update(TABLE_GALLERY, values, COLUMN_GID + "=?", new String[]{String.valueOf(gid)});
        }
    }

    /**
     * True if update
     *
     * @param gid
     * @return
     */
    private boolean updateGallery(GalleryInfo galleryInfo) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_GID, galleryInfo.gid);
        values.put(COLUMN_TOKEN, galleryInfo.token);
        values.put(COLUMN_TITLE, galleryInfo.title);
        values.put(COLUMN_POSTED, galleryInfo.posted);
        values.put(COLUMN_CATEGORY, galleryInfo.category);
        values.put(COLUMN_THUMB, galleryInfo.thumb);
        values.put(COLUMN_UPLOADER, galleryInfo.uploader);
        values.put(COLUMN_RATING, galleryInfo.rating);

        return mDatabase.update(TABLE_GALLERY, values,
                COLUMN_GID + "=?", new String[]{String.valueOf(galleryInfo.gid)}) != 0;
    }

    /**
     * decrease reference by 1, only delete when reference is 0
     * @param gid
     */
    private boolean deleteGallery(int gid) {
        int reference = getGalleryReference(gid);
        if (reference == -1) {
            Log.w(TAG, "Can't get reference when deleteGallery, gid is " + gid);
            return false;
        }
        reference--;
        if (reference == 0) { // delete gallery
            // delete in sql
            mDatabase.delete(TABLE_GALLERY, COLUMN_GID + "=?", new String[]{String.valueOf(gid)});
            // delete in list
            mGallerys.remove(gid);
        } else { // update reference
            ContentValues values = new ContentValues();
            values.put(COLUMN_REFERENCE, reference);
            mDatabase.update(TABLE_GALLERY, values, COLUMN_GID + "=?", new String[]{String.valueOf(gid)});
        }
        return true;
    }

    /****** download ******/
    private synchronized void getDownloads() {
        mDownloads = new ArrayList<DownloadInfo>();

        Cursor cursor = mDatabase.rawQuery("select * from "
                + TABLE_DOWNLOAD, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {

                int gid = cursor.getInt(0);
                GalleryInfo galleryInfo = getGallery(gid);
                if (galleryInfo == null) {
                    cursor.moveToNext();
                    continue;
                }
                int mode = cursor.getInt(1);
                int state = cursor.getInt(2);
                int legacy = cursor.getInt(3);
                if (state == DownloadInfo.STATE_WAIT || state == DownloadInfo.STATE_DOWNLOAD)
                    state = DownloadInfo.STATE_NONE;

                DownloadInfo downloadInfo = new DownloadInfo(
                        galleryInfo, mode, state, legacy);
                mDownloads.add(downloadInfo);

                cursor.moveToNext();
            }
        }
        cursor.close();
    }

    /**
     * Return null if not found
     * @return
     */
    public synchronized DownloadInfo getFirstWaitDownloadInfo() {
        for (DownloadInfo di : mDownloads) {
            if (di.state == DownloadInfo.STATE_WAIT)
                return di;
        }
        return null;
    }

    /**
     * True if contain this item
     *
     * @param gid
     * @return
     */
    public synchronized boolean containsDownload(int gid) {
        boolean re = false;
        Cursor cursor = mDatabase.rawQuery("select * from "
                + TABLE_DOWNLOAD + " where " + COLUMN_GID + "=?",
                new String[]{String.valueOf(gid)});
        if (cursor.moveToFirst())
            re = true;
        cursor.close();
        return re;
    }

    public synchronized boolean addDownload(DownloadInfo downloadInfo) {
        GalleryInfo galleryInfo = downloadInfo.galleryInfo;
        int gid = galleryInfo.gid;

        // Add to download
        boolean update;
        if (containsDownload(gid)) {
            update = true;
        } else {
            update = false;
            addGallery(galleryInfo);
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_GID, gid);
        values.put(COLUMN_MODE, downloadInfo.mode);
        values.put(COLUMN_STATE, downloadInfo.state);
        values.put(COLUMN_LEGACY, downloadInfo.legacy);

        if (update) {
            mDatabase.update(TABLE_DOWNLOAD, values,
                    COLUMN_GID + "=?", new String[]{String.valueOf(galleryInfo.gid)});
            return false;
        } else {
            if (mDatabase.insert(TABLE_DOWNLOAD, null,
                    values) != -1) { // If add ok
                mDownloads.add(downloadInfo);
                return true;
            } else { // If add fail
                deleteGallery(gid);
                return false;
            }
        }
    }

    public synchronized boolean deleteDownload(int gid) {
        int deleteNum = mDatabase.delete(TABLE_DOWNLOAD, COLUMN_GID + "=?", new String[]{String.valueOf(gid)});
        if (deleteNum > 1)
            Log.w(TAG, "WTF? more than one gid is " + gid);

        if (deleteNum == 0)
            return false;

        // delete from list
        for (DownloadInfo di : mDownloads) {
            if (gid == di.galleryInfo.gid) {
                mDownloads.remove(di);
                break;
            }
        }

        // sub reference
        deleteGallery(gid);

        return true;
    }

    public synchronized List<DownloadInfo> getAllDownloads() {
        return mDownloads;
    }

    /**
     * Return null if not found
     *
     * @param gid
     * @return
     */
    public synchronized DownloadInfo getDownload(int gid) {
        for (DownloadInfo di : mDownloads) {
            if (gid == di.galleryInfo.gid)
                return di;
        }
        return null;
    }

    /****** local favourite ******/
    private synchronized void getLocalFavourites() {
        mLocalFavourites = new ArrayList<GalleryInfo>();
        Cursor cursor = mDatabase.rawQuery("select * from "
                + TABLE_LOCAL_FAVOURITE, null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int gid = cursor.getInt(0);
                GalleryInfo galleryInfo = getGallery(gid);
                if (galleryInfo != null)
                    mLocalFavourites.add(galleryInfo);
                cursor.moveToNext();
            }
        }
        cursor.close();
    }

    public synchronized boolean containsLocalFavourite(int gid) {
        boolean re = false;
        Cursor cursor = mDatabase.rawQuery("select * from "
                + TABLE_LOCAL_FAVOURITE + " where " + COLUMN_GID + "=?",
                new String[]{String.valueOf(gid)});
        if (cursor.moveToFirst())
            re = true;
        cursor.close();
        return re;
    }

    /**
     * galleryInfo can't be null
     * @param galleryInfo
     */
    public synchronized void addLocalFavourite(GalleryInfo galleryInfo) {
        int gid = galleryInfo.gid;

        if (containsLocalFavourite(gid)) {
            updateGallery(galleryInfo);
        } else {
            addGallery(galleryInfo);
            // add to sql
            ContentValues values = new ContentValues();
            values.put(COLUMN_GID, galleryInfo.gid);
            mDatabase.insert(TABLE_LOCAL_FAVOURITE, null, values);
            // add to list
            mLocalFavourites.add(galleryInfo);

            // Analytics
            Analytics.addToFavoriteGallery(mContext, gid, galleryInfo.token);
        }
    }

    public synchronized void deleteLocalFavourite(int gid) {
        // delete from list
        for (GalleryInfo  galleryInfo : mLocalFavourites) {
            if (galleryInfo.gid == gid) {
                mLocalFavourites.remove(galleryInfo);
                break;
            }
        }

        // delete from sql
        mDatabase.delete(TABLE_LOCAL_FAVOURITE, COLUMN_GID + "=?", new String[]{String.valueOf(gid)});
        deleteGallery(gid);
    }

    public synchronized List<GalleryInfo> getAllLocalFavourites() {
        return mLocalFavourites;
    }

    /******  tag ******/
    private synchronized void getTags() {
        mTags = new ArrayList<Tag>();
        mTagRowNum = 0;
        Cursor cursor = mDatabase.rawQuery("select * from " + TABLE_TAG
                + " order by " + COLUMN_ID + " asc", null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {

                String name = cursor.getString(1);
                int mode = cursor.getInt(2);
                int category = cursor.getInt(3);
                String search = cursor.getString(4);
                int advance = cursor.getInt(5);
                int min_rating = cursor.getInt(6);
                String tagStr = cursor.getString(7);

                Tag tag = new Tag(name, category, search);
                tag.setAdvance(advance, min_rating);
                tag.setTag(tagStr);
                tag.setMode(mode);

                mTags.add(tag);

                cursor.moveToNext();
                mTagRowNum++;
            }
        }
        cursor.close();
    }

    public synchronized boolean addTag(Tag tag) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, mTagRowNum);
        values.put(COLUMN_NAME, tag.getName());
        values.put(COLUMN_MODE, tag.getMode());
        values.put(COLUMN_CATEGORY, tag.getCategory());
        values.put(COLUMN_SEARCH, tag.getSearch());
        values.put(COLUMN_ADVANCE, tag.getAdvanceType());
        values.put(COLUMN_MIN_RATING, tag.getMinRating());
        values.put(COLUMN_TAG, tag.getTag());

        boolean ok = mDatabase.insert(
                TABLE_TAG, null, values) != -1;
        if (ok) {
            mTagRowNum++;
            mTags.add(tag);
        }
        return ok;
    }

    public synchronized boolean deleteTag(int id) {
        int deleteNum = mDatabase.delete(TABLE_TAG, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        if (deleteNum > 1)
            Log.e(TAG, "WTF? more than one id is " + id);

        if (deleteNum == 0)
            return false;
        if (id < 0 || id >= mTags.size()) {
            Log.e(TAG, "id is out of bounds, id is " + id + ", mTags.size() is " + mTags.size());
            return false;
        }

        // Update sql
        ContentValues values = new ContentValues();
        for (int i = id + 1; i < mTagRowNum; i++) {
            values.put(COLUMN_ID, i - 1);
            mDatabase.update(TABLE_TAG, values, COLUMN_ID + "=?", new String[]{String.valueOf(i)});
        }
        mTagRowNum -= deleteNum;

        // Update list
        mTags.remove(id);

        return true;
    }

    public synchronized List<Tag> getAllTags() {
        return mTags;
    }

    public synchronized Tag getTag(int location) {
        return mTags.get(location);
    }

    public synchronized List<String> getAllTagNames() {
        List<String> names = new ArrayList<String>();
        for (Tag tag : mTags)
            names.add(tag.getName());
        return names;
    }

    /**
     * location >= 0 && location <= tagRowNum please
     * @param location
     * @param tag
     */
    public synchronized void setTag(int location, Tag tag) {
        if (location == mTagRowNum) {
            addTag(tag);
        } else {
            // Update list
            mTags.set(location, tag);

            // Update sql
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, tag.getName());
            values.put(COLUMN_CATEGORY, tag.getCategory());
            values.put(COLUMN_SEARCH, tag.getSearch());
            values.put(COLUMN_ADVANCE, tag.getAdvanceType());
            values.put(COLUMN_MIN_RATING, tag.getMinRating());
            mDatabase.update(TABLE_TAG, values, COLUMN_ID + "=?", new String[]{String.valueOf(location)});
        }
    }

    public synchronized boolean swapTag(int indexOne, int indexTwo) {
        if (indexOne < 0 || indexOne > mTagRowNum
                || indexTwo < 0 || indexTwo > mTagRowNum)
            return false;

        if (indexOne == indexTwo)
            return true;

        // Update list
        Tag temp = mTags.get(indexOne);
        mTags.set(indexOne, mTags.get(indexTwo));
        mTags.set(indexTwo, temp);
        // Update sql
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, -1);
        mDatabase.update(TABLE_TAG, values, COLUMN_ID + "=?", new String[]{String.valueOf(indexOne)});
        values.put(COLUMN_ID, indexOne);
        mDatabase.update(TABLE_TAG, values, COLUMN_ID + "=?", new String[]{String.valueOf(indexTwo)});
        values.put(COLUMN_ID, indexTwo);
        mDatabase.update(TABLE_TAG, values, COLUMN_ID + "=?", new String[]{String.valueOf(-1)});
        return true;
    }

    public class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DB_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // gallery
            String CreateGallery = "create table " + TABLE_GALLERY + "("
                    + COLUMN_GID + " integer primary key,"
                    + COLUMN_TOKEN + " text,"
                    + COLUMN_TITLE + " text,"
                    + COLUMN_POSTED + " text,"
                    + COLUMN_CATEGORY + " integer,"
                    + COLUMN_THUMB + " text,"
                    + COLUMN_UPLOADER + " text,"
                    + COLUMN_RATING + " float,"
                    + COLUMN_REFERENCE + " integer);";
            db.execSQL(CreateGallery);

            // local favourite
            String CreateLocalFavourite = "create table "
                    + TABLE_LOCAL_FAVOURITE + "("
                    + COLUMN_GID + " integer primary key,"
                    + "foreign key(" + COLUMN_GID + ") references " + TABLE_GALLERY + "(" + COLUMN_GID + "));";
            db.execSQL(CreateLocalFavourite);

            // tag
            String CreateTag = "create table "
                    + TABLE_TAG + "("
                    + COLUMN_ID + " integer primary key,"
                    + COLUMN_NAME + " text,"
                    + COLUMN_MODE + " integer,"
                    + COLUMN_CATEGORY + " integer,"
                    + COLUMN_SEARCH + " text,"
                    + COLUMN_ADVANCE + " integer,"
                    + COLUMN_MIN_RATING + " integer,"
                    + COLUMN_TAG + " text);";
            db.execSQL(CreateTag);

            // download
            createDownloadTable(db);
        }

        private void createDownloadTable(SQLiteDatabase db) {
            String CreateDownload = "create table "
                    + TABLE_DOWNLOAD + "("
                    + COLUMN_GID + " integer primary key,"
                    + COLUMN_MODE + " integer,"
                    + COLUMN_STATE + " integer,"
                    + COLUMN_LEGACY + " integer,"
                    + "foreign key(" + COLUMN_GID + ") references " + TABLE_GALLERY + "(" + COLUMN_GID + "));";

            SqlUtils.exeSQLSafely(db, CreateDownload);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            switch (oldVersion) {
            case 1:
                SqlUtils.dropTable(db, TABLE_DOWNLOAD);
                SqlUtils.dropTable(db, "read");
                createDownloadTable(db);
                break;
            case VERSION:
                break;
            default:
                SqlUtils.dropAllTable(db);
                onCreate(db);
                break;
            }
        }
    }
}
