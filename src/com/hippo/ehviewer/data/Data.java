package com.hippo.ehviewer.data;

import java.util.ArrayList;
import java.util.List;

import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.Tag;
import com.hippo.ehviewer.util.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author Hippo
 * 
 */

public class Data {
    
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
    private static final String COLUMN_UPLODER = "uploder";
    private static final String COLUMN_RATING = "rating";
    private static final String COLUMN_REFERENCE = "reference";
    
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_SEARCH = "search";
    private static final String COLUMN_ADVANCE = "advance";
    private static final String COLUMN_MIN_RATING = "min_rating";
    
    private long tagRowNum;
    private List<Tag> mTags = new ArrayList<Tag>();
    
    private Context mContext;
    
    private DBHelper mDBHelper;
    private SQLiteDatabase mDatabase;
    
    public Data(Context context) {
        mContext = context;
        mDBHelper = new DBHelper(mContext);
        mDatabase = mDBHelper.getWritableDatabase();
        
        getTags();
    }
    
    @Override
    public void finalize() {
        mDBHelper.close();
    }
    
    /******  TAG ******/
    private synchronized List<Tag> getTags() {
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
        return mTags;
    }
    
    public synchronized boolean addTag(String name, Tag tag) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, tagRowNum);
        values.put(COLUMN_NAME, name);
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
    
    public synchronized void deleteTag(int id) {
        int deleteNum = mDatabase.delete(TABLE_TAG, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        tagRowNum -= deleteNum;
    }
    
    public List<Tag> getAllTags() {
        return mTags;
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
        mDatabase.update(TABLE_TAG, values, COLUMN_ID + "=?", new String[]{String.valueOf(indexOne)});
        return true;
    }
    
    
    
    public class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DB_NAME, null, mVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String CreateGallery = "create table " + TABLE_GALLERY + "("
                    + "gid text primary key,"
                    + "token text,"
                    + "title text,"
                    + "posted text,"
                    + "category integer,"
                    + "thumb text,"
                    + "uploder text,"
                    + "rating text,"
                    + "reference integer);";
            db.execSQL(CreateGallery);
            
            String CreateRead = "create table "
                    + TABLE_READ + "("
                    + "gid text primary key);";
            db.execSQL(CreateRead);
            
            String CreateLocalFavourite = "create table "
                    + TABLE_LOCAL_FAVOURITE + "("
                    + "gid text primary key);";
            db.execSQL(CreateLocalFavourite);
            
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
