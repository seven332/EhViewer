/*
 * Copyright (C) 2014-2015 Hippo Seven
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

package com.hippo.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;

import com.hippo.util.FastByteArrayOutputStream;
import com.hippo.util.Log;
import com.hippo.util.Utils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * It is not thread safe
 */
public class HttpHelper {
    private static final String TAG = HttpHelper.class.getSimpleName();

    private static final int MAX_RETRY = 3;
    private static final int MAX_REDIRECTS = 3;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/39.0.2171.95 Safari/537.36";
    public static final String USER_AGENT =
            System.getProperty("http.agent", DEFAULT_USER_AGENT);

    private static final int HTTP_TEMP_REDIRECT = 307;

    private static final String DEFAULT_CHARSET = "utf-8";
    private static final String CHARSET_KEY = "charset=";

    private static final CookieManager sCookieManager = CookieManager.getInstance();

    private int mResponseCode = -1;

    static {
        System.setProperty("file.encoding", DEFAULT_CHARSET);
    }

    public void reset() {
        mResponseCode = -1;
    }

    public int getResponseCode() {
        return mResponseCode;
    }

    /**
     * Get cookie for the url
     * @param url the URL
     * @return the cookie for the URL
     */
    protected String getCookie(URL url) {
        return sCookieManager.getCookie(url.toString());
    }

    /**
     * Store cookie for the url
     * @param url the URL
     * @param value the cookie for the URL
     */
    protected void storeCookie(URL url, String value) {
        sCookieManager.setCookie(url.toString(), value);
    }

    /**
     * Prepare before connecting
     * @param conn the connection
     */
    protected void onBeforeConnect(HttpURLConnection conn) {
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
    }

    /**
     * Is URL.toString is same
     */
    public static boolean isURLEquals(URL url1, URL url2) {
        if (url1 != null && url2 != null) {
            String urlStr1 = url1.toString();
            String urlStr2 = url2.toString();
            if (urlStr1 != null && urlStr2 != null) {
                return urlStr1.equals(urlStr2);
            }
        }
        return false;
    }

    /**
     * parse string like <code>haha=hehe; fere=bfdgds</code>
     *
     * @param raw the raw string
     * @return the key is toLowerCase
     */
    public static @NonNull Map<String, String> parseMap(@Nullable String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw != null) {
            String[] pieces = raw.split(";");
            for (String p : pieces) {
                int index = p.indexOf('=');
                if (index != -1) {
                    String key = p.substring(0, index).trim();
                    String value = p.substring(index + 1).trim();

                    // value might be "blabla", remove "
                    int valueLength = value.length();
                    if (value.length() > 1 && value.charAt(0) == '"' &&
                            value.charAt(valueLength - 1) == '"') {
                        value = value.substring(1, valueLength - 1);
                    }
                    map.put(key.toLowerCase(), value);
                }
            }
        }

