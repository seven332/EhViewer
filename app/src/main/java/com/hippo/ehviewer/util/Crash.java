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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

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

    public static final String getThrowableInfo(Throwable t) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        t.printStackTrace(printWriter);
        Throwable cause = t.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        return writer.toString();
    }

    /**
     * Save throwable infomation to file
     *
     * @param ex The throwable to store
     */
    @SuppressLint("CommitPrefEdits")
    public static void saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        collectDeviceInfo(sb);

        String result = getThrowableInfo(ex);
        Log.e(TAG, result);
        sb.append("======== CrashInfo ========\n").append(result).append("\n");
        try {
            long timestamp = System.currentTimeMillis();
            String time = Utils.sDate.format(System.currentTimeMillis());
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
            // Save it immediately
            editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "An error occured while writing crash file...", e);
        }
    }

    private static void collectDeviceInfo(StringBuffer sb) {
        boolean getPackageInfo = false;
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = String.valueOf(pi.versionCode);
                sb.append("======== PackageInfo ========\n");
                sb.append("PackageName=").append(pi.packageName).append("\n");
                sb.append("VersionName=").append(versionName).append("\n");
                sb.append("VersionCode=").append(versionCode).append("\n");
                sb.append("\n");

                getPackageInfo = true;
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        if (!getPackageInfo) {
            sb.append("======== PackageInfo ========\n");
            sb.append("Can't get package information\n");
            sb.append("\n");
        }

        sb.append("======== DeviceInfo ========\n");
        sb.append("BOARD=").append(Build.BOARD).append("\n");
        sb.append("BOOTLOADER=").append(Build.BOOTLOADER).append("\n");
        sb.append("CPU_ABI=").append(Build.CPU_ABI).append("\n");
        sb.append("CPU_ABI2=").append(Build.CPU_ABI2).append("\n");
        sb.append("DEVICE=").append(Build.DEVICE).append("\n");
        sb.append("DISPLAY=").append(Build.DISPLAY).append("\n");
        sb.append("FINGERPRINT=").append(Build.FINGERPRINT).append("\n");
        sb.append("HARDWARE=").append(Build.HARDWARE).append("\n");
        sb.append("HOST=").append(Build.HOST).append("\n");
        sb.append("ID=").append(Build.ID).append("\n");
        sb.append("MANUFACTURER=").append(Build.MANUFACTURER).append("\n");
        sb.append("MODEL=").append(Build.MODEL).append("\n");
        sb.append("PRODUCT=").append(Build.PRODUCT).append("\n");
        sb.append("RADIO=").append(Build.getRadioVersion()).append("\n");
        sb.append("SERIAL=").append(Build.SERIAL).append("\n");
        sb.append("TAGS=").append(Build.TAGS).append("\n");
        sb.append("TYPE=").append(Build.TYPE).append("\n");
        sb.append("USER=").append(Build.USER).append("\n");
        sb.append("CODENAME=").append(Build.VERSION.CODENAME).append("\n");
        sb.append("INCREMENTAL=").append(Build.VERSION.INCREMENTAL).append("\n");
        sb.append("RELEASE=").append(Build.VERSION.RELEASE).append("\n");
        sb.append("SDK=").append(Build.VERSION.SDK_INT).append("\n");
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
            configPre.edit().putBoolean(NEW_CRAHS, false).apply();
            return new String(buffer);
        } catch (Exception e) {
            Log.e(TAG, "An error occured while reading crash file...", e);
        }
        return null;
    }
}
