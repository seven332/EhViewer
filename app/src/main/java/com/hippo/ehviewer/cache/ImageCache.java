/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageCache extends AnyCache<Bitmap>{

    private static final String TAG = "ImageCache";

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float MEM_CACHE_DIVIDER = 0.25f;

    /**
     * Compression settings when writing images to disk cache
     */
    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;

    /**
     * Image compression quality
     */
    private static final int COMPRESS_QUALITY = 98;

    private static ImageCache sImageCache;

    public static @NonNull ImageCache getImageCache(@NonNull Context context) {
        if (sImageCache == null) {
            sImageCache = new ImageCache(context.getApplicationContext());
        }
        return sImageCache;
    }


    private ImageCache(Context context) {
        final ActivityManager activityManager = (ActivityManager)context
                .getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryCacheMax = Math.round(MEM_CACHE_DIVIDER * activityManager.getMemoryClass()
                * 1024 * 1024);
        setMemoryCache(memoryCacheMax);

        File diskCacheDir = getDiskCacheDir(context, TAG);
        if (diskCacheDir != null) {
            if (!diskCacheDir.exists()) {
                diskCacheDir.mkdirs();
            }
            setDiskCache(diskCacheDir, 20 * 1024 * 1024);
        }
    }


    @Override
    protected int sizeOf(String key, Bitmap value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return value.getAllocationByteCount();
        } else {
            return value.getByteCount();
        }
    }

    @Override
    protected Bitmap read(InputStream is) {
        return BitmapFactory.decodeStream(is);
    }

    @Override
    protected boolean write(OutputStream os, Bitmap value) {
        return value.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, os);
    }

    /**
     * Get the external app cache directory
     *
     * @param context The {@link Context} to use
     * @return The external cache directory
     */
    private static File getExternalCacheDir(final Context context) {
        return context.getExternalCacheDir();
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise)
     *
     * @param context The {@link Context} to use
     * @param uniqueName A unique directory name to append to the cache
     *            directory
     * @return The cache directory
     */
    private static File getDiskCacheDir(final Context context, final String uniqueName) {
        // getExternalCacheDir(context) returns null if external storage is not ready
        final String cachePath = getExternalCacheDir(context) != null
                ? getExternalCacheDir(context).getPath()
                : context.getCacheDir().getPath();
        return new File(cachePath, uniqueName);
    }
}
