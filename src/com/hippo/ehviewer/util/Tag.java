package com.hippo.ehviewer.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.ArrayList;

import com.hippo.ehviewer.ListMangaDetail;
import com.hippo.ehviewer.ListUrls;

import android.content.Context;
import android.content.SharedPreferences;

public class Tag {
    
    private static final String SHA_PRE_NAME = "tag";
    private static final String LIST_ORDER = "list_order";
    
    private static Context mContext;
    private static SharedPreferences mTagPre;
    private static ArrayList<String> mListOrder = new ArrayList<String>();
    private static ArrayList<ListUrls> mTags = new ArrayList<ListUrls>();
    
    private static boolean mInit = false;
    private static final String NOT_INIT_ERROR_INFO = "Please init Tag first.";
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
        mTagPre = mContext.getSharedPreferences(SHA_PRE_NAME, 0);
        
        getTag();
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }
    
    private static void getTag() {
        if (!mInit)
            throw new IllegalStateException(NOT_INIT_ERROR_INFO);
        
        String[] allOrder = Util.getStrings(mTagPre, LIST_ORDER);
        
        if (allOrder == null)
            return;
        
        boolean errorFlag = false;
        for (String item : allOrder) {
            mListOrder.add(item);
            String strTag = mTagPre.getString(item, null);
            if (strTag == null) {
                mListOrder.remove(strTag);
                errorFlag = true;
                continue;
            }
            mTags.add(decode(strTag));
        }
        if (errorFlag)
            Util.putStrings(mTagPre, LIST_ORDER, mListOrder);
    }
    
    /**
     * Add a new tag item to the end of tag list
     * 
     * @param key
     * @param tag
     * @return
     */
    public static boolean add(String key, ListUrls tag) {
        if (!mInit)
            throw new IllegalStateException(NOT_INIT_ERROR_INFO);
        
        // Check contain this item or not
        if (mListOrder.indexOf(key) == -1) {
            mListOrder.add(key);
            mTags.add(tag);
            Util.putStrings(mTagPre, LIST_ORDER, mListOrder);
            mTagPre.edit().putString(key, encode(tag)).apply();
            return true;
        } else
            return false;
    }
    
    /**
     * Swap tag list order
     * 
     * @param oldIndex
     * @param newIndex
     * @return
     */
    public static boolean swap(int indexOne, int indexTwo) {
        if (indexOne < 0 || indexOne > mListOrder.size()
                || indexTwo < 0 || indexTwo > mListOrder.size())
            return false;
        
        if (indexOne == indexTwo)
            return true;
        
        String temp1 = mListOrder.get(indexOne);
        mListOrder.set(indexOne, mListOrder.get(indexTwo));
        mListOrder.set(indexTwo, temp1);
        Util.putStrings(mTagPre, LIST_ORDER, mListOrder);
        
        ListUrls temp2 = mTags.get(indexOne);
        mTags.set(indexOne, mTags.get(indexTwo));
        mTags.set(indexTwo, temp2);
        return true;
    }
    
    public static boolean contains(String key) {
        return mListOrder.contains(key);
    }
    
    /**
     * Remove Tag item
     * 
     * @param index
     * @return True if remove successfully
     */
    public static boolean remove(String key) {
        if (!mInit)
            throw new IllegalStateException(NOT_INIT_ERROR_INFO);
        
        int index = mListOrder.indexOf(key);
        if (index != -1) {
            mListOrder.remove(index);
            mTags.remove(index);
            mTagPre.edit().remove(key).apply();
            Util.putStrings(mTagPre, LIST_ORDER, mListOrder);
            return true;
        } else
            return false;
    }
    
    /**
     * Remove Tag item
     * 
     * @param index
     * @return True if remove successfully
     */
    public static boolean remove(int index) {
        if (!mInit)
            throw new IllegalStateException(NOT_INIT_ERROR_INFO);
        
        mTags.remove(index);
        String key = mListOrder.remove(index);
        mTagPre.edit().remove(key).apply();
        Util.putStrings(mTagPre, LIST_ORDER, mListOrder);
        return true;
    }
    
    public static ListUrls get(String key) {
        int index = mListOrder.indexOf(key);
        if (index == -1)
            return null;
        return mTags.get(index);
    }
    
    public static ListUrls get(int position) {
        return mTags.get(position);
    }
    
    public static String getKey(int position) {
        return mListOrder.get(position);
    }
    
    public static void set(int position, String newKey, ListUrls listUrls) {
        String oldKey = mListOrder.set(position, newKey);
        mTags.set(position, listUrls);
        
        Util.putStrings(mTagPre, LIST_ORDER, mListOrder);
        mTagPre.edit().remove(oldKey).putString(newKey, encode(listUrls)).apply();
    }
    
    public static void set(int position, ListUrls listUrls) {
        mTags.set(position, listUrls);
        mTagPre.edit().putString(mListOrder.get(position), encode(listUrls)).apply();
    }
    
    /**
     * You must not modify what you get
     * 
     * @return
     */
    public static ArrayList<ListUrls> getTagList() {
        if (!mInit)
            throw new IllegalStateException(NOT_INIT_ERROR_INFO);
        return mTags;
    }
    
    /**
     * You must not modify what you get
     * 
     * @return
     */
    public static ArrayList<String> getKeyList() {
        if (!mInit)
            throw new IllegalStateException(NOT_INIT_ERROR_INFO);
        return mListOrder;
    }
    
    public static void set(String oldKey, String newKey, ListUrls lus) {
        int index = mListOrder.indexOf(oldKey);
        if (index == -1) {
            mListOrder.set(index, newKey);
            mTags.set(index, lus);
        }
    }
    
    protected static String encode(ListUrls tag) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            writeObject(tag, outputStream);
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

    protected static ListUrls decode(String strTag) {
        byte[] bytes = Util.hexStringToByteArray(strTag);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ListUrls tag = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            tag = readObject(ois);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tag;
    }
    
    public static void writeObject(ListUrls tag, ObjectOutputStream out) throws IOException {
        out.writeObject((Integer)tag.getType());
        out.writeObject(tag.getSearch());
        out.writeObject((Boolean)tag.isAdvance());
        out.writeObject((Integer)tag.getAdvanceType());
        out.writeObject((Boolean)tag.isMinRating());
        out.writeObject((Integer)tag.getMinRating());
    }
    
    public static ListUrls readObject(ObjectInputStream in) {
        ListUrls tag = null;
        try {
            int type = (Integer)in.readObject();
            String search = (String)in.readObject();
            tag = new ListUrls(type, search);
            
            boolean isAdvance = (Boolean)in.readObject();
            int advanceType = (Integer)in.readObject();
            boolean isMinRating = (Boolean)in.readObject();
            int minRating = (Integer)in.readObject();
            if (isAdvance) {
                if (isMinRating)
                    tag.setAdvance(advanceType, minRating);
                else
                    tag.setAdvance(advanceType);
            }
        } catch (OptionalDataException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tag;
    }
}
