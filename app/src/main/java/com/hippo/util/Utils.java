/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Vibrator;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

public final class Utils {
    @SuppressWarnings("unused")
    private static String TAG = Utils.class.getSimpleName();

    public static final DateFormat DATE_FORMATE_FOR_GALLERY = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    public static final DateFormat DATE_FORMATE_FOR_UPDATE = new SimpleDateFormat("yyyyMMdd", Locale.US);

    public static final String EMPTY_STRING = "";

    /**
     * Colse is. Don't worry about anything.
     *
     * @param is
     */
    public static void closeQuietly(Closeable is) {
        try {
            if (is != null)
                is.close();
        } catch (IOException e) {
        }
    }

    /**
     * Put InputStream to File, default bufferSize is 512 * 1024
     *
     * @param is
     * @param file
     * @throws java.io.IOException
     */
    public static void inputStream2File(InputStream is, File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        copy(is, os);
        os.close();
    }

    /**
     * Copy from is to os
     *
     * @param is
     * @param os
     * @throws java.io.IOException
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        copy(is, os, 512 * 1024);
    }

    /**
     * Copy from is to os
     *
     * @param is
     * @param os
     * @param size Buffer size
     * @throws java.io.IOException
     */
    public static void copy(InputStream is, OutputStream os, int size) throws IOException {
        byte[] buffer = new byte[size];
        int bytesRead;
        while((bytesRead = is.read(buffer)) !=-1)
            os.write(buffer, 0, bytesRead);
        os.flush();
    }

