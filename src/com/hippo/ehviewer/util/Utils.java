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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;

public class Utils {
    @SuppressWarnings("unused")
    private static String TAG = "Util";

    /**
     * Put InputStream to File, default bufferSize is 512 * 1024
     *
     * @param is
     * @param file
     * @throws IOException
     */
    public static void inputStream2File(InputStream is, File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        copy(is, os);
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        copy(is, os, 512 * 1024);
    }

    public static void copy(InputStream is, OutputStream os, int size) throws IOException {
        byte[] buffer = new byte[size];
        int bytesRead;
        while((bytesRead = is.read(buffer)) !=-1)
            os.write(buffer, 0, bytesRead);
        is.close();
        os.flush();
        os.close();
        buffer = null;
    }

    public static final int BITMAP = 0x0;
    public static final int MOVIE = 0x1;

    public static int getResourcesType(String url) {
        int type = BITMAP;
        int index = url.lastIndexOf('.');
        if (index != -1 && getExtension(url).equals("gif"))
            type = MOVIE;
        return type;
    }

    public static String getExtension(String url) {
        int index = url.lastIndexOf('.');
        if (index != -1)
            return url.substring(index + 1).toLowerCase();
        else
            return "png";
    }

    public static String getName(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index != -1)
            return fileName.substring(0, index).toLowerCase();
        else
            return "png";
    }

    public static boolean isNumber(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch < '0' || ch > '9')
                return false;
        }
        return true;
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte element : b) {
            int v = element & 0xFF;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase(Locale.getDefault());
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length()/2*2;
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[(i / 2)] = ((byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16)));
        }
        return data;
    }

    /**
     * Convernt context in stream to string
     *
     * @param is
     * @param charset
     * @return
     */
    public static String InputStream2String(InputStream is, String charset) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String str = null;
        try {
            Utils.copy(is, baos, 1024);
            str = baos.toString(charset);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.closeStreamQuietly(baos);
        }
        return str;
    }

    public static String getFileForUrl(String url) {
        String file = null;
        int index = url.lastIndexOf("/");
        if (index == -1)
            return url;
        else
            return url.substring(index + 1);
    }

    public static String[] getStrings(SharedPreferences shaper, String key) {
        String str = shaper.getString(key, null);
        if (str == null || str.length() == 0)
            return null;
        return new String(Utils.hexStringToByteArray(str)).split("\n");
    }

    public static void putStrings(SharedPreferences shaper, String key, List<String> strs) {
        StringBuffer sb = new StringBuffer();
        for (String item : strs) {
            sb.append(item);
            sb.append('\n');
        }
        int length = sb.length();
        if (length != 0)
            sb.delete(length - 1, length);
        shaper.edit().putString(key, byteArrayToHexString(sb.toString().getBytes())).apply();
    }

    public static void closeStreamQuietly (Closeable is) {
        try {
            if (is != null)
                is.close();
        } catch (IOException e) {
        }
    }

    /**
     * Delete dir and it child file and dir
     *
     * @param dir
     * The dir to deleted
     * @throws IOException
     */
    public static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
          throw new IOException("not a readable directory: " + dir);
        }
        for (File file : files) {
          if (file.isDirectory()) {
            deleteContents(file);
          }
          if (!file.delete()) {
            throw new IOException("failed to delete file: " + file);
          }
        }
      }

    @SuppressLint("SimpleDateFormat")
    public static int getDate() {
        int time = 0;
        try {
            time = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(new Date()));
        } catch (NumberFormatException e) {}
        return time;
    }


    private static String[] SIZE_UNIT = {"%.2f B", "%.2f KB", "%.2f MB", "%.2f GB"};

    public static String sizeToString(long size) {
        int length = SIZE_UNIT.length;

        float sizeFloat = size;
        for (int i = 0; i < length; i++) {
            if (sizeFloat < 1024 || i == length - 1) {
                return String.format(SIZE_UNIT[i], sizeFloat);
            }
            sizeFloat /= 1024;
        }
        return null;
    }

    /**
     * Execute an {@link AsyncTask} on a thread pool
     *
     * @param forceSerial True to force the task to run in serial order
     * @param task Task to execute
     * @param args Optional arguments to pass to
     *            {@link AsyncTask#execute(Object[])}
     * @param <T> Task argument type
     */
    @SuppressLint("NewApi")
    public static <T> void execute(final boolean forceSerial, final AsyncTask<T, ?, ?> task,
            final T... args) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.DONUT) {
            throw new UnsupportedOperationException(
                    "This class can only be used on API 4 and newer.");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || forceSerial) {
            task.execute(args);
        } else {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
        }
    }

    /**
     * Method that removes the support for HardwareAcceleration from a {@link View}.<br/>
     * <br/>
     * Check AOSP notice:<br/>
     * <pre>
     * 'ComposeShader can only contain shaders of different types (a BitmapShader and a
     * LinearGradient for instance, but not two instances of BitmapShader)'. But, 'If your
     * application is affected by any of these missing features or limitations, you can turn
     * off hardware acceleration for just the affected portion of your application by calling
     * setLayerType(View.LAYER_TYPE_SOFTWARE, null).'</pre>
     *
     * @param v The view
     */
    public static void removeHardwareAccelerationSupport(View v) {
        if (v.getLayerType() != View.LAYER_TYPE_SOFTWARE) {
            v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /**
     * Mostly title and url need it
     * @param str
     * @return
     */
    public static final String htmlUnsescape(String str) {
        return str.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#039;", "'");
    }

    /**
     * Make file name valid by removing invalid character and set max length
     * @param name
     * @return
     */
    public static final String rightFileName(String name) {
        name = name.replaceAll("[\\\\|/|:|*|?|\"|<|>|\\|]", "");
        return name.length() > 255 ? name.substring(0,  255) : name;
    }

    /**
     * Joins the elements of the provided array into a single String
     * containing the provided list of elements.
     *
     * @param array
     * @param separator
     * @return
     */
    public static final String join(final Object[] array,
            final Object separator) {
        final StringBuilder sb = new StringBuilder(array.length * 16);
        for (int i = 0; i < array.length; i++) {
            if (i != 0)
                sb.append(separator);
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static final boolean int2boolean(int integer) {
        return integer == 0 ? false : true;
    }

    public static void getCenterInWindows(View view, int[] location) {
        getLocationInWindow(view, location);
        location[0] += view.getWidth() / 2;
        location[1] += view.getHeight() / 2;
    }

    public static void getLocationInWindow(View view, int[] location) {
        if (location == null || location.length < 2) {
            throw new IllegalArgumentException("location must be an array of two integers");
        }

        float[] position = new float[2];

        position[0] = view.getLeft();
        position[1] = view.getTop();

        ViewParent viewParent = view.getParent();
        while (viewParent instanceof View) {
            view = (View)viewParent;
            if (view.getId() == android.R.id.content) {
                break;
            }

            position[0] -= view.getScrollX();
            position[1] -= view.getScrollY();

            position[0] += view.getLeft();
            position[1] += view.getTop();

            viewParent = view.getParent();
         }

        location[0] = (int) (position[0] + 0.5f);
        location[1] = (int) (position[1] + 0.5f);
    }

    /**
     * Returns a bitmap showing a screenshot of the view passed in.
     * @param v
     * @return
     */
    public static Bitmap getBitmapFromView(View v) {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        // TODO I need to know why I need it, when ScrollView
        canvas.translate(-v.getScrollX(), -v.getScrollY());
        v.draw(canvas);
        return bitmap;
    }
}