        return map;
    }

    private Object doRequst(RequestHelper rh) throws Exception {
        URL url;
        HttpURLConnection conn = null;
        int redirectionCount = 0;
        try {
            url = rh.getUrl();
            while (redirectionCount++ < MAX_REDIRECTS) {
                Log.d(TAG, "Request: " + url.toString());
                conn = (HttpURLConnection) url.openConnection();
                // Prepare before connecting
                onBeforeConnect(conn);
                // Set cookie
                String cookie = getCookie(url);
                if (cookie != null) {
                    conn.setRequestProperty("Cookie", cookie);
                }
                // Do custom staff
                rh.onBeforeConnect(conn);

                conn.connect();
                // Store cookie
                List<String> cookieList = conn.getHeaderFields().get("Set-Cookie");
                if (cookieList != null) {
                    for (String cookieTemp : cookieList) {
                        if (cookieTemp != null) {
                            storeCookie(url, cookieTemp);
                        }
                    }
                }
                final int responseCode = conn.getResponseCode();
                mResponseCode = responseCode;
                Log.d(TAG, "Response code: " + responseCode);
                switch (responseCode) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    // Should not come here,
                    // because conn.setInstanceFollowRedirects(true)
                    final String location = conn.getHeaderField("Location");
                    Log.d(TAG, "New location: " + location);
                    conn.disconnect();
                    url = new URL(url, location);
                    break;
                default:
                    // Check redirect
                    URL finalURL = conn.getURL();
                    if (!isURLEquals(url, finalURL)) {
                        rh.onRedirect(finalURL);
                    }
                    return rh.onAfterConnect(conn);
                }
            }
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        throw new RedirectionException();
    }

    protected Object requst(@NonNull RequestHelper rh) throws Exception {
        Exception exception = null;
        for (
                int times = 0;
                times < MAX_RETRY && (times == 0 || rh.onRetry(exception));
                times++) {
            try {
                return doRequst(rh);
            } catch (Exception e) {
                exception = e;
            }
        }

        rh.onRequestFailed(exception);

        throw exception;
    }

    public interface RequestHelper {

        /**
         * Get the URL to connect
         * @return the URL to connect
         */
        URL getUrl() throws MalformedURLException;

        /**
         * Add header or do something else for HttpURLConnection before connect
         *
         * @param conn the connection
         * @throws Exception
         */
        void onBeforeConnect(HttpURLConnection conn) throws Exception;

        /**
         * If get redirect
         *
         * @param newURL the new URL
         */
        void onRedirect(URL newURL);

        /**
         * Get what do you need from HttpURLConnection after connect
         * Return null means get error
         *
         * @param conn the connection
         * @return what you want to return
         * @throws Exception
         */
        Object onAfterConnect(HttpURLConnection conn) throws Exception;

        /**
         * Retry http connecting, or stop
         *
         * @param previousException previous thrown
         * @return true for retry, false for stop
         */
        boolean onRetry(Exception previousException);

        /**
         * Called when request failed by exception
         *
         * @param exception the final exception
         */
        void onRequestFailed(Exception exception);
    }

    public static abstract class GetStringHelper implements RequestHelper {
        private final String mUrl;

        public GetStringHelper(String url) {
            mUrl = url;
        }

        @Override
        public URL getUrl() throws MalformedURLException {
            return new URL(mUrl);
        }

        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            conn.addRequestProperty("Accept-Encoding", "gzip");
        }

        private String getCharset(HttpURLConnection conn) {
            String charset = parseMap(conn.getContentType()).get("charset");
            if (charset != null) {
                return charset;
            } else {
                return DEFAULT_CHARSET;
            }
        }

        private String getBody(HttpURLConnection conn)
                throws Exception {
            String body = null;
            InputStream is = null;
            ByteArrayOutputStream baos = null;
            try {
                try {
                    // First try to get input stream
                    is = conn.getInputStream();
                } catch (Exception t){
                    // If we get error, get error stream
                    is = conn.getErrorStream();
                }
                String encoding = conn.getContentEncoding();
                if (encoding != null && encoding.equalsIgnoreCase("gzip"))
                    is = new GZIPInputStream(is);

                int length = conn.getContentLength();
                if (length >= 0) {
                    baos = new ByteArrayOutputStream(length);
                } else {
                    baos = new ByteArrayOutputStream();
                }

                Utils.copy(is, baos);

                body = baos.toString(getCharset(conn));

            } finally {
                Utils.closeQuietly(is);
                Utils.closeQuietly(baos);
            }

            return body;
        }

        @Override
        public void onRedirect(@NonNull URL newURL) {
            // Empty
        }

        @Override
        public Object onAfterConnect(@NonNull HttpURLConnection conn)
                throws Exception {
            return getBody(conn);
        }

        @Override
        public boolean onRetry(@NonNull Exception previousException) {
            // Do not care about exception, just retry
            return true;
        }

        @Override
        public void onRequestFailed(@NonNull Exception exception) {
            // Empty
        }
    }

    /**
     * RequstHelper for GET method
     */
    public static class GetHelper extends GetStringHelper {

        public GetHelper(String url) {
            super(url);
        }

        @Override
        public void onBeforeConnect(@NonNull HttpURLConnection conn)
                throws Exception {
            super.onBeforeConnect(conn);
            conn.setRequestMethod("GET");
        }
    }

    /**
     * RequstHelper for post form data, use POST method
     */
    public static class PostFormHelper extends GetStringHelper {
        private final String[][] mArgs;

        public PostFormHelper(String url, String[][] args) {
            super(url);
            mArgs = args;
        }

        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            super.onBeforeConnect(conn);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (String[] arg : mArgs) {
                if (i != 0)
                    sb.append("&");
                sb.append(URLEncoder.encode(arg[0], "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(arg[1], "UTF-8"));
                i++;
            }
            out.writeBytes(sb.toString());
            out.flush();
            out.close();
        }
    }

    /**
     * RequstHelper for post json, use POST method
     */
    public static class PostJsonHelper extends GetStringHelper {
        private final JSONObject mJo;

        public PostJsonHelper(String url, JSONObject jo) {
            super(url);
            mJo = jo;
        }

        @Override
        public void onBeforeConnect(HttpURLConnection conn) throws Exception {
            super.onBeforeConnect(conn);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            String body = mJo.toString();
            Log.d(TAG, "Post json: " + body);
            out.write(body.getBytes("utf-8"));
            out.flush();
            out.close();
        }
    }

    public abstract static class FormData {
        private final Map<String, String> mProperties;

        public FormData() {
            mProperties = new LinkedHashMap<>();
        }

        public void setProperty(String key, String value) {
            mProperties.put(key, value);
        }

        public void clearProperty(String key) {
            mProperties.remove(key);
        }

        public void clearAllProperties() {
            mProperties.clear();
        }

        /**
         * Put information to target OutputStream.
         *
         * @param os target outputStream
         * @throws java.io.IOException
         */
        public abstract void output(OutputStream os) throws IOException;

        public void doOutPut(OutputStream os) throws IOException {
            StringBuilder sb = new StringBuilder();
            for (String key : mProperties.keySet())
                sb.append(key).append(": ").append(mProperties.get(key)).append("\r\n");
            sb.append("\r\n");
            os.write(sb.toString().getBytes());
            output(os);
        }
    }

    public static class StringData extends FormData {
        private final String mStr;

        public StringData(String str) {
            mStr = str;
        }

        @Override
        public void output(OutputStream os) throws IOException {
            os.write(mStr.getBytes());
            os.write("\r\n".getBytes());
        }
    }

    public static class BitmapData extends FormData {
        private final Bitmap mBitmap;

        public BitmapData(Bitmap bmp) {
            mBitmap = bmp;
            setProperty("Content-Type", "image/jpeg");
        }

        @Override
        public void output(OutputStream os) throws IOException {
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 98, os);
            os.write("\r\n".getBytes());
        }
    }

    public static class FileData extends FormData {
        private final File mFile;

        public FileData(File file) {
            mFile = file;
            setProperty("Content-Type",
                    URLConnection.guessContentTypeFromName(file.getName()));
        }

        @Override
        public void output(OutputStream os) throws IOException {
            InputStream is = new FileInputStream(mFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while((bytesRead = is.read(buffer)) !=-1)
                os.write(buffer, 0, bytesRead);
            is.close();

            os.write("\r\n".getBytes());
        }
    }

    public static class PostFormDataHelper extends GetStringHelper {
        private static final String BOUNDARY = "----WebKitFormBoundary7eDB0hDQ91s22Tkf";

        private final List<FormData> mDataList;


        public PostFormDataHelper(String url, List<FormData> dataList) {
            super(url);
            mDataList = dataList;
        }

        @Override
        public void onBeforeConnect(HttpURLConnection conn) throws Exception {
            super.onBeforeConnect(conn);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + BOUNDARY);

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());

            for (FormData data : mDataList) {
                out.write("--".getBytes());
                out.write(BOUNDARY.getBytes());
                out.write("\r\n".getBytes());
                data.doOutPut(out);
            }
            out.write("--".getBytes());
            out.write(BOUNDARY.getBytes());
            out.write("--".getBytes());

            out.flush();
            out.close();
        }
    }

    public static class GetImageHelper implements RequestHelper {
        private final String mUrl;

        public GetImageHelper(String url) {
            mUrl = url;
        }

        @Override
        public URL getUrl() throws MalformedURLException {
            return new URL(mUrl);
        }

        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            conn.setRequestMethod("GET");
        }

        @Override
        public void onRedirect(@NonNull URL newURL) {
            // Empty
        }

        @Override
        public Object onAfterConnect(HttpURLConnection conn)
                throws Exception {
            // If just
            // Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream(), null, Ui.getBitmapOpt());
            // bitmap might be incomplete.
            int size = conn.getContentLength();
            FastByteArrayOutputStream fbaos = new FastByteArrayOutputStream(size == -1 ? 24 * 1024 : (size + 100));
            Utils.copy(conn.getInputStream(), fbaos);
            Bitmap bmp = BitmapFactory.decodeByteArray(fbaos.getBuffer(), 0, fbaos.size(), null);

            if (bmp == null) {
                throw new IllegalStateException("Can not decode to bitmap.");
            }
            return bmp;
        }

        @Override
        public boolean onRetry(Exception previousException) {
            // Do not care about exception, just retry
            return true;
        }

        @Override
        public void onRequestFailed(Exception exception) {
            // Empty
        }
    }

    public interface OnDownloadListener {

        /**
         * Called before connecting
         */
        void onStartConnecting();

        /**
         * Called after connecting and before downloading
         *
         * @param totalSize content length, -1 for unknown
         */
        void onStartDownloading(int totalSize);

        /**
         * File name is fixed
         *
         * @param newName new file name
         */
        void onNameFix(String newName);

        /**
         * Called repeatedly during downloading
         *
         * @param downloadSize downloaded size
         * @param totalSize content length, -1 for unknown
         */
        void onDownload(int downloadSize, int totalSize);

        /**
         * Called when download ok
         */
        void onSuccess();

        /**
         * Called when download stop
         */
        void onStop();

        /**
         * Called when download failed
         *
         * @param e the exception why fail
         */
        void onFailure(Exception e);
    }

    public static class DownloadControlor {
        private boolean mStop = false;

        public void stop() {
            mStop = true;
        }

        public void reset() {
            mStop = false;
        }

        public boolean isStop() {
            return mStop;
        }
    }

    public static class DownloadHelper implements RequestHelper {

        private static final String DOWNLOAD_EXTENSION = ".download";

        private final String mUrl;
        private final File mDir;
        private String mFileName;
        private File mFile;
        private File mTempFile;
        private final DownloadControlor mControlor;
        private final OnDownloadListener mListener;
        private int mContentLength;
        private int mReceivedSize;

        public DownloadHelper(String url, File dir, String fileName,
                DownloadControlor controlor, OnDownloadListener listener) {
            mUrl = url;
            mDir = dir;
            mFileName = fileName;
            mControlor = controlor;
            mListener = listener;
        }

        @Override
        public URL getUrl() throws MalformedURLException {
            return new URL(mUrl);
        }

        private void checkStop() throws StopRequestException {
            if (mControlor != null && mControlor.isStop()) {
                throw new StopRequestException();
            }
        }

        @Override
        public void onBeforeConnect(HttpURLConnection conn) throws Exception {
            checkStop();

            conn.setRequestMethod("GET");
            // Sometimes for application/octet-stream, we need to add this line
            // to get content length
            conn.setRequestProperty("Range", "bytes=0-");

            if (mListener != null) {
                mListener.onStartConnecting();
            }
        }

        private int getContentLength(HttpURLConnection conn) {
            int contentLength = conn.getContentLength();
            String range;
            if (contentLength == -1 &&
                    (range = conn.getHeaderField("Content-Range")) != null) {
                // Content-Range looks like bytes 500-999/1234
                int index = range.indexOf('/');
                if (index != -1) {
                    contentLength = Utils.parseIntSafely(range.substring(index + 1), -1);
                }
            }
            return contentLength;
        }

        @Override
        public void onRedirect(@NonNull URL newURL) {
            // TODO
        }

        /**
         * Get new extension
         *
         * @param ext new extension
         * @return null for not changed
         */
        protected String fixNewExtension(String ext) {
            return null;
        }

        @Override
        public Object onAfterConnect(HttpURLConnection conn) throws Exception {
            // Check stop
            if (mControlor != null && mControlor.isStop())
                throw new StopRequestException();

            String contentType = conn.getContentType();
            int index = contentType.indexOf(';');
            if (index != -1)
                contentType = contentType.substring(0, index);

            mContentLength = getContentLength(conn);
            if (mListener != null) {
                mListener.onStartDownloading(mContentLength);
            }

            // Fix extension
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String newExtension = mime.getExtensionFromMimeType(contentType);
            if (newExtension == null) {
                index = contentType.lastIndexOf('/');
                if (index != -1) {
                    newExtension = contentType.substring(index + 1);
                }
            }
            newExtension = fixNewExtension(newExtension);
            if (newExtension != null) {
                index = mFileName.lastIndexOf('.');
                if (index == -1) {
                    mFileName = mFileName + '.' + newExtension;
                } else {
                    mFileName = mFileName.substring(0, index) + '.' + newExtension;
                }
            }

            // Make sure parent exist
            if (!mDir.mkdirs() && !mDir.isDirectory()) {
                throw new FileNotFoundException("Can not create dir " +
                        mDir.getPath());
            }

            mFile = new File(mDir, mFileName);
            mTempFile = new File(mDir, mFileName + DOWNLOAD_EXTENSION);

            // Transfer
            transferData(conn.getInputStream(), new FileOutputStream(mTempFile));

            // Get ok, rename
            Utils.deleteFile(mFile);
            if (!mTempFile.renameTo(mFile)) {
                throw new FileNotFoundException("Rename " + mTempFile.getPath() +
                        " to " + mFile.getPath() + " error");
            }

            // Callback
            if (mListener != null) {
                mListener.onSuccess();
            }

            // We do not need result
            return null;
        }

        private void transferData(InputStream in, OutputStream out)
                throws Exception {
            final byte data[] = new byte[512 * 1024];
            mReceivedSize = 0;

            while (true) {
                // Check stop first
                checkStop();

                int bytesRead = in.read(data);
                if (bytesRead == -1)
                    break;
                out.write(data, 0, bytesRead);
                mReceivedSize += bytesRead;

                if (mListener != null) {
                    mListener.onDownload(mReceivedSize, mContentLength);
                }
            }

            if (mContentLength != -1 && mReceivedSize != mContentLength) {
                throw new UncompletedException("Received size is " + mReceivedSize
                        + ", but ContentLength is " + mContentLength);
            }
        }

        @Override
        public boolean onRetry(Exception previousException) {
            // Delete unfinished file
            Utils.deleteFile(mTempFile);
            Utils.deleteFile(mFile);

            if (previousException instanceof StopRequestException) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void onRequestFailed(Exception exception) {
            if (mListener != null) {
                if (exception instanceof StopRequestException) {
                    mListener.onStop();
                } else {
                    mListener.onFailure(exception);
                }
            }
        }
    }

    /**
     * Http GET method
     * @param url the url to get
     * @return body
     */
    public String get(String url) throws Exception {
        return (String) requst(new GetHelper(url));
    }

    /**
     * Post form data
     * @param url the url to post
     * @param args the form to post
     * @return body
     */
    public String postForm(String url, String[][] args) throws Exception {
        return (String) requst(new PostFormHelper(url, args));
    }

    /**
     * Post json data
     * @param url the url to post
     * @param json the json to post
     * @return body
     */
    public String postJson(String url, JSONObject json) throws Exception {
        return (String) requst(new PostJsonHelper(url, json));
    }

    /**
     * Post data, multipart/form-data
     * @param url the url to post
     * @param dataList the data list to post
     * @return body
     */
    public String postFormData(String url, List<FormData> dataList) throws Exception {
        return (String)requst(new PostFormDataHelper(url, dataList));
    }

    /**
     * Get image
     * @param url the url to get image
     * @return bitmap
     */
    public Bitmap getImage(String url) throws Exception {
        return (Bitmap) requst(new GetImageHelper(url));
    }

    /**
     * Init DownloadHelper
     *
     * @param url the url to download
     * @param dir the dir to download file
     * @param file the file name
     * @param controlor download control
     * @param listener download listener
     */
    public void download(String url, File dir, String file,
            DownloadControlor controlor, OnDownloadListener listener) throws Exception {
        requst(new DownloadHelper(url, dir, file, controlor, listener));
    }
}
