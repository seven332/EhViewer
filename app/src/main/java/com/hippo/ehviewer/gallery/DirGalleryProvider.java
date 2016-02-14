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
import android.util.Log;

import com.hippo.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Stack;

public class DirGalleryProvider extends GalleryProvider {

    private static final String TAG = DirGalleryProvider.class.getSimpleName();

    private File mDir;
    private String[] mFiles;

    private final Stack<Integer> mRequests = new Stack<>();

    private boolean mRun = true;

    public DirGalleryProvider(File dir) {
        mDir = dir;
        mFiles = mDir.list(new ImageFilter());
    }

    @Override
    public void start() {
        mRun = true;
        new Decoder().start();
    }

    @Override
    public void stop() {
        mRun = false;
        synchronized (mRequests) {
            mRequests.notify();
        }
    }

    @Override
    public int size() {
        return mFiles != null ? mFiles.length : 0;
    }

    @Override
    @Result
    public int request(int index) {
        if (index < 0 || index >= size()) {
            return RESULT_ERROR;
        } else {
            synchronized (mRequests) {
                mRequests.add(index);
                mRequests.notify();
            }
            return RESULT_WAIT;
        }
    }

    private static final String[] ENDS = {
            ".jpg", // Joint Photographic Experts Group
            ".jpeg",
            ".png", // Portable Network Graphics
            ".gif", // Graphics Interchange Format
    };

    private static class ImageFilter implements FilenameFilter {

        private static boolean endsWith(String str, String[] ends) {
            if (str == null) {
                return false;
            }

            for (int i = 0, n = ends.length; i < n; i++) {
                if (str.endsWith(ends[i])) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean accept(File dir, String filename) {
            return endsWith(filename.toLowerCase(), ENDS);
        }
    }

    private class Decoder extends Thread {

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (mRun) {
                int index;
                synchronized (mRequests) {
                    if (mRequests.isEmpty()) {
                        try {
                            mRequests.wait();
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                        continue;
                    }
                    index = mRequests.pop();
                }

                try {
                    InputStream is = new FileInputStream(new File(mDir, mFiles[index]));
                    Image image = Image.decode(is, false);
                    if (image != null) {
                        notifyPageSucceed(index, image);
                    } else {
                        notifyPageFailed(index, new Exception("Decode error"));
                    }
                } catch (FileNotFoundException e) {
                    notifyPageFailed(index, e);
                }
            }

            Log.i(TAG, "ImageDecoder end");
        }
    }
}
