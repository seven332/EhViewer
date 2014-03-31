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
import android.util.Log;
import android.util.SparseArray;

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
    
    private static final String TAG = "ImageSet";
    
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
        void onDecodeOver(Bitmap bmp, int index);
    }
    
    private class ImageData {
        int state;
        String fileName;
        
        public ImageData() {
            state = STATE_NONE;
            fileName = null;
        }
    }
    
    // [startIndex, endIndex)
    public ImageSet(Context context, String gid, File folder, int size, int startIndex, int endIndex,
            Set<Integer> failIndexSet) {
        if (folder == null || !folder.isDirectory())
            throw new IllegalArgumentException("Folder is null or not directory");
        if (size < 0 || startIndex < 0
                || startIndex > endIndex || endIndex > size)
            throw new IllegalArgumentException("size or index value error");
        
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
            mThreadPool.submit(new Job<Bitmap>() {
                @Override
                public Bitmap run(JobContext jc) {
                    Bitmap bmp = null;
                    if (imageData.fileName == null) {
                        imageData.fileName = getFileForName(getFileNameForIndex(index));
                    }
                    if (imageData.fileName == null)
                        return null;
                    
                    
                    Log.d(TAG, imageData.fileName);
                    
                    File file = new File(mFolder, imageData.fileName);
                    try {
                        BitmapFactory.Options opt = new BitmapFactory.Options();
                        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        FileInputStream fis = new FileInputStream(file);
                        bmp = BitmapFactory.decodeStream(fis, null, opt);
                        fis.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return bmp;
                }
            }, new FutureListener<Bitmap>() {
                @Override
                public void onFutureDone(Future<Bitmap> future) {
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
    
    public String getFileForName(String name) {
        String[] list = mFolder.list();
        for (String item : list) {
            if(name.equals(Util.getName(item)))
                return item;
        }
        return null;
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
