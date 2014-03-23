package com.hippo.ehviewer.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.ArrayList;

import com.hippo.ehviewer.ListMangaDetail;

import android.content.Context;
import android.content.SharedPreferences;

public class Favourite {
    //private static final String TAG = "Favourite";
    
    private static final String SHA_PRE_NAME = "favourite";
    private static final String LIST_ORDER = "list_order";
    
    private static Context mContext;
    private static SharedPreferences mFavouritePre;
    //private static boolean isLru = false; // TODO float the last read favourite item
    private static ArrayList<String> mListOrder = null;
    private static ArrayList<ListMangaDetail> mFavouriteLmd = null;
    
    private static boolean mInit = false;
    
    /**
     * Init Favourite
     * 
     * @param context Application context
     */
    public static void init(Context context) {
        if (mInit)
            return;
        mInit = true;
        
        mContext = context;
        mFavouritePre = mContext.getSharedPreferences(SHA_PRE_NAME, 0);
        
        getFavourite();
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }
    
    private static void getFavourite() {
        mListOrder = new ArrayList<String>();
        mFavouriteLmd = new ArrayList<ListMangaDetail>();
        String[] allOrder = Util.getStrings(mFavouritePre, LIST_ORDER);
        
        if (allOrder == null)
            return;
        
        boolean errorFlag = false;
        for (String item : allOrder) {
            mListOrder.add(item);
            String strLmd = mFavouritePre.getString(item, null);
            if (strLmd == null) {
                mListOrder.remove(strLmd);
                errorFlag = true;
                continue;
            }
            mFavouriteLmd.add(decodeLmd(strLmd));
        }
        if (errorFlag)
            Util.putStrings(mFavouritePre, LIST_ORDER, mListOrder);
    }
    
    /**
     * Add a new favourite item to the top of favourite list
     * 
     * @param lmd
     * @return True if push successfully
     */
    public static boolean push(ListMangaDetail lmd) {
        if (!mInit) {
            throw new IllegalStateException("Please init Favourite first.");
        }
        // Check contain this item or not
        int oldIndex;
        if ((oldIndex = mListOrder.indexOf(lmd.gid)) != -1) {
            mListOrder.remove(oldIndex);
            mListOrder.add(0, lmd.gid);
            mFavouriteLmd.remove(oldIndex);
            mFavouriteLmd.add(0, lmd);
        } else {
            mListOrder.add(0, lmd.gid);
            mFavouriteLmd.add(0, lmd);
        }
        
        Util.putStrings(mFavouritePre, LIST_ORDER, mListOrder);
        mFavouritePre.edit().putString(lmd.gid, encodeLmd(lmd)).apply();
        
        return true;
    }
    
    /**
     * Remove favourite item
     * 
     * @param index
     * @return True if remove successfully
     */
    public static boolean remove(int index) {
        mFavouriteLmd.remove(index);
        String key = mListOrder.remove(index);
        Util.putStrings(mFavouritePre, LIST_ORDER, mListOrder);
        mFavouritePre.edit().remove(key).apply();
        return true;
    }
    
    /**
     * Please do not change it
     * 
     * @return
     */
    public static ArrayList<ListMangaDetail> getFavouriteList() {
        return mFavouriteLmd;
    }
    
    /**
     * Top a favourite item
     * 
     * @param index
     */
    public static void top(int index) {
        // TODO
    }
    
    /**
     * Update favourite item data
     */
    public static void updateData() {
        // TODO
    }
    
    protected static String encodeLmd(ListMangaDetail lmd) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            writeObject(lmd, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Util.byteArrayToHexString(os.toByteArray());
    }

    protected static ListMangaDetail decodeLmd(String lmdStr) {
        byte[] bytes = Util.hexStringToByteArray(lmdStr);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ListMangaDetail lmd = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            lmd = readObject(ois);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return lmd;
    }
    
    public static void writeObject(ListMangaDetail lmd, ObjectOutputStream out) throws IOException {
        out.writeObject(lmd.gid);
        out.writeObject(lmd.token);
        out.writeObject(lmd.archiver_key);
        out.writeObject(lmd.title);
        out.writeObject(lmd.title_jpn);
        out.writeObject((Integer)lmd.category);
        out.writeObject(lmd.thumb);
        out.writeObject(lmd.uploader);
        out.writeObject(lmd.posted);
        out.writeObject(lmd.filecount);
        out.writeObject((Long)lmd.filesize);
        out.writeObject((Boolean)lmd.expunged);
        out.writeObject(lmd.rating);
        out.writeObject(lmd.torrentcount);
    }
    
    public static ListMangaDetail readObject(ObjectInputStream in) {
        ListMangaDetail lmd = new ListMangaDetail();
        try {
            lmd.gid = (String)in.readObject();
            lmd.token = (String)in.readObject();
            lmd.archiver_key = (String)in.readObject();
            lmd.title = (String)in.readObject();
            lmd.title_jpn = (String)in.readObject();
            lmd.category = (Integer)in.readObject();
            lmd.thumb = (String)in.readObject();
            lmd.uploader = (String)in.readObject();
            lmd.posted = (String)in.readObject();
            lmd.filecount = (String)in.readObject();
            lmd.filesize = (Long)in.readObject();
            lmd.expunged = (Boolean)in.readObject();
            lmd.rating = (String)in.readObject();
            lmd.torrentcount = (String)in.readObject();
        } catch (OptionalDataException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lmd;
    }
}
