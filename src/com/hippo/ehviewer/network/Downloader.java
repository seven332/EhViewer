package com.hippo.ehviewer.network;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.http.conn.ConnectTimeoutException;

import android.content.Context;
import com.hippo.ehviewer.util.Log;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.exception.FileException;
import com.hippo.ehviewer.exception.NoSdCardException;
import com.hippo.ehviewer.exception.RedirectionException;
import com.hippo.ehviewer.exception.ResponseCodeException;
import com.hippo.ehviewer.exception.StopRequestException;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Util;

// TODO 添加检查次磁盘空间

/**
 * Thread unsafe
 * 
 * @author Hippo
 *
 */
public class Downloader implements Runnable {
    private static final String TAG = "Downloader";
    
    public static final int NONE = 0x0;
    public static final int READY = 0x1;
    public static final int CONNECTING = 0x2;
    public static final int DOWNLOADING = 0x3;
    public static final int FAILED = 0x4;
    public static final int COMPLETED = 0x5;
    public static final int STOPED = 0x6;
    
    private static final int MAX_RETRY = 3;
    
    private Context mContext;
    
    private File mFile;
    private File mFolder;
    private String mPath;
    private String mFileName;
    private URL mUrl;
    private Controlor mContorlor;
    
    private int mTotalSize;
    // If support Content-Range, start from it
    private int mDownloadSize = 0;
    private int mRedirectionCount = 0;
    
    private int status = NONE;
    private OnDownloadListener mListener;
    
    private Exception mException;
    
    public interface OnDownloadListener {
        public void onDownloadStartConnect();
        public void onDownloadStartDownload(int totalSize);
        public void onDownloadStatusUpdate(int downloadSize, int totalSize);
        /**
         * FAILED or COMPLETED or STOPED
         * @param status
         */
        public void onDownloadOver(int status, String eMsg);
    }
    
    public class Controlor {
        private boolean mStop = false;
        
        public synchronized void stop() {
            mStop = true; 
        }
        
        public synchronized void reset() {
            mStop = false; 
        }
        
        public synchronized boolean isStop() {
            return mStop; 
        }
    }
    
    public Controlor resetData(String path, String fileName, String urlStr) throws MalformedURLException {
        mPath = path;
        mFileName = fileName;
        mUrl = new URL(urlStr);
        mException = null;
        
        mDownloadSize = 0;
        mRedirectionCount = 0;
        status = READY;
        
        mContorlor.reset();
        return mContorlor;
    }
    
    public void setOnDownloadListener(OnDownloadListener listener) {
        mListener = listener;
    }
    
    public Downloader(Context context) {
        mContext = context;
        mContorlor = new Controlor();
    }
    
    
    public String getEMsg() {
        if (mException == null)
            return mContext.getString(R.string.em_unknown_error);
        
        else if (mException instanceof MalformedURLException)
            return mContext.getString(R.string.em_url_format_error);
        
        else if (mException instanceof ConnectTimeoutException ||
                mException instanceof SocketTimeoutException)
            return mContext.getString(R.string.em_timeout);
        
        else if (mException instanceof UnknownHostException)
            return mContext.getString(R.string.em_unknown_host);
        
        else if (mException instanceof ResponseCodeException)
            return String.format(mContext.getString(R.string.em_unexpected_response_code),
                    ((ResponseCodeException)mException).getResponseCode());
        
        else if (mException instanceof RedirectionException)
            return mContext.getString(R.string.em_redirection_error);
        
        else if (mException instanceof SocketException)
            return "SocketException : " + mException.getMessage();
        
        else if (mException instanceof NoSdCardException)
            return mContext.getString(R.string.em_no_sdcard);
        
        else if (mException instanceof FileException)
            return ((FileException)mException).getMessage();
        
        else
            return mException.getMessage();
    }
    
    @Override
    public void run() {
        int retryTimes = 0;
        if (Cache.hasSdCard()) {
            
            boolean checkFolder = false;
            
            try {
                initFolder();
                checkFolder = true;
            } catch (Exception e) {
                mException = e;
                e.printStackTrace();
            }
            
            while(checkFolder) {
                try {
                    if (mContorlor.isStop())
                        throw new StopRequestException();
                    mRedirectionCount = 0;
                    status = READY;
                    executeDownload();
                    status = COMPLETED;
                    break;
                } catch (Exception e) {
                    mException = e;
                    e.printStackTrace();
                    if (mContorlor.isStop() || retryTimes > MAX_RETRY) {
                        status = FAILED;
                        break;
                    }
                    retryTimes++;
                }
            }
        } else
            mException = new NoSdCardException();
        
        if (mContorlor.isStop())
            status = STOPED;
        
        if (status != COMPLETED && mFile != null)
            mFile.delete();
        
        if (mListener != null)
            mListener.onDownloadOver(status, getEMsg());
    }
    
    public int getStatus() {
        return status;
    }
    
    private void initFolder() throws Exception {
        mFolder = new File(mPath);
        
        if (!mFolder.exists()) {
            if (!mFolder.mkdirs())
                throw new FileException(FileException.MKDIR_ERROR);
        } else if (!mFolder.isDirectory()){
            throw new FileException(FileException.NOT_DIR);
        }
        
        if (!mFolder.canWrite())
            throw new FileException(FileException.CANNOT_WRITE);
    }
    
