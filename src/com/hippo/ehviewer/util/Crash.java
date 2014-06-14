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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.util.Log;

public class Crash {
    private static final String TAG = "Crash";
    
    private static Context mContext;
    
    private static String externalCrashFilePath = "/EhViewer/crash";
    private static String internalCrashFilePath = "/crash";
    
    private static final String NEW_CRAHS = "new_crash";
    private static final String LAST_CRASH_POSITION = "last_crash_position";
    private static final String LAST_CRASH_NAME = "last_crash_name";
    
    private static boolean mInit = false;
    
    /**
     * Init Crash
     * 
     * @param context Application context
     */
    public static void init(Context context) {
        if (mInit)
            return;
        mInit = true;
        
        mContext = context;
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }
    
    /**
     * Save throwable infomation to file
     * 
     * @param ex The throwable to store
     */
    public static void saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        collectDeviceInfo(sb);
        
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        Log.e(TAG, result);
        sb.append("======== CrashInfo ========\n");
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = ((AppContext)mContext).getDateFormat().format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            String path = null;
            boolean position = false;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                path = Environment.getExternalStorageDirectory() + externalCrashFilePath;
                position = true;
            }
            else {
                path = mContext.getFilesDir() + internalCrashFilePath;
                position = false;
            }
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(new File(path, fileName));
            fos.write(sb.toString().getBytes());
            fos.close();
            // Record info for next time upload
            SharedPreferences configPre = mContext.getSharedPreferences("config", 0);
            SharedPreferences.Editor editor = configPre.edit();
            editor.putBoolean(NEW_CRAHS, true);
            editor.putBoolean(LAST_CRASH_POSITION, position);
            editor.putString(LAST_CRASH_NAME, fileName);
            editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "An error occured while writing crash file...", e);
        }
    }
    
    private static void collectDeviceInfo(StringBuffer sb) {
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = String.valueOf(pi.versionCode);
                sb.append("======== PackageInfo ========\n");
                sb.append("PackageName=" + pi.packageName + "\n");
                sb.append("VersionName=" + versionName + "\n");
                sb.append("VersionCode=" + versionCode + "\n");
                sb.append("\n");
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        sb.append("======== DeviceInfo ========\n");
        sb.append("BOARD=" + Build.BOARD + "\n");
        sb.append("BOOTLOADER=" + Build.BOOTLOADER + "\n");
        sb.append("CPU_ABI=" + Build.CPU_ABI + "\n");
        sb.append("CPU_ABI2=" + Build.CPU_ABI2 + "\n");
        sb.append("DEVICE=" + Build.DEVICE + "\n");
        sb.append("DISPLAY=" + Build.DISPLAY + "\n");
        sb.append("FINGERPRINT=" + Build.FINGERPRINT + "\n");
        sb.append("HARDWARE=" + Build.HARDWARE + "\n");
        sb.append("HOST=" + Build.HOST + "\n");
        sb.append("ID=" + Build.ID + "\n");
        sb.append("MANUFACTURER=" + Build.MANUFACTURER + "\n");
        sb.append("MODEL=" + Build.MODEL + "\n");
        sb.append("PRODUCT=" + Build.PRODUCT + "\n");
        sb.append("RADIO=" + Build.getRadioVersion() + "\n");
        sb.append("SERIAL=" + Build.SERIAL + "\n");
        sb.append("TAGS=" + Build.TAGS + "\n");
        sb.append("TYPE=" + Build.TYPE + "\n");
        sb.append("USER=" + Build.USER + "\n");
        sb.append("CODENAME=" + Build.VERSION.CODENAME + "\n");
        sb.append("INCREMENTAL=" + Build.VERSION.INCREMENTAL + "\n");
        sb.append("RELEASE=" + Build.VERSION.RELEASE + "\n");
        sb.append("SDK=" + Build.VERSION.SDK_INT + "\n");
        sb.append("\n");
    }
    
    /**
     * Check is there last crash
     * 
     * @return Return the crash String or null if there is no
     */
    public static String getLastCrash() {
        SharedPreferences configPre = mContext.getSharedPreferences("config", 0);
        if (!configPre.getBoolean(NEW_CRAHS, false))
            return null;
        
        boolean position = configPre.getBoolean(LAST_CRASH_POSITION, false);
        String lastCrashName;
        if ((lastCrashName = configPre.getString(LAST_CRASH_NAME, null)) == null)
            return null;
        
        File lastCrashFile;
        if (position) {
            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                return null;
            lastCrashFile = new File(Environment.getExternalStorageDirectory() + externalCrashFilePath, lastCrashName);
        }
        else {
            lastCrashFile = new File(mContext.getFilesDir() + internalCrashFilePath, lastCrashName);
        }
        if (!lastCrashFile.isFile())
            return null;
        
        try {
            byte[] buffer = new byte[(int) lastCrashFile.length()];
            DataInputStream din = new DataInputStream(new FileInputStream(lastCrashFile));
            din.readFully(buffer);
            din.close();
            configPre.edit().putBoolean(NEW_CRAHS, false).commit();
            return new String(buffer);
        } catch (Exception e) {
            Log.e(TAG, "An error occured while reading crash file...", e);
        }
        return null;
    }
}
