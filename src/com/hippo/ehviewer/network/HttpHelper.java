package com.hippo.ehviewer.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.http.conn.ConnectTimeoutException;

import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * It is not thread safe
 * 
 * @author Hippo
 *
 */
public class HttpHelper {
    private static final String TAG = "HttpHelper";
    
    private static final String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.76 Safari/537.36";
    private static final int DEFAULT_TIMEOUT = 5 * 1000;
    
    private int lastEMsgId = R.string.em_unknown_error;
    
    /**
     * Get last error message id
     * @return
     */
    public int getLastEMsgId() {
        return lastEMsgId;
    }
    
    public Bitmap getImage(String urlStr, String key,
            LruCache<String, Bitmap> memoryCache,
            DiskCache diskCache, boolean getImage) {
        HttpURLConnection conn = null;
        ByteArrayOutputStream baos = null;
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            Log.d(TAG, "Get Image " + urlStr);
            URL url = new URL(urlStr);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setConnectTimeout(DEFAULT_TIMEOUT);
            conn.setReadTimeout(DEFAULT_TIMEOUT);
            conn.connect();
            is = conn.getInputStream();
            
            if (diskCache == null) {
                if (getImage
                        && (bitmap = BitmapFactory.decodeStream(is, null, Ui.getBitmapOpt())) != null) {
                    if (memoryCache != null)
                        memoryCache.put(key, bitmap);
                }
            } else {
                boolean twice = memoryCache != null || getImage;
                if (twice) {
                    baos = new ByteArrayOutputStream();
                    Util.copy(is, baos);
                    byte[] bytes = baos.toByteArray();
                    // To disk cache
                    is = new ByteArrayInputStream(bytes);
                    diskCache.put(key, is);
                    is.close();
                    // 
                    is = new ByteArrayInputStream(bytes);
                    Bitmap bmp = BitmapFactory.decodeStream(is, null, Ui.getBitmapOpt());
                    if (bmp != null) {
                        if (memoryCache != null)
                            memoryCache.put(key, bmp);
                        if (getImage)
                            bitmap = bmp;
                    }
                    is.close();
                    baos.close();
                } else {
                    diskCache.put(key, is);
                }
            }
        } catch (MalformedURLException e) {
            lastEMsgId = R.string.em_url_error;
            e.printStackTrace();
        } catch (ConnectTimeoutException e) {
            lastEMsgId = R.string.em_timeout;
            e.printStackTrace();
        } catch (UnknownHostException e) {
            lastEMsgId = R.string.em_no_network_2;
            e.printStackTrace();
        } catch (IOException e) {
            lastEMsgId = R.string.em_network_error;
            e.printStackTrace();
        } finally {
            if (baos != null)
                Util.closeStreamQuietly(baos);
            if (is != null)
                Util.closeStreamQuietly(is);
            if (conn != null)
                conn.disconnect();
        }
        return bitmap;
    }
}
