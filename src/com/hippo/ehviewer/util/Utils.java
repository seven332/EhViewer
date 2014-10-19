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
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Vibrator;

public final class Utils {
    @SuppressWarnings("unused")
    private static String TAG = "Util";

    public static final boolean SUPPORT_IMAGE;
    @SuppressLint("SimpleDateFormat")
    public static final DateFormat sDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    @SuppressLint("SimpleDateFormat")
    public static final DateFormat sDate2 = new SimpleDateFormat("yyyyMMdd");


    static {
        String cpu = Build.CPU_ABI;
        SUPPORT_IMAGE = cpu.equals("armeabi") || cpu.equals("armeabi-v7a");
    }

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
            Utils.closeQuietly(baos);
        }
        return str;
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

    public static void closeQuietly (Closeable is) {
        try {
            if (is != null)
                is.close();
        } catch (IOException e) {
        }
    }

    /**
     * Try to delete file, dir and it's children
     *
     * @param dir
     * The dir to deleted
     */
    public static void deleteFile(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files)
                deleteFile(f);
        }
        file.delete();
    }

    /**
     * Start a new thread to delete dir
     */
    public static void deleteDirInThread(final File file) {
        new BgThread() {
            @Override
            public void run() {
                deleteFile(file);
            }
        }.start();
    }

    static {

    }

    public static int getDate() {
        int time = 0;
        try {
            time = Integer.parseInt(sDate2.format(new Date()));
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

    private static final String[] ESCAPE_CHARATER_LIST = {
        "&amp;",
        "&lt;",
        "&gt;",
        "&quot;",
        "&#039;",
        "&times;"
    };

    private static final String[] UNESCAPE_CHARATER_LIST = {
        "&",
        "<",
        ">",
        "\"",
        "'",
        "×"
    };

    /**
     * Mostly title and url need it
     * @param str
     * @return
     */
    public static String unescapeXml(String str) {
        return replaceEach(str, ESCAPE_CHARATER_LIST, UNESCAPE_CHARATER_LIST);
    }

    /**
     * Make file name valid by removing invalid character and set max length
     * @param name
     * @return
     */
    public static String standardizeFilename(String name) {
        name = name.replaceAll("[\\\\/:*?\"<>\\|]", "");
        name = name.length() > 255 ? name.substring(0,  255) : name;
        return name.replaceAll("^[\\s]+", "").replace("[\\s]+$", "");
    }

    /**
     * Joins the elements of the provided array into a single String
     * containing the provided list of elements.
     *
     * @param array
     * @param separator
     * @return
     */
    public static String join(final Object[] array,
            final Object separator) {
        final StringBuilder sb = new StringBuilder(array.length * 16);
        for (int i = 0; i < array.length; i++) {
            if (i != 0)
                sb.append(separator);
            sb.append(array[i]);
        }
        return sb.toString();
    }

    /**
     * <p>
     * Replaces all occurrences of Strings within another String.
     * </p>
     *
     * <p>
     * A {@code null} reference passed to this method is a no-op, or if
     * any "search string" or "string to replace" is null, that replace will be
     * ignored. This will not repeat. For repeating replaces, call the
     * overloaded method.
     * </p>
     *
     * <pre>
     *  StringUtils.replaceEach(null, *, *)        = null
     *  StringUtils.replaceEach("", *, *)          = ""
     *  StringUtils.replaceEach("aba", null, null) = "aba"
     *  StringUtils.replaceEach("aba", new String[0], null) = "aba"
     *  StringUtils.replaceEach("aba", null, new String[0]) = "aba"
     *  StringUtils.replaceEach("aba", new String[]{"a"}, null)  = "aba"
     *  StringUtils.replaceEach("aba", new String[]{"a"}, new String[]{""})  = "b"
     *  StringUtils.replaceEach("aba", new String[]{null}, new String[]{"a"})  = "aba"
     *  StringUtils.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"w", "t"})  = "wcte"
     *  (example of how it does not repeat)
     *  StringUtils.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "t"})  = "dcte"
     * </pre>
     *
     * @param text
     *            text to search and replace in, no-op if null
     * @param searchList
     *            the Strings to search for, no-op if null
     * @param replacementList
     *            the Strings to replace them with, no-op if null
     * @return the text with any replacements processed, {@code null} if
     *         null String input
     * @throws IllegalArgumentException
     *             if the lengths of the arrays are not the same (null is ok,
     *             and/or size 0)
     */
    // Get from org.apache.commons.lang3.StringUtils
    public static String replaceEach(final String text, final String[] searchList, final String[] replacementList) {
        return replaceEach(text, searchList, replacementList, false, 0);
    }

    /**
     * <p>
     * Replaces all occurrences of Strings within another String.
     * </p>
     *
     * <p>
     * A {@code null} reference passed to this method is a no-op, or if
     * any "search string" or "string to replace" is null, that replace will be
     * ignored.
     * </p>
     *
     * <pre>
     *  StringUtils.replaceEach(null, *, *, *) = null
     *  StringUtils.replaceEach("", *, *, *) = ""
     *  StringUtils.replaceEach("aba", null, null, *) = "aba"
     *  StringUtils.replaceEach("aba", new String[0], null, *) = "aba"
     *  StringUtils.replaceEach("aba", null, new String[0], *) = "aba"
     *  StringUtils.replaceEach("aba", new String[]{"a"}, null, *) = "aba"
     *  StringUtils.replaceEach("aba", new String[]{"a"}, new String[]{""}, *) = "b"
     *  StringUtils.replaceEach("aba", new String[]{null}, new String[]{"a"}, *) = "aba"
     *  StringUtils.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"w", "t"}, *) = "wcte"
     *  (example of how it repeats)
     *  StringUtils.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "t"}, false) = "dcte"
     *  StringUtils.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "t"}, true) = "tcte"
     *  StringUtils.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "ab"}, *) = IllegalStateException
     * </pre>
     *
     * @param text
     *            text to search and replace in, no-op if null
     * @param searchList
     *            the Strings to search for, no-op if null
     * @param replacementList
     *            the Strings to replace them with, no-op if null
     * @param repeat if true, then replace repeatedly
     *       until there are no more possible replacements or timeToLive < 0
     * @param timeToLive
     *            if less than 0 then there is a circular reference and endless
     *            loop
     * @return the text with any replacements processed, {@code null} if
     *         null String input
     * @throws IllegalStateException
     *             if the search is repeating and there is an endless loop due
     *             to outputs of one being inputs to another
     * @throws IllegalArgumentException
     *             if the lengths of the arrays are not the same (null is ok,
     *             and/or size 0)
     */
    // Get from org.apache.commons.lang3.StringUtils
    private static String replaceEach(
            final String text, final String[] searchList, final String[] replacementList, final boolean repeat, final int timeToLive) {

        // mchyzer Performance note: This creates very few new objects (one major goal)
        // let me know if there are performance requests, we can create a harness to measure

        if (text == null || text.isEmpty() || searchList == null ||
                searchList.length == 0 || replacementList == null || replacementList.length == 0) {
            return text;
        }

        // if recursing, this shouldn't be less than 0
        if (timeToLive < 0) {
            throw new IllegalStateException("Aborting to protect against StackOverflowError - " +
                                            "output of one loop is the input of another");
        }

        final int searchLength = searchList.length;
        final int replacementLength = replacementList.length;

        // make sure lengths are ok, these need to be equal
        if (searchLength != replacementLength) {
            throw new IllegalArgumentException("Search and Replace array lengths don't match: "
                + searchLength
                + " vs "
                + replacementLength);
        }

        // keep track of which still have matches
        final boolean[] noMoreMatchesForReplIndex = new boolean[searchLength];

        // index on index that the match was found
        int textIndex = -1;
        int replaceIndex = -1;
        int tempIndex = -1;

        // index of replace array that will replace the search string found
        // NOTE: logic duplicated below START
        for (int i = 0; i < searchLength; i++) {
            if (noMoreMatchesForReplIndex[i] || searchList[i] == null ||
                    searchList[i].isEmpty() || replacementList[i] == null) {
                continue;
            }
            tempIndex = text.indexOf(searchList[i]);

            // see if we need to keep searching for this
            if (tempIndex == -1) {
                noMoreMatchesForReplIndex[i] = true;
            } else {
                if (textIndex == -1 || tempIndex < textIndex) {
                    textIndex = tempIndex;
                    replaceIndex = i;
                }
            }
        }
        // NOTE: logic mostly below END

        // no search strings found, we are done
        if (textIndex == -1) {
            return text;
        }

        int start = 0;

        // get a good guess on the size of the result buffer so it doesn't have to double if it goes over a bit
        int increase = 0;

        // count the replacement text elements that are larger than their corresponding text being replaced
        for (int i = 0; i < searchList.length; i++) {
            if (searchList[i] == null || replacementList[i] == null) {
                continue;
            }
            final int greater = replacementList[i].length() - searchList[i].length();
            if (greater > 0) {
                increase += 3 * greater; // assume 3 matches
            }
        }
        // have upper-bound at 20% increase, then let Java take over
        increase = Math.min(increase, text.length() / 5);

        final StringBuilder buf = new StringBuilder(text.length() + increase);

        while (textIndex != -1) {

            for (int i = start; i < textIndex; i++) {
                buf.append(text.charAt(i));
            }
            buf.append(replacementList[replaceIndex]);

            start = textIndex + searchList[replaceIndex].length();

            textIndex = -1;
            replaceIndex = -1;
            tempIndex = -1;
            // find the next earliest match
            // NOTE: logic mostly duplicated above START
            for (int i = 0; i < searchLength; i++) {
                if (noMoreMatchesForReplIndex[i] || searchList[i] == null ||
                        searchList[i].isEmpty() || replacementList[i] == null) {
                    continue;
                }
                tempIndex = text.indexOf(searchList[i], start);

                // see if we need to keep searching for this
                if (tempIndex == -1) {
                    noMoreMatchesForReplIndex[i] = true;
                } else {
                    if (textIndex == -1 || tempIndex < textIndex) {
                        textIndex = tempIndex;
                        replaceIndex = i;
                    }
                }
            }
            // NOTE: logic duplicated above END

        }
        final int textLength = text.length();
        for (int i = start; i < textLength; i++) {
            buf.append(text.charAt(i));
        }
        final String result = buf.toString();
        if (!repeat) {
            return result;
        }

        return replaceEach(result, searchList, replacementList, repeat, timeToLive - 1);
    }

    /**
     * 0 for false, Non 0 for true
     *
     * @param integer
     * @return
     */
    public static boolean int2boolean(int integer) {
        return integer == 0 ? false : true;
    }

    /**
     * Returns the ASCII characters up to but not including the next "\r\n", or
     * "\n".
     *
     * @throws java.io.EOFException if the stream is exhausted before the next
     *             newline character.
     */
    public static String readAsciiLine(final InputStream in) throws IOException {
        final StringBuilder result = new StringBuilder(80);
        while (true) {
            final int c = in.read();
            if (c == -1) {
                throw new EOFException();
            } else if (c == '\n') {
                break;
            }

            result.append((char)c);
        }
        final int length = result.length();
        if (length > 0 && result.charAt(length - 1) == '\r') {
            result.setLength(length - 1);
        }
        return result.toString();
    }

    /**
     * Try to make dir exists
     *
     * @param dir
     * @param force
     */
    public static void ensureDir(File dir, boolean force) {
        if (!dir.exists()) {
            dir.mkdirs();
        } else if (force && dir.isFile()) {
            dir.delete();
            dir.mkdirs();
        }
    }

    /**
     * Get extension from target string, int lower case
     *
     * @param name
     * @param defautl
     * @return
     */
    public static String getExtension(String name, String defautl) {
        int index = name.lastIndexOf('.');
        if (index == -1 || index == name.length() - 1)
            return defautl;
        else
            return name.substring(index + 1).toLowerCase();
    }

    /**
     * Return true if point is in the rect
     *
     * @param area
     * @param x
     * @param y
     * @return
     */
    public static boolean isInArea(int[] area, int x, int y) {
        if (area.length != 4)
            throw new IllegalArgumentException(
                    "area's length should be 4, but it's length is " + area.length);
        if (x >= area[0] && x < area[2] && y >= area[1] && y < area[3])
            return true;
        else
            return false;
    }

    public static boolean isOpaque(int color) {
        return color >>> 24 == 0xFF;
    }

    /**
     * Throws AssertionError if the input is false.
     * @param cond
     */
    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    /**
     * vibrator
     */
    public static void vibrator(Context context, long milliseconds) {
        Vibrator v = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(milliseconds);
    }

    public static String getPathName(String parent, String name) {
        StringBuilder sb = new StringBuilder(parent);
        if (parent.charAt(parent.length() - 1) != File.separatorChar)
            sb.append(File.separatorChar);
        sb.append(name);
        return sb.toString();
    }

    public static int parseIntSafely(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public static int[] toIntArray(Collection<? extends Number> collection) {
        Object[] boxedArray = collection.toArray();
        int len = boxedArray.length;
        int[] array = new int[len];
        for (int i = 0; i < len; i++) {
            // checkNotNull for GWT (do not optimize)
            array[i] = ((Number) (boxedArray[i])).intValue();
        }
        return array;
    }

    // Throws NullPointerException if the input is null.
    public static <T> T checkNotNull(T object) {
        if (object == null) throw new NullPointerException();
        return object;
    }
}