    private void executeDownload() throws Exception {
        while (mRedirectionCount++ < Constant.MAX_REDIRECTS) {
            HttpURLConnection conn = null;
            try {
                Log.d(TAG, "Get file " + mUrl.toString());
                
                status = CONNECTING;
                if (mListener != null)
                    mListener.onDownloadStartConnect();
                
                conn = (HttpURLConnection)mUrl.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.addRequestProperty("Range", "bytes=" + mDownloadSize + "-");
                conn.setConnectTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setReadTimeout(Constant.DEFAULT_TIMEOUT);
                conn.connect();
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_PARTIAL:
                        processResponseHeaders(conn);
                        status = DOWNLOADING;
                        if (mListener != null)
                            mListener.onDownloadStartDownload(mTotalSize);
                        transferData(conn);
                        return;

                    case HttpURLConnection.HTTP_MOVED_PERM:
                    case HttpURLConnection.HTTP_MOVED_TEMP:
                    case HttpURLConnection.HTTP_SEE_OTHER:
                    case Constant.HTTP_TEMP_REDIRECT:
                        final String location = conn.getHeaderField("Location");
                        mUrl = new URL(mUrl, location);
                        continue;

                    default:
                        throw new ResponseCodeException(responseCode);
                }
            } catch (Exception e) {
                throw e;
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        }
        throw new RedirectionException();
    }
    
    /**
     * Get Response Headers Info
     * 
     * @param conn
     */
    private void processResponseHeaders(HttpURLConnection conn) {
        mTotalSize = conn.getContentLength();
        
        // bytes 500-999/1234
        String range;
        if ((range = conn.getHeaderField("Content-Range")) != null) { // Support Content-Range
            boolean newNum = true;
            int numIndex = 0;
            int num = 0;
            int length = range.length();
            char ch;
            for (int i = 0; i < length; i++) {
                ch = range.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    if (newNum) {
                        newNum = false;
                        num = ch - '0';
                    } else
                        num = num * 10 + ch - '0';
                } else {
                    if (!newNum) {
                        newNum = true;
                        if (numIndex == 0)
                            mDownloadSize = num;
                        else if (numIndex == 2)
                            mTotalSize = num;
                        numIndex++;
                    }
                }
            }
            if (numIndex == 2)
                mTotalSize = num;
        } else { // Do not support Content-Range, restart download
            mDownloadSize = 0;
        }
    }
    
    /**
     * Transfer data from the given connection to the destination file.
     */
    private void transferData(HttpURLConnection conn) throws Exception {
        InputStream in = null;
        RandomAccessFile raf = null;
        
        if (mContorlor.isStop())
            throw new StopRequestException();
        
        try {
            try {
                in = conn.getInputStream();
            } catch (IOException e) {
                throw e;  // TODO
            }

            try {
                mFile = new File(mPath, mFileName);
                raf = new RandomAccessFile(mFile, "rw");
                raf.seek(mDownloadSize);
            } catch (IOException e) {
                throw new FileException(FileException.CREATE_FILE_ERROR);
            }

            // Start streaming data, periodically watch for pause/cancel
            // commands and checking disk space as needed.
            transferData(in, raf);
        } finally {
            if (in != null)
                Util.closeStreamQuietly(in);
            if (raf != null)
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
    
    
    /**
     * Transfer as much data as possible from the HTTP response to the
     * destination file.
     */
    @SuppressWarnings("unused")
    private void transferData(InputStream in, OutputStream out)
            throws Exception {
        final byte data[] = new byte[Constant.BUFFER_SIZE];
        while (true) {
            
            if (mContorlor.isStop())
                throw new StopRequestException();
            
            int bytesRead = readFromResponse(data, in);
            if (bytesRead == -1) { // success, end of stream already reached
                return;
            }
            
            writeDataToDestination(data, bytesRead, out);
            mDownloadSize += bytesRead;
            if (mListener != null)
                mListener.onDownloadStatusUpdate(mDownloadSize, mTotalSize);
            
        }
    }
    
    /**
     * Transfer as much data as possible from the HTTP response to the
     * destination file.
     */
    private void transferData(InputStream in, RandomAccessFile raf)
            throws Exception {
        final byte data[] = new byte[Constant.BUFFER_SIZE];
        while (true) {
            if (mContorlor.isStop())
                throw new StopRequestException();
            
            int bytesRead = readFromResponse(data, in);
            if (bytesRead == -1) { // success, end of stream already reached
                return;
            }
            
            writeDataToDestination(data, bytesRead, raf);
            mDownloadSize += bytesRead;
            if (mListener != null)
                mListener.onDownloadStatusUpdate(mDownloadSize, mTotalSize);
        }
    }
    
    /**
     * Read some data from the HTTP response stream, handling I/O errors.
     * @param data buffer to use to read data
     * @param entityStream stream for reading the HTTP response entity
     * @return the number of bytes actually read or -1 if the end of the stream has been reached
     */
    private int readFromResponse(byte[] data, InputStream entityStream)
            throws Exception {
        try {
            return entityStream.read(data);
        } catch (IOException e) {
            // TODO: handle stream errors the same as other retries
            throw e;
        }
    }
    
    
    /**
     * Write a data buffer to the destination file.
     * @param data buffer containing the data to write
     * @param bytesRead how many bytes to write from the buffer
     */
    private void writeDataToDestination(byte[] data, int bytesRead, OutputStream out)
            throws Exception {
        try {
            out.write(data, 0, bytesRead);
            return;
        } catch (IOException e) {
            // TODO: better differentiate between DRM and disk failures
            // TODO: check disk is full
            throw e;
        }
    }
    
    /**
     * Write a data buffer to the destination file.
     * @param data buffer containing the data to write
     * @param bytesRead how many bytes to write from the buffer
     */
    private void writeDataToDestination(byte[] data, int bytesRead, RandomAccessFile raf)
            throws Exception {
        try {
            raf.write(data, 0, bytesRead);
            return;
        } catch (IOException e) {
            // TODO: better differentiate between DRM and disk failures
            // TODO: check disk is full
            throw e;
        }
    }
}
