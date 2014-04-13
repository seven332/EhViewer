package com.hippo.ehviewer.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hippo.ehviewer.GalleryInfo;
import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.Tag;
import com.hippo.ehviewer.util.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author Hippo
 * 
 */

public class Data {
    private static final String TAG = "Data";
    
    private static final int mVersion = 1;
    private static final String DB_NAME = "data";
    
    private static final String TABLE_GALLERY = "gallery";
    private static final String TABLE_READ = "read";
    private static final String TABLE_LOCAL_FAVOURITE = "local_favourite";
    private static final String TABLE_TAG = "tag";
    
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
    private static final String COLUMN_SEARCH = "search";
    private static final String COLUMN_ADVANCE = "advance";
    private static final String COLUMN_MIN_RATING = "min_rating";
    
    private Map<String, GalleryInfo> mGallerys;
    
    private long tagRowNum;
    private List<Tag> mTags;
    
    private List<GalleryInfo> mLocalFavourites;
    
    private Context mContext;
    
    private DBHelper mDBHelper;
    private SQLiteDatabase mDatabase;
    
    public Data(Context context) {
        mContext = context;
        mDBHelper = new DBHelper(mContext);
        mDatabase = mDBHelper.getWritableDatabase();
        
        mGallerys = new HashMap<String, GalleryInfo>();
        
        getTags();
        getLocalFavourites();
    }
    
    @Override
    public void finalize() {
        mDBHelper.close();
    }
    
    /****** gallery ******/
    public GalleryInfo getGallery(String gid) {
        GalleryInfo galleryInfo = mGallerys.get(gid);
        if (galleryInfo == null) {
            Cursor cursor = mDatabase.rawQuery("select * from " + TABLE_GALLERY + " where " + COLUMN_GID + "=?",
                    new String[]{gid});
            if (cursor.moveToFirst()) {
                galleryInfo = new GalleryInfo();
                galleryInfo.gid = gid;
                galleryInfo.token = cursor.getString(1);
                galleryInfo.title = cursor.getString(2);
                galleryInfo.posted = cursor.getString(3);
                galleryInfo.category = cursor.getInt(4);
                galleryInfo.thumb = cursor.getString(5);
                galleryInfo.uploader = cursor.getString(6);
                galleryInfo.rating = cursor.getString(7);
                
                // add to map
                mGallerys.put(gid, galleryInfo);
            }
        }
        return galleryInfo;
    }
    
