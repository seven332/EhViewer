package com.hippo.ehviewer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.jakewharton.disklrucache.DiskLruCache;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.os.Environment;
import com.hippo.ehviewer.util.Log;

// For Gallery cover key is gid,
// for example , gid is 618395, token is 0439fa3666,
// so key is 618395

// For Gallery preview key is gid-preview-num-row,
// for example , gid is 618395, token is 0439fa3666, page2
// so key is 618395-preview-2-0

// For Gallery image key is gid-preview-num,
// for example , gid is 618395, token is 0439fa3666, image2
// so key is 618395-image-2

public class DiskCache {
    private static String TAG = "ImageDiskCache";
    
    private static final int VALUE_IDX = 0;
    
    private Context mContext;
    private BitmapFactory.Options opt;
    private final DiskLruCache diskLruCache;
    
    public DiskCache(Context context, String path, long maxSize) throws NameNotFoundException, IOException {
        this.mContext = context;
        
        opt = Ui.getBitmapOpt();
        if (!Cache.hasSdCard()) {
            throw new IllegalStateException("Has no SdCard.");
        }
        
        File dir = new File(Environment.getExternalStorageDirectory() + path);
        int version = mContext.getPackageManager().getPackageInfo(
                mContext.getPackageName(), 0).versionCode;
        version = 1;
        diskLruCache = DiskLruCache.open(dir, version, 1, maxSize);
    }
    
    public long size() {
        return diskLruCache.size();
    }
    
    public long maxSize() {
        return diskLruCache.getMaxSize();
    }
    
    public void setMaxSize(long maxSize) {
        diskLruCache.setMaxSize(maxSize);
    }
    
    public void close() {
        try {
            diskLruCache.close();
        } catch (IOException e) {
            Log.d(TAG, "diskLruCache close error !");
        }
    }
    
    public boolean hasKey(String key) {
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = diskLruCache.get(key);
        } catch (IOException e) {
        }
        if(snapshot==null)
            return false;
        snapshot.close();
        return true;
    }
    
    public Object get(String key, int type) {
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = diskLruCache.get(key);
        } catch (IOException e) {
        }
        if (snapshot == null)
            return null;
        InputStream is = snapshot.getInputStream(VALUE_IDX);
        if (type == Util.BITMAP) {
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opt);
            return bitmap;
        } else if (type == Util.MOVIE) {
            is = new BufferedInputStream(is, 16 * 1024);
            is.mark(16 * 1024);
            Movie movie = Movie.decodeStream(is);
            return movie;
        }
        return null;
    }
    
    public boolean put(String key, InputStream is) throws IOException {
        DiskLruCache.Editor editor = diskLruCache.edit(key);
        if (editor == null)
            return false;
        BufferedOutputStream bos = new BufferedOutputStream(editor.newOutputStream(VALUE_IDX));
        OutputStream os = new CacheOutputStream(bos, editor);
        Util.copy(is, os);
        return true;
    }
    
    public void clear() {
        try {
            diskLruCache.delete();
        } catch (IOException e) {
        }
    }
    
    private class CacheOutputStream extends FilterOutputStream {

        private final DiskLruCache.Editor editor;
        private boolean failed = false;

        private CacheOutputStream(OutputStream os, DiskLruCache.Editor editor) {
            super(os);
            this.editor = editor;
        }

        @Override
        public void close() throws IOException {
            IOException closeException = null;
            try {
                super.close();
            } catch (IOException e) {
                closeException = e;
            }

            if (failed) {
                editor.abort();
            } else {
                editor.commit();
            }

            if (closeException != null) throw closeException;
        }

        @Override
        public void flush() throws IOException {
            try {
                super.flush();
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void write(int oneByte) throws IOException {
            try {
                super.write(oneByte);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            try {
                super.write(buffer);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            try {
                super.write(buffer, offset, length);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }
    }
    /*
    private Context context = null;
    private File dir = null;
    int maxSize = 1024 * 10;
    
    public DiskCache(Context context, String path, int maxSize) {
        this.context = context;
        this.maxSize = maxSize;
        if (maxSize <= 0)
            maxSize = 1024 * 10;

        File rootdir = Environment.getExternalStorageDirectory();
        dir = new File(rootdir + path);
        dir.mkdirs();
    }

    public boolean hasKey(String key) {
        synchronized (key) {
            
            File file = new File(dir, key);
            if (file.exists()) {
                if (file.isDirectory())
                    file.delete();
                else
                    return true;
            }
            return false;
        }
    }

    public Object get(String key, int type) {
        synchronized (key) {
            File file = new File(dir, key);
            if (!file.isFile())
                return null;
            try {
                InputStream is = new FileInputStream(file);
                if (type == Util.BITMAP) {
                    Bitmap bitmap = BitmapFactory.decodeStream(is, null, Util.opt);
                    return bitmap;
                } else if (type == Util.MOVIE) {
                    is = new BufferedInputStream(is, 16 * 1024);
                    is.mark(16 * 1024);
                    Movie movie = Movie.decodeStream(is);
                    return movie;
                }
            } catch (FileNotFoundException e) {
                Log.d(TAG, "WTF, I have checked that the file does exists.");
                e.printStackTrace();
            }
            return null;
        }
    }

    public boolean put(String key, InputStream is) throws IOException {
        synchronized (key) {
            File file = new File(dir, key);
            try {
                file.delete();
                file.createNewFile();
            } catch (IOException e) {
                dir.mkdirs();
                file.delete();
                try {
                    file.createNewFile();
                } catch (IOException e1) {
                    Log.d(TAG, "WTF, mkdirs didn't work? Path is " + file.getPath());
                    e1.printStackTrace();
                    return false;
                }
            }
            try {
                Util.inputStream2File(is, file);
                return true;
            } catch (IOException e) {
                Log.d(TAG, "Put " + key + " error.");
                throw e;
            }
        }
    }
    
    public void clear() {
        clearDirectory(dir);
    }
    
    private void clearDirectory(File dir) {
        
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) clearDirectory(file);
            file.delete();
        }
        
    }*/
    /*
     * private static String getStingMD5(String s) { String re = null; try {
     * MessageDigest messageDigest =MessageDigest.getInstance(Algorithm); byte[]
     * inputByteArray = s.getBytes(); messageDigest.update(inputByteArray);
     * byte[] resultByteArray = messageDigest.digest(); re =
     * byteArrayToHex(resultByteArray); } catch (NoSuchAlgorithmException e) {
     * e.printStackTrace(); Log.d(TAG, e.getMessage()); } return re; }
     * 
     * private static String byteArrayToHex(byte[] byteArray) { char[] hexDigits
     * = {'0','1','2','3','4','5','6','7','8','9', 'A','B','C','D','E','F' };
     * char[] resultCharArray =new char[byteArray.length * 2]; int index = 0;
     * for (byte b : byteArray) { resultCharArray[index++] = hexDigits[b>>> 4 &
     * 0xf]; resultCharArray[index++] = hexDigits[b& 0xf]; } return new
     * String(resultCharArray); }
     */
}
