/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.gallery;

import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.glgallery.GalleryPageView;
import com.hippo.image.Image;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.StringUtils;
import com.hippo.yorozuya.thread.PriorityThread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipGalleryProvider extends GalleryProvider2 implements Runnable {

    private static final String TAG = ZipGalleryProvider.class.getSimpleName();
    private static final AtomicInteger sIdGenerator = new AtomicInteger();

    private final File mFile;
    private final Stack<Integer> mRequests = new Stack<>();
    private final AtomicInteger mDecodingIndex = new AtomicInteger(GalleryPageView.INVALID_INDEX);
    @Nullable
    private Thread mBgThread;
    private volatile int mSize = STATE_WAIT;
    private String mError;

    public ZipGalleryProvider(File file) {
        mFile = file;
    }

    @Override
    public void start() {
        super.start();

        mBgThread = new PriorityThread(this, TAG + '-' + sIdGenerator.incrementAndGet(),
                Process.THREAD_PRIORITY_BACKGROUND);
        mBgThread.start();
    }

    @Override
    public void stop() {
        super.stop();

        if (mBgThread != null) {
            mBgThread.interrupt();
            mBgThread = null;
        }
    }

    @Override
    public int size() {
        return mSize;
    }

    @Override
    protected void onRequest(int index) {
        synchronized (mRequests) {
            if (!mRequests.contains(index) && index != mDecodingIndex.get()) {
                mRequests.add(index);
                mRequests.notify();
            }
        }
        notifyPageWait(index);
    }

    @Override
    protected void onForceRequest(int index) {
        onRequest(index);
    }

    @Override
    protected void onCancelRequest(int index) {
        synchronized (mRequests) {
            mRequests.remove(Integer.valueOf(index));
        }
    }

    @Override
    public String getError() {
        return mError;
    }

    @NonNull
    @Override
    public String getImageFilename(int index) {
        // TODO
        return Integer.toString(index);
    }

    @Override
    public boolean save(int index, @NonNull UniFile file) {
        // TODO
        return false;
    }

    @Nullable
    @Override
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        // TODO
        return null;
    }

    @Override
    public void run() {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(mFile);
        } catch (ZipException e) {
            mError = GetText.getString(R.string.error_invalid_zip_file);
        } catch (FileNotFoundException e) {
            mError = GetText.getString(R.string.error_not_found);
        } catch (IOException e) {
            mError = GetText.getString(R.string.error_reading_failed);
        }

        // Check zip file null
        if (zipFile == null) {
            mSize = STATE_ERROR;

            // Notify to to show error
            notifyDataChanged();

            Log.i(TAG, "ImageDecoder end with error");
            return;
        }

        // Get all image name
        List<String> filenames = new ArrayList<>(zipFile.size());
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            String filename = zipEntry.getName();
            if (!zipEntry.isDirectory() && StringUtils.endsWith(filename, SUPPORT_IMAGE_EXTENSIONS)) {
                filenames.add(filename);
            }
        }
        Collections.sort(filenames);

        // Update size and notify changed
        mSize = filenames.size();
        notifyDataChanged();

        while (!Thread.currentThread().isInterrupted()) {
            int index;
            synchronized (mRequests) {
                if (mRequests.isEmpty()) {
                    try {
                        mRequests.wait();
                    } catch (InterruptedException e) {
                        // Interrupted
                        break;
                    }
                    continue;
                }
                index = mRequests.pop();
                mDecodingIndex.lazySet(index);
            }

            // Check index valid
            if (index < 0 || index >= filenames.size()) {
                mDecodingIndex.lazySet(GalleryPageView.INVALID_INDEX);
                notifyPageFailed(index, GetText.getString(R.string.error_out_of_range));
                continue;
            }

            try {
                ZipEntry zipEntry = zipFile.getEntry(filenames.get(index));
                if (zipEntry != null) {
                    InputStream is = zipFile.getInputStream(zipEntry);
                    Image image = Image.decode(is, true);
                    mDecodingIndex.lazySet(GalleryPageView.INVALID_INDEX);
                    if (image != null) {
                        notifyPageSucceed(index, image);
                    } else {
                        notifyPageFailed(index, GetText.getString(R.string.error_decoding_failed));
                    }
                } else {
                    mDecodingIndex.lazySet(GalleryPageView.INVALID_INDEX);
                    notifyPageFailed(index, GetText.getString(R.string.error_reading_failed));
                }
            } catch (IOException e) {
                mDecodingIndex.lazySet(GalleryPageView.INVALID_INDEX);
                notifyPageFailed(index, GetText.getString(R.string.error_reading_failed));
            }
        }

        // Clear
        try {
            zipFile.close();
        } catch (IOException e) {
            // Ignore
        }

        Log.i(TAG, "ImageDecoder end");
    }
}
