package com.hippo.ehviewer.gallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;

import com.hippo.conaco.BitmapPool;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.PriorityThread;
import com.hippo.yorozuya.Say;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// TODO Support uniFile
public class ZipGalleryProvider implements GalleryProvider, Runnable {

    private static final String TAG = ZipGalleryProvider.class.getSimpleName();

    private static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif"};

    private volatile boolean mRestart = false;
    private volatile boolean mStop = false;

    private List<GalleryProviderListener> mGalleryProviderListeners = new ArrayList<>();

    private ZipFile mZipFile;
    private volatile int mSize = -1;
    private List<String> mFilenames = new ArrayList<>();
    private BlockingQueue<Integer> mRequestQueue = new LinkedBlockingQueue<>();
    private BitmapPool mBitmapPool = new BitmapPool();

    private final Object mLock = new Object();

    public ZipGalleryProvider(File file) throws IOException {
        mZipFile = new ZipFile(file);

        Thread thread = new PriorityThread(this, "ZipGalleryProvider", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    @Override
    public Object request(int index) {
        if (index < 0 || index >= mSize) {
            return GalleryProvider.RESULT_OUT_OF_RANGE;
        } else {
            mRequestQueue.add(index);
            synchronized (mLock) {
                mLock.notify();
            }
            return GalleryProvider.RESULT_WAIT;
        }
    }

    @Override
    public Object forceRequest(int index) {
        // TODO
        return null;
    }

    public void stop() {
        mStop = true;
        setPause(false);
        synchronized (mLock) {
            mLock.notify();
        }
        IOUtils.closeQuietly(mZipFile);
    }

    @Override
    public void restart() {
        mRestart = true;
        synchronized (mLock) {
            mLock.notify();
        }
    }

    @Override
    public void retry() {
        // TODO
    }

    @Override
    public int size() {
        return mSize;
    }

    @Override
    public void setPause(boolean pause) {
        // TODO
    }

    @Override
    public void releaseBitmap(Bitmap bitmap) {
        mBitmapPool.addReusableBitmap(bitmap);
    }

    @Override
    public void addGalleryProviderListener(GalleryProviderListener listener) {
        mGalleryProviderListeners.add(listener);
    }

    @Override
    public void removeGalleryProviderListener(GalleryProviderListener listener) {
        mGalleryProviderListeners.remove(listener);
    }

    private void reportTotallyFailed(final Exception e) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onTotallyFailed(e);
                }
            }
        });
    }

    private void reportGetSize(final int size) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onGetSize(size);
                }
            }
        });
    }

    private void reportGetBitmap(final int index, final Bitmap bitmap) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onGetImage(index, bitmap);
                }
            }
        });
    }

    private Bitmap getBitmap(String filename) {
        InputStream is = null;
        try {
            ZipEntry zipEntry = mZipFile.getEntry(filename);
            if (zipEntry != null) {
                is = mZipFile.getInputStream(zipEntry);

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                BitmapFactory.decodeStream(is, null, options);
                is.close();

                // Check out size
                if (options.outWidth > 0 || options.outHeight > 0) {
                    options.inJustDecodeBounds = false;
                    options.inMutable = true;
                    options.inSampleSize = 1;
                    options.inBitmap = mBitmapPool.getInBitmap(options);

                    is = mZipFile.getInputStream(zipEntry);
                    return BitmapFactory.decodeStream(is, null, options);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
        }

        return null;
    }

    public void doIt() throws Exception {
        // Get all image name and count
        List<String> filenames = mFilenames;
        Enumeration<? extends ZipEntry> enumeration = mZipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            String filename = zipEntry.getName();
            if (!zipEntry.isDirectory() && StringUtils.endsWith(filename, IMAGE_EXTENSIONS)) {
                filenames.add(filename);
            }
        }
        Collections.sort(filenames);

        int size = filenames.size();
        reportGetSize(size);
        mSize = size;

        while (!mStop) {
            Integer index = mRequestQueue.poll();
            if (index == null) {
                synchronized (mLock) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
                continue;
            }

            int indexI = index;
            if (indexI < 0 || index >= filenames.size()) {
                reportGetBitmap(indexI, null);
            } else {
                reportGetBitmap(indexI, getBitmap(filenames.get(indexI)));
            }
        }
    }

    @Override
    public void run() {
        Say.d(TAG, "ZipGalleryProvider start");

        while (!mStop) {
            try {
                doIt();
            } catch (Exception e) {
                // Stop action, disconnect current HttpRequest may raise Exception
                // No need to report it
                if (mStop) {
                    break;
                }

                mRestart = false;
                reportTotallyFailed(e);

                Say.d(TAG, "ZipGalleryProvider totallyFailed");

                while (!mStop && !mRestart) {
                    synchronized (mLock) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException ex) {
                            // Ignore
                        }
                    }
                }
            }
        }

        Say.d(TAG, "ZipGalleryProvider end");
    }
}
