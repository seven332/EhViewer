/*
 * Copyright 2015 Hippo Seven
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import com.hippo.beerbelly.SimpleDiskCache;
import com.hippo.conaco.BitmapPool;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.httpclient.HttpClient;
import com.hippo.io.UniFileInputStreamPipe;
import com.hippo.io.UniFileOutputStreamPipe;
import com.hippo.network.DownloadClient;
import com.hippo.network.DownloadRequest;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.io.InputStreamPipe;
import com.hippo.yorozuya.io.OutputStreamPipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ImageHandler {

    private static final String TAG = ImageHandler.class.getSimpleName();

    public static final String[] POSSIBLE_IMAGE_EXTENSIONS = new String[] {
            "jpg", "jpeg", "png", "gif"
    };

    private static SimpleDiskCache sDiskCache;

    private GalleryBase mGalleryBase;
    private UniFile mDownloadDirParent;
    private String mDownloadDirname;
    private @Nullable UniFile mDownloadDir;
    private volatile Mode mMode;
    private AtomicReferenceArray<String> mFilenames;
    private BitmapPool mBitmapPool = new BitmapPool();

    public enum Mode {
        READ,
        DOWNLOAD
    }

    public static void init(SimpleDiskCache diskCache) {
        sDiskCache = diskCache;
    }

    ImageHandler(GalleryBase galleryBase, int pages, Mode mode, UniFile downloadDirParent, String downloadDirname) {
        mGalleryBase = galleryBase;
        mMode = mode;
        mFilenames = new AtomicReferenceArray<>(pages);
        mDownloadDirParent = downloadDirParent;
        mDownloadDirname = downloadDirname;

        if (mode == Mode.DOWNLOAD) {
            mDownloadDir = mDownloadDirParent.createDirectory(mDownloadDirname);
        }
    }

    public void setMode(Mode mode) {
        if (mMode != mode) {
            mMode = mode;
            mDownloadDir = mDownloadDirParent.createDirectory(mDownloadDirname);
        }
        // Let DownloadManager to set spiderObj's download to true
    }

    private static String getImageFilename(int index, String extension) {
        return String.format("%08d.%s", index + 1, extension);
    }

    private String guessImageFilename(int index) {
        if (mDownloadDir == null) {
            return null;
        }

        for (String extension : POSSIBLE_IMAGE_EXTENSIONS) {
            String filename = getImageFilename(index, extension);
            if (mDownloadDir.findFile(filename) != null) {
                return filename;
            }
        }
        return null;
    }

    private @Nullable String getImageFilename(int index) {
        String filename = mFilenames.get(index);
        if (filename == null) {
            filename = guessImageFilename(index);
            if (filename != null) {
                // Put filename into array
                mFilenames.lazySet(index, filename);
            }
        }
        return filename;
    }

    private InputStreamPipe getReadInputStreamPipe(int index) {
        String key = EhCacheKeyFactory.getImageKey(mGalleryBase.gid, index);
        return sDiskCache.getInputStreamPipe(key);
    }

    private InputStreamPipe getDownloadInputStreamPipe(String filename) {
        if (mDownloadDir == null) {
            return null;
        }

        UniFile uniFile = mDownloadDir.findFile(filename);
        if (uniFile != null) {
            return new UniFileInputStreamPipe(uniFile);
        } else {
            return null;
        }
    }

    private OutputStreamPipe getReadOutputStreamPipe(int index) {
        String key = EhCacheKeyFactory.getImageKey(mGalleryBase.gid, index);
        return sDiskCache.getOutputStreamPipe(key);
    }

    private OutputStreamPipe getDownloadOutputStreamPipe(String filename) {
        if (mDownloadDir == null) {
            return null;
        }

        UniFile uniFile = mDownloadDir.createFile(filename);
        if (uniFile != null) {
            return new UniFileOutputStreamPipe(uniFile);
        } else {
            return null;
        }
    }

    public void releaseBitmap(Bitmap bitmap) {
        mBitmapPool.addReusableBitmap(bitmap);
    }

    private boolean containFromRead(int index) {
        String key = EhCacheKeyFactory.getImageKey(mGalleryBase.gid, index);
        return sDiskCache.contain(key);
    }

    private boolean containFromDownload(int index) {
        if (index >= mFilenames.length()) {
            return false;
        }

        String filename = mFilenames.get(index);
        if (filename != null) {
            // Check it in dir
            if (mDownloadDir != null && mDownloadDir.findFile(filename) != null) {
                return true;
            }
        } else {
            filename = guessImageFilename(index);
            if (filename != null) {
                // Put filename into array
                mFilenames.lazySet(index, filename);
                return true;
            }
        }

        // Try to find image in cache
        InputStreamPipe isPipe = getReadInputStreamPipe(index);
        if (isPipe == null) {
            return false;
        }

        if (mDownloadDir == null) {
            // Can't get download dir
            return false;
        }

        // Copy image from cache to dir
        OutputStream os = null;
        try {
            isPipe.obtain();

            if (filename == null) {
                // Try to get extension
                InputStream is = isPipe.open();
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, options);
                isPipe.close();
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(options.outMimeType);
                if (extension == null) {
                    // Can't get extension;
                    return false;
                }
                filename = getImageFilename(index, extension);
                // Put filename into array
                mFilenames.lazySet(index, filename);
            }

            UniFile file = mDownloadDir.createFile(filename);
            if (file == null) {
                return false;
            }
            os = file.openOutputStream();
            IOUtils.copy(isPipe.open(), os);
            return true;
        } catch (IOException e) {
            // Ignore
            return false;
        } finally {
            IOUtils.closeQuietly(os);
            isPipe.close();
            isPipe.release();
        }
    }

    public boolean contain(int index) {
        if (mMode == Mode.READ) {
            return containFromRead(index);
        } else if (mMode == Mode.DOWNLOAD) {
            return containFromDownload(index);
        } else {
            return false;
        }
    }

    public void save(HttpClient httpClient, String url, int index, SaveHelper helper) throws Exception {
        OutputStreamPipe osPipe = null;

        if (mMode == Mode.READ) {
            osPipe = getReadOutputStreamPipe(index);
        } else if (mMode == Mode.DOWNLOAD && mDownloadDir == null) {
            return;
        }

        if (helper.mCancel) {
            throw new IOException("Cancelled");
        }

        DownloadListener downloadListener = new DownloadListener(index, helper);
        DownloadRequest request = new DownloadRequest();
        helper.mRequest = request;
        request.setHttpClient(httpClient);
        request.setUrl(url);

        if (mMode == Mode.READ) {
            request.setOSPipe(osPipe);
        } else if (mMode == Mode.DOWNLOAD) {
            // Let download listener to fix filename
            request.setFilename(getImageFilename(index, "jpg"));
            request.setDir(mDownloadDir);
        }

        request.setListener(downloadListener);
        DownloadClient.execute(request);

        if (downloadListener.mException != null) {
            // Remove failed stuff
            if (mMode == Mode.READ) {
                sDiskCache.remove(EhCacheKeyFactory.getImageKey(mGalleryBase.gid, index));
            } else if (mMode == Mode.DOWNLOAD) {
                String filename = getImageFilename(index);
                if (filename != null) {
                    UniFile file = mDownloadDir.findFile(filename);
                    if (file != null) {
                        file.delete();
                    }
                }
            }

            throw downloadListener.mException;
        }
    }

    class DownloadListener extends DownloadClient.SimpleDownloadListener {

        private int mIndex;
        private SaveHelper mHelper;

        private Exception mException;

        public DownloadListener(int index, SaveHelper listener) {
            mIndex = index;
            mHelper = listener;
        }

        @Override
        public String onFixname(String newFileFirstname, String newExtension, String oldFilename) {
            String filename;
            if (TextUtils.isEmpty(newExtension)) {
                filename = oldFilename;
            } else {
                filename = FileUtils.getNameFromFilename(oldFilename) + '.' + newExtension;
            }
            mFilenames.lazySet(mIndex, filename);
            return filename;
        }

        @Override
        public void onConnect(long totalSize) {
            mHelper.onStartSaving(totalSize);
        }

        @Override
        public void onDonwlad(long receivedSize) {
            mHelper.onSave(receivedSize);
        }

        @Override
        public void onFailed(Exception e) {
            mException = e;
        }
    }

    /**
     * @return
     */
    public Object getImage(int index) {
        InputStreamPipe isPipe = null;
        if (mMode == Mode.READ) {
            isPipe = getReadInputStreamPipe(index);
        } else if (mMode == Mode.DOWNLOAD) {
            String filename = getImageFilename(index);
            if (filename != null) {
                isPipe = getDownloadInputStreamPipe(filename);
            }
        }

        if (isPipe == null) {
            return null;
        }

        Bitmap bitmap = null;
        try {
            isPipe.obtain();

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            InputStream is = isPipe.open();
            BitmapFactory.decodeStream(is, null, options);
            isPipe.close();

            // Check out size
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                isPipe.release();
                return null;
            }

            if ("image/gif".equals(options.outMimeType)) {
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                is = isPipe.open();
                bitmap = BitmapFactory.decodeStream(is, null, options);
                isPipe.close();

                if (bitmap != null) {
                    if (mMode == Mode.READ) {
                        // Copy the file to
                        File tempFile = AppConfig.createTempFile();
                        if (tempFile == null) {
                            throw new IOException("Can't get temp dir or can't create temp dir");
                        }
                        is = isPipe.open();
                        IOUtils.copy(is, new FileOutputStream(tempFile));
                        isPipe.close();
                        is = new TempFileInputStream(tempFile);
                    } else if (mMode == Mode.DOWNLOAD) {
                        if (mDownloadDir == null) {
                            throw new IOException("Can't find download dir");
                        }
                        UniFile uniFile = mDownloadDir.findFile(getImageFilename(index));
                        if (uniFile == null) {
                            throw new IOException("Can't find image file");
                        }
                        is = uniFile.openInputStream();
                    } else {
                        throw new IllegalStateException("Unknown state");
                    }

                    GifDecoderBuilder gifDecoderBuilder = new GifDecoderBuilder(is);

                    return new Pair<>(gifDecoderBuilder, bitmap);
                }
                return null;
            } else {
                // It is not gif
                options.inJustDecodeBounds = false;
                options.inMutable = true;
                options.inSampleSize = 1;
                options.inBitmap = mBitmapPool.getInBitmap(options);
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                is = isPipe.open();
                bitmap = BitmapFactory.decodeStream(is, null, options);
                isPipe.close();

                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
            releaseBitmap(bitmap);
            return null;
        } finally {
            isPipe.close();
            isPipe.release();
        }
    }

    public static abstract class SaveHelper {

        private DownloadRequest mRequest;

        private boolean mCancel = false;

        public abstract void onStartSaving(long totalSize);

        public abstract void onSave(long receivedSize);

        public boolean isCancelled() {
            return mCancel;
        }

        public void cancel() {
            mCancel = true;
            if (mRequest != null) {
                mRequest.cancel();
            }
        }
    }

    public static class TempFileInputStream extends FileInputStream {

        private File mFile;

        public TempFileInputStream(File file) throws FileNotFoundException {
            super(file);
            mFile = file;
        }

        @Override
        public void close() throws IOException {
            super.close();
            mFile.delete();
        }
    }
}
