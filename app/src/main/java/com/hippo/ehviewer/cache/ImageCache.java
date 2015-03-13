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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageCache {

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

    private static AnyCache<Bitmap> sImageCache;

    public static AnyCache<Bitmap> getImageCache(Context context) {
        if (sImageCache == null) {
            AnyCache.Builder builder = new AnyCache.Builder();

            final ActivityManager activityManager = (ActivityManager)context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            final int memoryCacheMax = Math.round(MEM_CACHE_DIVIDER * activityManager.getMemoryClass()
                    * 1024 * 1024);
            builder.setMemoryCache(memoryCacheMax, new BitmapMemoryCacheHelper());

            File diskCacheDir = getDiskCacheDir(context, TAG);
            if (diskCacheDir != null) {
                if (!diskCacheDir.exists()) {
                    diskCacheDir.mkdirs();
                }
                builder.setDiskCache(diskCacheDir, 20 * 1024 * 1024, new BitmapDiskCacheHelper());
            }

            sImageCache = builder.build();
        }
        return sImageCache;
    }

    /**
     * Get the external app cache directory
     *
     * @param context The {@link Context} to use
     * @return The external cache directory
     */
    public static File getExternalCacheDir(final Context context) {
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
    public static File getDiskCacheDir(final Context context, final String uniqueName) {
        // getExternalCacheDir(context) returns null if external storage is not ready
        final String cachePath = getExternalCacheDir(context) != null
                ? getExternalCacheDir(context).getPath()
                : context.getCacheDir().getPath();
        return new File(cachePath, uniqueName);
    }

    private static class BitmapMemoryCacheHelper extends AnyCache.MemoryCacheHelper<Bitmap> {
        @Override
        public int sizeOf(String key, android.graphics.Bitmap bitmap) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return bitmap.getAllocationByteCount();
            } else {
                return bitmap.getByteCount();
            }
        }
    }

    private static class BitmapDiskCacheHelper extends AnyCache.DiskCacheHelper<Bitmap> {
        @Override
        public Bitmap get(InputStream is) {
            return BitmapFactory.decodeStream(is);
        }

        @Override
        public boolean put(OutputStream os, Bitmap value) {
            value.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, os);
            return true;
        }
    }
}
