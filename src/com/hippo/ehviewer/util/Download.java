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

package com.hippo.ehviewer.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;

import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.activity.DownloadInfo;

public class Download {
    private static final String SHA_PRE_NAME = "download_info";
    private static final String LIST_ORDER = "list_order";
    
    private static Context mContext;
    private static SharedPreferences mDownloadInfoPre;
    private static ArrayList<String> mListOrder = new ArrayList<String>();
    private static ArrayList<DownloadInfo> mDownloadInfos = new ArrayList<DownloadInfo>();
    
    private static boolean mInit = false;
    
    /**
     * Init Tag
     * 
     * @param context Application context
     */
    public static void init(Context context) {
        if (mInit)
            return;
        mInit = true;
        
        mContext = context;
        mDownloadInfoPre = mContext.getSharedPreferences(SHA_PRE_NAME, 0);
        
        getDownloadInfo();
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }
    
    private static void getDownloadInfo() {
        String[] allOrder = Util.getStrings(mDownloadInfoPre, LIST_ORDER);
        
        if (allOrder == null)
            return;
        
        boolean errorFlag = false;
        for (String item : allOrder) {
            mListOrder.add(item);
            String strDownloadInfo = mDownloadInfoPre.getString(item, null);
            if (strDownloadInfo == null) {
                mListOrder.remove(strDownloadInfo);
                errorFlag = true;
                continue;
            }
            DownloadInfo di = decode(strDownloadInfo);
            if (di.status == DownloadInfo.DOWNLOADING
                    || di.status == DownloadInfo.WAITING)
                di.status = DownloadInfo.STOP;
            
            mDownloadInfos.add(di);
        }
        if (errorFlag)
            Util.putStrings(mDownloadInfoPre, LIST_ORDER, mListOrder);
    }
    
    /**
     * Add a new tag item to the end of tag list
     * 
     * @param key
     * @param tag
     * @return
     */
    public static boolean add(String key, DownloadInfo di) {
        // Check contain this item or not
        if (mListOrder.indexOf(key) == -1) {
            mListOrder.add(key);
            mDownloadInfos.add(di);
            Util.putStrings(mDownloadInfoPre, LIST_ORDER, mListOrder);
            mDownloadInfoPre.edit().putString(key, encode(di)).apply();
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
        Util.putStrings(mDownloadInfoPre, LIST_ORDER, mListOrder);
        
        DownloadInfo temp2 = mDownloadInfos.get(indexOne);
        mDownloadInfos.set(indexOne, mDownloadInfos.get(indexTwo));
        mDownloadInfos.set(indexTwo, temp2);
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
        int index = mListOrder.indexOf(key);
        if (index != -1) {
            mListOrder.remove(index);
            mDownloadInfos.remove(index);
            mDownloadInfoPre.edit().remove(key).apply();
            Util.putStrings(mDownloadInfoPre, LIST_ORDER, mListOrder);
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
        mDownloadInfos.remove(index);
        String key = mListOrder.remove(index);
        mDownloadInfoPre.edit().remove(key).apply();
        Util.putStrings(mDownloadInfoPre, LIST_ORDER, mListOrder);
        return true;
    }
    
    public static DownloadInfo get(String key) {
        int index = mListOrder.indexOf(key);
        if (index == -1)
            return null;
        return mDownloadInfos.get(index);
    }
    
    /**
     * You must not modify what you get
     * 
     * @return
     */
    public static ArrayList<DownloadInfo> getDownloadInfoList() {
        return mDownloadInfos;
    }
    
    public static DownloadInfo get(int position) {
        return mDownloadInfos.get(position);
    }
    
    public static String getKey(int position) {
        return mListOrder.get(position);
    }
    
    public static ArrayList<String> getKeys() {
        return mListOrder;
    }
    
    public static void notify(String key) {
        int index = mListOrder.indexOf(key);
        if (index != -1) {
            mDownloadInfoPre.edit().putString(key, encode(mDownloadInfos.get(index))).apply();
        }
    }
    
    protected static String encode(DownloadInfo di) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            writeObject(di, outputStream);
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

    protected static DownloadInfo decode(String strDi) {
        byte[] bytes = Util.hexStringToByteArray(strDi);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        DownloadInfo di = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            di = readObject(ois);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return di;
    }
    
    public static void writeObject(DownloadInfo di, ObjectOutputStream out) throws IOException {
        out.writeObject(di.gid);
        out.writeObject(di.thumb);
        out.writeObject(di.title);
        out.writeObject((Integer)di.status);
        out.writeObject((Boolean)di.type);
        out.writeObject(di.detailUrlStr);
        out.writeObject((Integer)di.pageSum);
        out.writeObject((Integer)di.lastStartIndex);
        out.writeObject(di.pageUrlStr);
    }
    
    public static DownloadInfo readObject(ObjectInputStream in) {
        DownloadInfo di = new DownloadInfo();
        try {
            di.gid = (String)in.readObject();
            di.thumb = (String)in.readObject();
            di.title = (String)in.readObject();
            di.status = (Integer)in.readObject();
            di.type = (Boolean)in.readObject();
            di.detailUrlStr = (String)in.readObject();
            di.pageSum = (Integer)in.readObject();
            di.lastStartIndex = (Integer)in.readObject();
            di.pageUrlStr = (String)in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return di;
    }
}
