package com.hippo.ehviewer.gallery.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import com.hippo.ehviewer.util.Future;
import com.hippo.ehviewer.util.FutureListener;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.ThreadPool.Job;
import com.hippo.ehviewer.util.ThreadPool.JobContext;
import com.hippo.ehviewer.util.Util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import com.hippo.ehviewer.util.Log;
import android.util.SparseArray;

// TODO 初期建立一个线程检索文件夹中所有文件以寻找到所有图片文件

/**
 * @author Hippo
 * 
 * ImageSet 用于记录漫画的下载信息
 * 对于已完成下载的则无特殊之处。
 * 对于未完成下载的要记录，开始下载之处，目前下载截止之处，
 * 以及下载失败之处。
 * 同时拥有检查该目录识别图片文件。
 */
public class ImageSet {
    
    @SuppressWarnings("unused")
    private static final String TAG = "ImageSet";
    
    private static final String GIF_EXTENSION = "gif";
    
    public static final int TYPE_NONE = 0x0;
    public static final int TYPE_BITMAP = 0x1;
    public static final int TYPE_MOVIE = 0x2;
    
    public static final int INVALID_ID = -1;
    
    public static final int STATE_NONE = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_LOADED = 2;
    public static final int STATE_FAIL = 3;
    
    protected Context mContext;
    protected String mGid;
    protected File mFolder;
    protected int mSize;
    
    private SparseArray<ImageData> mImagesDate;
    
    private ThreadPool mThreadPool;
    private OnStateChangeListener mOnStateChangeListener;
    
    public interface OnStateChangeListener {
        void onStateChange(int index, int state);
    }
    
    public interface OnDecodeOverListener {
        /**
         * May return Bitmap or Movie
         * 
         * @param res
         * @param index
         */
        void onDecodeOver(Object res, int index);
    }
    
    private class ImageData {
        int state;
        String fileName;
        int type;
        
        public ImageData() {
            state = STATE_NONE;
            fileName = null;
            type = TYPE_NONE;
        }
    }
    
    // [startIndex, endIndex)
    public ImageSet(Context context, String gid, File folder, int size, int startIndex, int endIndex,
            Set<Integer> failIndexSet) {
        
        if (folder == null || !folder.isDirectory())
            size = 0;
        
        
        // TODO
        /*
        if (folder == null)
            throw new IllegalArgumentException("Folder is null");
        if (!folder.isDirectory())
            throw new IllegalArgumentException("Folder is not directory, path is " + folder.getPath());
        */
        
        if (size < 0)
            size = 0;
        if (endIndex > size)
            endIndex = size;
        if (startIndex < 0)
            startIndex = 0;
        if (startIndex > endIndex)
            startIndex = endIndex;
        
        // TODO
        /*
        if (size < 0 || startIndex < 0
                || startIndex > endIndex || endIndex > size)
            throw new IllegalArgumentException("size or index value error, size = "
                + size + ", startIndex = " + startIndex
                + ", endIndex = " + endIndex + ", path is " + folder.getPath());
        */
        
        mContext = context;
        mGid = gid;
        mFolder = folder;
        mSize = size;
        
        // TODO sometimes size is too large
        mImagesDate = new SparseArray<ImageData>(mSize);
        int i = 0;
        for (; i < startIndex; i++) {
            mImagesDate.append(i, new ImageData());
        }
        for (; i < endIndex; i++) {
            if (failIndexSet != null && failIndexSet.contains(i)) {
                ImageData imageData = new ImageData();
                imageData.state = STATE_FAIL;
                mImagesDate.append(i, imageData);
            }
            else {
                // Get file name only when you need it
                ImageData imageData = new ImageData();
                imageData.state = STATE_LOADED;
                mImagesDate.append(i, imageData);
            }
        }
        for (; i < size; i++) {
            mImagesDate.append(i, new ImageData());
        }
        
        mThreadPool = new ThreadPool(1, 1);
    }

    public int getSize() {
        return mSize;
    }
    
    public void setOnStateChangeListener(OnStateChangeListener l) {
        mOnStateChangeListener = l;
    }
    
    public int getImage(final int index, final OnDecodeOverListener listener) {
        final ImageData imageData = mImagesDate.get(index);
        int state;
        if (imageData == null)
            state = INVALID_ID;
        else
            state = imageData.state;
        if (state == STATE_LOADED && listener != null) {
            mThreadPool.submit(new Job<Object>() {
                @Override
                public Object run(JobContext jc) {
                    Object res = null;
                    if (imageData.fileName == null
                            && !getFileForName(getFileNameForIndex(index), imageData))
                        return null;
                    
                    File file = new File(mFolder, imageData.fileName);
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(file);
                        if (imageData.type == TYPE_BITMAP) {
                            BitmapFactory.Options opt = new BitmapFactory.Options();
                            // TODO why only ARGB_8888 always work well, other may slit image
                            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
                            res = BitmapFactory.decodeStream(fis, null, opt);
                        } else if (imageData.type == TYPE_MOVIE) {
                            res = Movie.decodeStream(fis);
                        } else {
                            // TYPE_NONE or something else, get error
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (fis != null)
                            Util.closeStreamQuietly(fis);
                    }
                    return res;
                }
            }, new FutureListener<Object>() {
                @Override
                public void onFutureDone(Future<Object> future) {
                    listener.onDecodeOver(future.get(), index);
                }
            });
        }
        return state;
    }
    
    @SuppressLint("DefaultLocale")
    public String getFileNameForIndex(int index) {
        return String.format("%05d", index + 1);
    }
    
    public boolean getFileForName(String name, ImageData imageData) {
        String[] list = mFolder.list();
        for (String item : list) {
            if(name.equals(Util.getName(item))) {
                imageData.fileName = item;
                if (Util.getExtension(item).toLowerCase().equals(GIF_EXTENSION))
                    imageData.type = TYPE_MOVIE;
                else
                    imageData.type = TYPE_BITMAP;
                return true;
            }
        }
        return false;
    }
    
    public void changeState(int index, int state) {
        final ImageData imageData = mImagesDate.get(index);
        if(imageData != null) {
            imageData.state = state;
            if (mOnStateChangeListener != null) {
                mOnStateChangeListener.onStateChange(index, state);
            }
        }
    }
    
    
    /**
     * mSize is faithful
     * 
     */
    public void scan() {
        String[] fileList = mFolder.list();
    }
}
