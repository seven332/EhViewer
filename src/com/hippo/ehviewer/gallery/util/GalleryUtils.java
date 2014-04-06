/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.hippo.ehviewer.gallery.util;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import com.hippo.ehviewer.util.Log;
import android.view.WindowManager;

import com.hippo.ehviewer.util.ThreadPool.CancelListener;
import com.hippo.ehviewer.util.ThreadPool.JobContext;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

public class GalleryUtils {
    private static final String TAG = "GalleryUtils";
    private static final String MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    private static final String MAPS_CLASS_NAME = "com.google.android.maps.MapsActivity";

    public static final String MIME_TYPE_IMAGE = "image/*";
    public static final String MIME_TYPE_VIDEO = "video/*";
    public static final String MIME_TYPE_PANORAMA360 = "application/vnd.google.panorama360+jpg";
    public static final String MIME_TYPE_ALL = "*/*";

    private static float sPixelDensity = -1f;

    public static void initialize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        sPixelDensity = metrics.density;
    }

    public static float[] intColorToFloatARGBArray(int from) {
        return new float[] {
            Color.alpha(from) / 255f,
            Color.red(from) / 255f,
            Color.green(from) / 255f,
            Color.blue(from) / 255f
        };
    }

    public static float dpToPixel(float dp) {
        return sPixelDensity * dp;
    }

    public static int dpToPixel(int dp) {
        return Math.round(dpToPixel((float) dp));
    }

    public static int meterToPixel(float meter) {
        // 1 meter = 39.37 inches, 1 inch = 160 dp.
        return Math.round(dpToPixel(meter * 39.37f * 160));
    }

    public static byte[] getBytes(String in) {
        byte[] result = new byte[in.length() * 2];
        int output = 0;
        for (char ch : in.toCharArray()) {
            result[output++] = (byte) (ch & 0xFF);
            result[output++] = (byte) (ch >> 8);
        }
        return result;
    }

    // Below are used the detect using database in the render thread. It only
    // works most of the time, but that's ok because it's for debugging only.

    private static volatile Thread sCurrentThread;
    private static volatile boolean sWarned;

    public static void setRenderThread() {
        sCurrentThread = Thread.currentThread();
    }

    public static void assertNotInRenderThread() {
        if (!sWarned) {
            if (Thread.currentThread() == sCurrentThread) {
                sWarned = true;
                Log.w(TAG, new Throwable("Should not do this in render thread"));
            }
        }
    }

    private static final double RAD_PER_DEG = Math.PI / 180.0;
    private static final double EARTH_RADIUS_METERS = 6367000.0;

    public static double fastDistanceMeters(double latRad1, double lngRad1,
            double latRad2, double lngRad2) {
       if ((Math.abs(latRad1 - latRad2) > RAD_PER_DEG)
             || (Math.abs(lngRad1 - lngRad2) > RAD_PER_DEG)) {
           return accurateDistanceMeters(latRad1, lngRad1, latRad2, lngRad2);
       }
       // Approximate sin(x) = x.
       double sineLat = (latRad1 - latRad2);

       // Approximate sin(x) = x.
       double sineLng = (lngRad1 - lngRad2);

       // Approximate cos(lat1) * cos(lat2) using
       // cos((lat1 + lat2)/2) ^ 2
       double cosTerms = Math.cos((latRad1 + latRad2) / 2.0);
       cosTerms = cosTerms * cosTerms;
       double trigTerm = sineLat * sineLat + cosTerms * sineLng * sineLng;
       trigTerm = Math.sqrt(trigTerm);

       // Approximate arcsin(x) = x
       return EARTH_RADIUS_METERS * trigTerm;
    }

    public static double accurateDistanceMeters(double lat1, double lng1,
            double lat2, double lng2) {
        double dlat = Math.sin(0.5 * (lat2 - lat1));
        double dlng = Math.sin(0.5 * (lng2 - lng1));
        double x = dlat * dlat + dlng * dlng * Math.cos(lat1) * Math.cos(lat2);
        return (2 * Math.atan2(Math.sqrt(x), Math.sqrt(Math.max(0.0,
                1.0 - x)))) * EARTH_RADIUS_METERS;
    }


    public static final double toMile(double meter) {
        return meter / 1609;
    }

    // For debugging, it will block the caller for timeout millis.
    public static void fakeBusy(JobContext jc, int timeout) {
        final ConditionVariable cv = new ConditionVariable();
        jc.setCancelListener(new CancelListener() {
            @Override
            public void onCancel() {
                cv.open();
            }
        });
        cv.block(timeout);
        jc.setCancelListener(null);
    }

    public static void startCameraActivity(Context context) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static String formatLatitudeLongitude(String format, double latitude,
            double longitude) {
        // We need to specify the locale otherwise it may go wrong in some language
        // (e.g. Locale.FRENCH)
        return String.format(Locale.ENGLISH, format, latitude, longitude);
    }

    public static void showOnMap(Context context, double latitude, double longitude) {
        try {
            // We don't use "geo:latitude,longitude" because it only centers
            // the MapView to the specified location, but we need a marker
            // for further operations (routing to/from).
            // The q=(lat, lng) syntax is suggested by geo-team.
            String uri = formatLatitudeLongitude("http://maps.google.com/maps?f=q&q=(%f,%f)",
                    latitude, longitude);
            ComponentName compName = new ComponentName(MAPS_PACKAGE_NAME,
                    MAPS_CLASS_NAME);
            Intent mapsIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(uri)).setComponent(compName);
            context.startActivity(mapsIntent);
        } catch (ActivityNotFoundException e) {
            // Use the "geo intent" if no GMM is installed
            Log.e(TAG, "GMM activity not found!", e);
            String url = formatLatitudeLongitude("geo:%f,%f", latitude, longitude);
            Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(mapsIntent);
        }
    }

    public static void setViewPointMatrix(
            float matrix[], float x, float y, float z) {
        // The matrix is
        // -z,  0,  x,  0
        //  0, -z,  y,  0
        //  0,  0,  1,  0
        //  0,  0,  1, -z
        Arrays.fill(matrix, 0, 16, 0);
        matrix[0] = matrix[5] = matrix[15] = -z;
        matrix[8] = x;
        matrix[9] = y;
        matrix[10] = matrix[11] = 1;
    }

    public static int getBucketId(String path) {
        return path.toLowerCase().hashCode();
    }

    // Return the local path that matches the given bucketId. If no match is
    // found, return null
    public static String searchDirForPath(File dir, int bucketId) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String path = file.getAbsolutePath();
                    if (GalleryUtils.getBucketId(path) == bucketId) {
                        return path;
                    } else {
                        path = searchDirForPath(file, bucketId);
                        if (path != null) return path;
                    }
                }
            }
        }
        return null;
    }

    public static boolean hasSpaceForSize(long size) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return false;
        }

        String path = Environment.getExternalStorageDirectory().getPath();
        try {
            StatFs stat = new StatFs(path);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize() > size;
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return false;
    }
}
