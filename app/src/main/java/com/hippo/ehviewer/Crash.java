/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.hippo.util.PackageUtils;
import com.hippo.util.ReadableTime;
import com.hippo.yorozuya.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class Crash {

    private static void collectInfo(Context context, FileWriter fw) throws IOException {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = String.valueOf(pi.versionCode);
                fw.write("======== PackageInfo ========\r\n");
                fw.write("PackageName=");fw.write(pi.packageName);fw.write("\r\n");
                fw.write("VersionName=");fw.write(versionName);fw.write("\r\n");
                fw.write("VersionCode=");fw.write(versionCode);fw.write("\r\n");
                String signature = PackageUtils.getSignature(context, pi.packageName);
                fw.write("Signature=");fw.write(null != signature ? signature : "null");fw.write("\r\n");
                fw.write("\r\n");
            }
        } catch (PackageManager.NameNotFoundException e) {
            fw.write("======== PackageInfo ========\r\n");
            fw.write("Can't get package information\r\n");
            fw.write("\r\n");
        }

        // Device info
        fw.write("======== DeviceInfo ========\r\n");
        fw.write("BOARD=");fw.write(Build.BOARD);fw.write("\r\n");
        fw.write("BOOTLOADER=");fw.write(Build.BOOTLOADER);fw.write("\r\n");
        fw.write("CPU_ABI=");fw.write(Build.CPU_ABI);fw.write("\r\n");
        fw.write("CPU_ABI2=");fw.write(Build.CPU_ABI2);fw.write("\r\n");
        fw.write("DEVICE=");fw.write(Build.DEVICE);fw.write("\r\n");
        fw.write("DISPLAY=");fw.write(Build.DISPLAY);fw.write("\r\n");
        fw.write("FINGERPRINT=");fw.write(Build.FINGERPRINT);fw.write("\r\n");
        fw.write("HARDWARE=");fw.write(Build.HARDWARE);fw.write("\r\n");
        fw.write("HOST=");fw.write(Build.HOST);fw.write("\r\n");
        fw.write("ID=");fw.write(Build.ID);fw.write("\r\n");
        fw.write("MANUFACTURER=");fw.write(Build.MANUFACTURER);fw.write("\r\n");
        fw.write("MODEL=");fw.write(Build.MODEL);fw.write("\r\n");
        fw.write("PRODUCT=");fw.write(Build.PRODUCT);fw.write("\r\n");
        fw.write("RADIO=");fw.write(Build.getRadioVersion());fw.write("\r\n");
        fw.write("SERIAL=");fw.write(Build.SERIAL);fw.write("\r\n");
        fw.write("TAGS=");fw.write(Build.TAGS);fw.write("\r\n");
        fw.write("TYPE=");fw.write(Build.TYPE);fw.write("\r\n");
        fw.write("USER=");fw.write(Build.USER);fw.write("\r\n");
        fw.write("CODENAME=");fw.write(Build.VERSION.CODENAME);fw.write("\r\n");
        fw.write("INCREMENTAL=");fw.write(Build.VERSION.INCREMENTAL);fw.write("\r\n");
        fw.write("RELEASE=");fw.write(Build.VERSION.RELEASE);fw.write("\r\n");
        fw.write("SDK=");fw.write(Integer.toString(Build.VERSION.SDK_INT));fw.write("\r\n");
        fw.write("\r\n");
    }

    public static void getThrowableInfo(Throwable t, FileWriter fw) {
        PrintWriter printWriter = new PrintWriter(fw);
        t.printStackTrace(printWriter);
        Throwable cause = t.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
    }

    public static void saveCrashInfo2File(Context context, Throwable ex) {
        File dir = AppConfig.getExternalCrashDir();
        if (dir == null) {
            return;
        }

        String nowString = ReadableTime.getFilenamableTime(System.currentTimeMillis());
        String fileName = "crash-" + nowString + ".log";
        File file = new File(dir, fileName);

        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write("TIME=");fw.write(nowString);fw.write("\r\n");
            fw.write("\r\n");
            collectInfo(context, fw);
            fw.write("======== CrashInfo ========\r\n");
            getThrowableInfo(ex, fw);
            fw.write("\r\n");

            fw.flush();

            Settings.putCrashFilename(fileName);
        } catch (Exception e) {
            file.delete();
        } finally {
            IOUtils.closeQuietly(fw);
        }
    }

    public static boolean hasCrashFile() {
        String filename = Settings.getCrashFilename();
        if (filename == null) {
            return false;
        }

        File dir = AppConfig.getExternalCrashDir();
        if (dir == null) {
            return false;
        }

        return new File(dir, filename).isFile();
    }

    public static String getCrashContent() {
        String filename = Settings.getCrashFilename();
        if (filename == null) {
            return null;
        }

        File dir = AppConfig.getExternalCrashDir();
        if (dir == null) {
            return null;
        }

        File file = new File(dir, filename);

        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return IOUtils.readString(is, "UTF-8");
        } catch (IOException e) {
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static void resetCrashFile() {
        Settings.putCrashFilename(null);
    }
}
