package com.hippo.ehviewer.gallery.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hippo.ehviewer.util.Future;
import com.hippo.ehviewer.util.FutureListener;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.ThreadPool.Job;
import com.hippo.ehviewer.util.ThreadPool.JobContext;
import com.hippo.ehviewer.util.Util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseIntArray;



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
    
    private File mFolder;
    private int mSize;
    
    private SparseIntArray imagesState;
    
    private ThreadPool mThreadPool;
    
    public interface onStateChangeListener {
        
    }
    
    public interface OnDecodeOverListener {
        void onDecodeOver(Bitmap bmp, int index);
    }
    
    // [startIndex, endIndex)
    public ImageSet(File folder, int size, int startIndex, int endIndex,
            Set<Integer> failIndexSet) {
        if (folder == null || !folder.isDirectory())
            throw new IllegalArgumentException("Folder is null or not directory");
        if (size < 0 || startIndex < 0
                || startIndex > endIndex || endIndex > size)
            throw new IllegalArgumentException("size or index value error");
        
        mFolder = folder;
        mSize = size;
        
        imagesState = new SparseIntArray();
        int i = 0;
        for (; i < startIndex; i++) {
            imagesState.append(i, STATE_NONE);
        }
        for (; i < endIndex; i++) {
            if (failIndexSet != null && failIndexSet.contains(i))
                imagesState.append(i, STATE_FAIL);
            else
                imagesState.append(i, STATE_LOADED);
        }
        for (; i < size; i++) {
            imagesState.append(i, STATE_NONE);
        }
        
        
        mThreadPool = new ThreadPool(1, 1);
    }
    
    public int getSize() {
        return mSize;
    }
    
    public int getImage(final int index, final OnDecodeOverListener listener) {
        int state = imagesState.get(index, INVALID_ID);
        if (state == STATE_LOADED && listener != null) {
            mThreadPool.submit(new Job<Bitmap>() {
                @Override
                public Bitmap run(JobContext jc) {
                    Bitmap bmp = null;
                    // TODO
                    String fileName = getFileForName(String.format("%05d", index + 1));
                    if (fileName == null)
                        return null;
                    File file = new File(mFolder, fileName);
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
    
    public String getFileForName(String name) {
        String[] list = mFolder.list();
        for (String item : list) {
            if(name.equals(Util.getName(item)))
                return item;
        }
        return null;
    }
    
    /**
     * mSize is faithful
     * 
     */
    public void scan() {
        String[] fileList = mFolder.list();
    }
}
