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
import android.webkit.MimeTypeMap;

import com.hippo.beerbelly.SimpleDiskCache;
import com.hippo.conaco.BitmapPool;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.dao.SpiderObj;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.ehviewer.util.Settings;
import com.hippo.httpclient.HttpResponse;
import com.hippo.io.UniFileInputStreamPipe;
import com.hippo.io.UniFileOutputStreamPipe;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.io.InputStreamPipe;
import com.hippo.yorozuya.io.OutputStreamPipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageHandler {

    public static String[] POSSIBLE_IMAGE_EXTENSIONS = new String[] {
            "jpg", "jpeg", "png", "gif"
    };

    private static SimpleDiskCache sDiskCache;

    private GalleryBase mGalleryBase;
    private UniFile mDownloadDir;
    private Mode mMode;
    private String[] mFilenames;
    private BitmapPool mBitmapPool = new BitmapPool();

    public enum Mode {
        READ,
        DOWNLOAD
    }

    public static void init(SimpleDiskCache diskCache) {
        sDiskCache = diskCache;
    }

    public ImageHandler(GalleryBase galleryBase, int pages, Mode mode) {
        mGalleryBase = galleryBase;
        mMode = mode;
        mFilenames = new String[pages];

        // TODO Let GallerySpider to handle db, just pass dirname to ImageHandler
        String dirname;
        SpiderObj spiderObj = DBUtils.getSpiderObj(galleryBase.gid);
        if (spiderObj == null) {
            spiderObj = newSpiderObj();
            DBUtils.insertSpiderObj(spiderObj);
        }
        dirname = spiderObj.getDirname();
        mDownloadDir = Settings.getDownloadPath().createDirectory(dirname);
    }

    public String downloadDirname() {
        return FileUtils.ensureFilename(mGalleryBase.gid + '-' + mGalleryBase.title);
    }

    private SpiderObj newSpiderObj() {
        SpiderObj spiderObj = new SpiderObj();
        spiderObj.setGid((long) mGalleryBase.gid);
        spiderObj.setLastIndex(0);
        spiderObj.setDirname(downloadDirname());
        spiderObj.setDownload(mMode == Mode.DOWNLOAD);
        spiderObj.setTime(System.currentTimeMillis());
        return spiderObj;
    }

    public void setMode(Mode mode) {
        mMode = mode;
        // Let DownloadManager to set spiderObj's download to true
    }

    private static String getImageFilename(int index, String extension) {
        return String.format("%08d.%s", index + 1, extension);
    }

    private String guessImageFilename(int index) {
        for (String extension : POSSIBLE_IMAGE_EXTENSIONS) {
            String filename = getImageFilename(index, extension);
            if (mDownloadDir.findFile(filename) != null) {
                return filename;
            }
        }
        return null;
    }

    private InputStreamPipe getReadInputStreamPipe(int index) {
        String key = EhCacheKeyFactory.getImageKey(mGalleryBase.gid, index);
        return sDiskCache.getInputStreamPipe(key);
    }

    private InputStreamPipe getDownloadInputStreamPipe(String filename) {
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

    public boolean contain(int index) {
        if (mMode == Mode.READ) {
            String key = EhCacheKeyFactory.getImageKey(mGalleryBase.gid, index);
            return sDiskCache.contain(key);
        } else if (mMode == Mode.DOWNLOAD) {
            if (index >= mFilenames.length) {
                return false;
            }

            String filename;

            filename = mFilenames[index];
            if (filename != null) {
                // Check it in dir
                if (mDownloadDir.findFile(filename) != null) {
                    return true;
                }
            } else {
                filename = guessImageFilename(index);
                if (filename != null) {
                    // Put filename into array
                    mFilenames[index] = filename;
                    return true;
                }
            }

            // Try to find image in cache
            InputStreamPipe isPipe = getReadInputStreamPipe(index);
            if (isPipe == null) {
                return false;
            }

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
                    mFilenames[index] = filename;
                }

                UniFile file = mDownloadDir.createFile(filename);
                if (file == null) {
                    return false;
                }
                os = file.openOutputStream();
                IOUtils.copy(isPipe.open(), os);

            } catch (IOException e) {
                // Ignore
            } finally {
                IOUtils.closeQuietly(os);
                isPipe.close();
                isPipe.release();
            }
        }
        return false;
    }

    public void save(int index, HttpResponse response) {
        OutputStreamPipe osPipe = null;

        if (mMode == Mode.READ) {
            osPipe = getReadOutputStreamPipe(index);
        } else if (mMode == Mode.DOWNLOAD) {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                    response.getHeaderField("Content-Type"));
            if (extension == null) {
                extension = MimeTypeMap.getFileExtensionFromUrl(response.getUrl().toString());
                if (extension == null) {
                    extension = "jpg";
                }
            }
            String filename = getImageFilename(index, extension);
            // Put filename into array
            mFilenames[index] = filename;
            osPipe = getDownloadOutputStreamPipe(filename);
        }

        if (osPipe == null) {
            return;
        }

        try {
            osPipe.obtain();
            IOUtils.copy(response.getInputStream(), osPipe.open());
        } catch (IOException e) {
            // Ignore
        } finally {
            osPipe.close();
            osPipe.release();
        }
    }

    public Bitmap getBitmap(int index) {
        InputStreamPipe isPipe = null;
        if (mMode == Mode.READ) {
            isPipe = getReadInputStreamPipe(index);
        } else if (mMode == Mode.DOWNLOAD) {
            String filename = mFilenames[index];
            if (filename == null) {
                filename = guessImageFilename(index);
                if (filename != null) {
                    // Put filename into array
                    mFilenames[index] = filename;
                }
            }
            if (filename != null) {
                isPipe = getDownloadInputStreamPipe(filename);
            }
        }

        if (isPipe == null) {
            return null;
        }

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

            options.inJustDecodeBounds = false;
            options.inMutable = true;
            options.inSampleSize = 1;
            options.inBitmap = mBitmapPool.getInBitmap(options);

            is = isPipe.open();
            return BitmapFactory.decodeStream(is, null, options);
        } catch (Exception e) {
            return null;
        } finally {
            isPipe.close();
            isPipe.release();
        }
    }
}