    /**
     * Get String from InputStream
     *
     * @param is
     * @param charset
     * @param size The InputStream size you estimate
     * @return
     */
    public static String inputStream2String(InputStream is, String charset, int size) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        String str = null;
        try {
            copy(is, baos, 1024);
            str = baos.toString(charset);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(baos);
        }
        return str;
    }

    /**
     * Not work for dir, Copy from scr to dst, no matter dst exist or not.
     *
     * @param src
     * @param dst
     * @return True for success, false for fail
     */
    public static boolean copyFile(File src, File dst) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst);
            copy(fis, fos);
        } catch (Throwable e) {
            return false;
        } finally {
            closeQuietly(fis);
            closeQuietly(fos);
        }
        return true;
    }

    /**
     * If you want to copy on new thread, use it to know
     * success or not.
     */
    public static interface OnCopyFileOverListener {
        public void onCopyFileOver(boolean success, File src, File dst);
    }

    /**
     * Copy file in new thread
     *
     * @param src
     * @param dst
     * @param l
     */
    public static void copyFileInNewThread(final File src, final File dst, final OnCopyFileOverListener l) {
        execute(false, new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return copyFile(src, dst);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (l != null)
                    l.onCopyFileOver(result, src, dst);
            }
        }, (Void[]) null);
    }

    /**
     * Try to delete file or dir and it's children.
     *
     * @param file The file to deleted.
     * @return True if file is delete, children included.
     */
    public static boolean deleteFile(File file) {
        if (file == null) {
            return false;
        }

        boolean result = true;

        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files)
                deleteFile(f);
        }
        if (file.exists()) {
            // Avoid EBUSY (Device or resource busy) error
            final File to = new File(file.getAbsolutePath()
                    + System.currentTimeMillis());
            if (file.renameTo(to))
                result &= to.delete();
            else
                result &= file.delete();
        }

        return result;
    }

    /**
     * If you want to copy on new thread, use it to know
     * success or not.
     */
    public static interface OnDeleteFileOverListener {
        public void onDeleteFileOver(boolean success, File file);
    }

    /**
     * Start a new thread to delete dir
     */
    public static void deleteFileInNewThread(final File file, final OnDeleteFileOverListener l) {
        execute(false, new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return deleteFile(file);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (l != null)
                    l.onDeleteFileOver(result, file);
            }
        }, (Void[]) null);
    }

    public static int getDateForUpdate() {
        return Utils.parseIntSafely(DATE_FORMATE_FOR_UPDATE.format(new Date()), 0);
    }


    private static String[] SIZE_UNIT = {"%.2f B", "%.2f KB", "%.2f MB", "%.2f GB"};

    public static String sizeToString(long size) {
        int length = SIZE_UNIT.length;

        float sizeFloat = size;
        for (int i = 0; i < length - 1; i++) {
            if (sizeFloat < 1024)
                return String.format(SIZE_UNIT[i], sizeFloat);
            sizeFloat /= 1024;
        }
        return String.format(SIZE_UNIT[length - 1], sizeFloat);
    }

    /**
     * Execute an {@link android.os.AsyncTask} on a thread pool
     *
     * @param forceSerial True to force the task to run in serial order
     * @param task Task to execute
     * @param args Optional arguments to pass to
     *            {@link android.os.AsyncTask#execute(Object[])}
     * @param <T> Task argument type
     */
    @SafeVarargs
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
        String result = name;
        // Remove invaild char
        result = result.replaceAll("[\\\\/:*?\"<>\\|]", "");
        // Make sure size
        if (result.length() > 255)
            result = result.substring(0,  255);
        // Avoid start with whitespace or end with whitespace
        result = result.replaceAll("^[\\s]+", "").replace("[\\s]+$", "");
        return result;
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

        if (text == null || TextUtils.isEmpty(text) || searchList == null ||
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
                    TextUtils.isEmpty(searchList[i]) || replacementList[i] == null) {
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
                        TextUtils.isEmpty(searchList[i]) || replacementList[i] == null) {
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
        return integer != 0;
    }

    /**
     * false for 0, true for 1
     *
     * @param bool
     * @return
     */
    public static int boolean2int(boolean bool) {
        return bool ? 1 : 0;
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
     * Return true if point is in the rect
     *
     * @param area
     * @param x
     * @param y
     * @return
     */
    public static boolean isInArea(int[] area, int x, int y) {
        if (area == null)
            throw new NullPointerException("area == null");
        if (area.length != 4)
            throw new IllegalArgumentException(
                    "area's length should be 4, but it's length is " + area.length);
        if (x >= area[0] && x < area[2] && y >= area[1] && y < area[3])
            return true;
        else
            return false;
    }

    /**
     * Is color is opaque
     *
     * @param color
     * @return
     */
    // TODO put it to UiUtils.class
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
     *
     * @param context
     * @param milliseconds Duration
     */
    public static void vibrator(Context context, long milliseconds) {
        // If user force disable vibrator permission, may throw error
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(milliseconds);
        } catch (Throwable e) {
        }
    }

    // Removes duplicate adjacent slashes and any trailing slash.
    private static String fixSlashes(String origPath) {
        // Remove duplicate adjacent slashes.
        boolean lastWasSlash = false;
        char[] newPath = origPath.toCharArray();
        int length = newPath.length;
        int newLength = 0;
        for (int i = 0; i < length; ++i) {
            char ch = newPath[i];
            if (ch == '/') {
                if (!lastWasSlash) {
                    newPath[newLength++] = File.separatorChar;
                    lastWasSlash = true;
                }
            } else {
                newPath[newLength++] = ch;
                lastWasSlash = false;
            }
        }
        // Remove any trailing slash (unless this is the root of the file system).
        if (lastWasSlash && newLength > 1) {
            newLength--;
        }
        // Reuse the original string if possible.
        return (newLength != length) ? new String(newPath, 0, newLength) : origPath;
    }

    // Joins two path components, adding a separator only if necessary.
    public static String joinPath(String dirPath, String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (dirPath == null || TextUtils.isEmpty(dirPath)) {
            return fixSlashes(name);
        } else if (TextUtils.isEmpty(name)) {
            return fixSlashes(dirPath);
        } else {
            int prefixLength = dirPath.length();
            boolean haveSlash = (prefixLength > 0 && dirPath.charAt(prefixLength - 1) == File.separatorChar);
            if (!haveSlash) {
                haveSlash = (name.length() > 0 && name.charAt(0) == File.separatorChar);
            }
            return fixSlashes(haveSlash ? (dirPath + name) : (dirPath + File.separatorChar + name));
        }
    }

    public static int parseIntSafely(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public static float parseFloatSafely(String str, float defaultValue) {
        try {
            return Float.parseFloat(str);
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

    /**
     * Get extension from target string, int lower case
     *
     * @param name
     * @param defautl
     * @return
     */
    public static String getExtension(String name, String defautl) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(name);
        if (TextUtils.isEmpty(ext))
            ext = defautl;
        return ext;
    }

    /**
     * It will return null if can't find mime
     *
     * @param url
     * @return
     */
    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    /**
     * If get error return "";
     *
     * @param key
     * @return
     */
    public static String getSystemProperties(String key) {
        try {
            @SuppressWarnings("rawtypes")
            Class c = Class.forName("android.os.SystemProperties");
            @SuppressWarnings("unchecked")
            Method m = c.getDeclaredMethod("get", String.class);
            m.setAccessible(true);
            String result = (String) m.invoke(null, key);
            return result == null ? EMPTY_STRING : result;
        } catch (Throwable e) {
            return EMPTY_STRING;
        }
    }
}