    @SuppressWarnings("unused")
    private synchronized boolean containsGallery(String gid) {
        boolean re = false;
        Cursor cursor = mDatabase.rawQuery("select * from "
                + TABLE_GALLERY + " where " + COLUMN_GID + "=?",
                new String[]{gid});
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
    private synchronized int getGalleryReference(String gid) {
        int reference = -1;
        Cursor cursor = mDatabase.rawQuery("select " + COLUMN_REFERENCE + " from "
                + TABLE_GALLERY + " where " + COLUMN_GID + "=?",
                new String[]{gid});
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
     * @param isUpdate
     */
    private void addGallery(GalleryInfo galleryInfo, boolean isUpdate) {
        boolean isInsert;
        String gid = galleryInfo.gid;
        // add to map
        mGallerys.put(gid, galleryInfo);
        
        // add to sql or update and add reference by 1
        int reference;
        if ((reference = getGalleryReference(gid)) != -1) {
            isInsert = false;
            if (!isUpdate)
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
            mDatabase.update(TABLE_GALLERY, values, COLUMN_GID + "=?", new String[]{gid});
        }
    }
    
    /**
     * decrease reference by 1, only delete when reference is 0
     * @param gid
     */
    private void deleteGallery(String gid) {
        int reference = getGalleryReference(gid);
        if (reference == -1) {
            Log.w(TAG, "Can't get reference when deleteGallery, gid is " + gid);
            return;
        }
        reference--;
        if (reference == 0) { // delete gallery
            mDatabase.delete(TABLE_GALLERY, COLUMN_GID + "=?", new String[]{gid});
        } else { // update reference
            ContentValues values = new ContentValues();
            values.put(COLUMN_REFERENCE, reference);
            mDatabase.update(TABLE_GALLERY, values, COLUMN_GID + "=?", new String[]{gid});
        }
    }
    
    /****** local favourite ******/
    private synchronized void getLocalFavourites() {
        mLocalFavourites = new ArrayList<GalleryInfo>();
        Cursor cursor = mDatabase.rawQuery("select * from "
                + TABLE_LOCAL_FAVOURITE, null);
        
        List<String> invaildGids = new ArrayList<String>();
        
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String gid = cursor.getString(0);
                GalleryInfo galleryInfo = getGallery(gid);
                if (galleryInfo == null) {
                    invaildGids.add(gid);
                } else {
                    mLocalFavourites.add(galleryInfo);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        
        // remove invail gid
        for (String invaildGid : invaildGids) {
            mDatabase.delete(TABLE_LOCAL_FAVOURITE, COLUMN_GID + "=?",
                    new String[]{String.valueOf(invaildGid)});
        }
    }
    
    public synchronized boolean containsLocalFavourite(String gid) {
        boolean re = false;
        Cursor cursor = mDatabase.rawQuery("select * from "
                + TABLE_LOCAL_FAVOURITE + " where " + COLUMN_GID + "=?",
                new String[]{gid});
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
        // add to sql
        ContentValues values = new ContentValues();
        values.put(COLUMN_GID, galleryInfo.gid);
        boolean isUpdate = mDatabase.insert(
                TABLE_LOCAL_FAVOURITE, null, values) == -1;
        addGallery(galleryInfo, isUpdate);
        
        // add to list
        if (!isUpdate)
            mLocalFavourites.add(galleryInfo);
    }
    
    public synchronized void deleteLocalFavourite(String gid) {
        // delete from list
        for (GalleryInfo  galleryInfo : mLocalFavourites) {
            if (galleryInfo.gid.equals(gid)) {
                mLocalFavourites.remove(galleryInfo);
                break;
            }
        }
        
        // delete from sql
        mDatabase.delete(TABLE_LOCAL_FAVOURITE, COLUMN_GID + "=?", new String[]{gid});
        deleteGallery(gid);
    }
    
    public synchronized List<GalleryInfo> getAllLocalFavourites() {
        return mLocalFavourites;
    }
    
    
    /******  tag ******/
    private synchronized void getTags() {
        mTags = new ArrayList<Tag>();
        tagRowNum = 0;
        Cursor cursor = mDatabase.rawQuery("select * from " + TABLE_TAG
                + " order by " + COLUMN_ID + " asc", null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                
                String name = cursor.getString(1);
                int category = cursor.getInt(2);
                String search = cursor.getString(3);
                int advance = cursor.getInt(4);
                int min_rating = cursor.getInt(5);
                
                Tag tag = new Tag(name, category, search);
                tag.setAdvance(advance, min_rating);
                
                mTags.add(tag);
                
                cursor.moveToNext();
                tagRowNum++;
            }
        }
        cursor.close();
    }
    
    public synchronized boolean addTag(Tag tag) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, tagRowNum);
        values.put(COLUMN_NAME, tag.getName());
        values.put(COLUMN_CATEGORY, tag.getType());
        values.put(COLUMN_SEARCH, tag.getSearch());
        values.put(COLUMN_ADVANCE, tag.getAdvanceType());
        values.put(COLUMN_MIN_RATING, tag.getMinRating());
        
        boolean ok = mDatabase.insert(
                TABLE_TAG, null, values) != -1;
        if (ok) {
            tagRowNum++;
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
        for (int i = id + 1; i < tagRowNum; i++) {
            values.put(COLUMN_ID, i - 1);
            mDatabase.update(TABLE_TAG, values, COLUMN_ID + "=?", new String[]{String.valueOf(i)});
        }
        tagRowNum -= deleteNum;
        
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
    
    public synchronized void setTag(int location, Tag tag) {
        if (location == tagRowNum) {
            addTag(tag);
        } else {
            // Update list
            mTags.set(location, tag);
            
            // Update sql
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, tag.getName());
            values.put(COLUMN_CATEGORY, tag.getType());
            values.put(COLUMN_SEARCH, tag.getSearch());
            values.put(COLUMN_ADVANCE, tag.getAdvanceType());
            values.put(COLUMN_MIN_RATING, tag.getMinRating());
            mDatabase.update(TABLE_TAG, values, COLUMN_ID + "=?", new String[]{String.valueOf(location)});
        }
    }
    
    public synchronized boolean swapTag(int indexOne, int indexTwo) {
        if (indexOne < 0 || indexOne > tagRowNum
                || indexTwo < 0 || indexTwo > tagRowNum)
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
            super(context, DB_NAME, null, mVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // gallery
            String CreateGallery = "create table " + TABLE_GALLERY + "("
                    + "gid text primary key,"
                    + "token text,"
                    + "title text,"
                    + "posted text,"
                    + "category integer,"
                    + "thumb text,"
                    + "uploader text,"
                    + "rating text,"
                    + "reference integer);";
            db.execSQL(CreateGallery);
            
            // read
            String CreateRead = "create table "
                    + TABLE_READ + "("
                    + "gid text primary key);";
            db.execSQL(CreateRead);
            
            // local favourite
            String CreateLocalFavourite = "create table "
                    + TABLE_LOCAL_FAVOURITE + "("
                    + "gid text primary key);";
            db.execSQL(CreateLocalFavourite);
            
            // tag
            String CreateTag = "create table "
                    + TABLE_TAG + "("
                    + "id integer primary key,"
                    + "name text,"
                    + "category integer,"
                    + "search text,"
                    + "advance integer,"
                    + "min_rating integer);";
            db.execSQL(CreateTag);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("drop table if exists " + TABLE_GALLERY);
            onCreate(db);
        }
    }
}
