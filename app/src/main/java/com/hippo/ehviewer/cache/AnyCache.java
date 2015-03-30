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

import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import com.hippo.ehviewer.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class AnyCache<V> {

    private static final String TAG = AnyCache.class.getSimpleName();

    private LruCache<String, V> mMemoryCache;
    private @Nullable DiskCache<V> mDiskCache;

    private final boolean mHasMemoryCache;
    private final boolean mHasDiskCache;

    /**
     * Used to temporarily pause the disk cache while scrolling
     */
    public boolean mPauseDiskAccess = false;
    private final Object mPauseLock = new Object();

    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;


    public AnyCache(AnyCacheParams params) {
        params.isValid();
        mHasMemoryCache = params.hasMemoryCache;
        mHasDiskCache = params.hasDiskCache;

        if (mHasMemoryCache) {
            initMemoryCache(params.memoryCacheMaxSize);
        }

        if (mHasDiskCache) {
            initDiskCache(params.diskCacheDir, params.diskCacheMaxSize);
        }
    }


    private void initMemoryCache(int maxSize) {
        mMemoryCache = new MemoryCahce<>(maxSize, this);
    }

    private void initDiskCache(File cacheDir, int maxSize) {
        // Set up disk cache
        synchronized (mDiskCacheLock) {
            try {
                mDiskCache = new DiskCache<>(cacheDir, maxSize, this);
            } catch (IOException e) {
                Log.e(TAG, "Can't create disk cache", e);
            }

            // Set up disk cache finished
            mDiskCacheStarting = false;
            mDiskCacheLock.notifyAll();
        }
    }


    protected abstract int sizeOf(String key, V value);

    protected abstract V read(InputStream is);

    protected abstract boolean write(OutputStream os, V value);

    /**
     * Check if have memory cache
     *
     * @return true if have memory cache
     */
    public boolean hasMemoryCache() {
        return mHasMemoryCache;
    }

    /**
     * Check if have disk cache
     *
     * @return true if have disk cache
     */
    public boolean hasDiskCache() {
        return mHasDiskCache;
    }

    /**
     * Get value from memory cache
     *
     * @param key the key to get value
     * @return the value you get, null for miss or no memory cache
     */
    public V getFromMemory(String key) {
        if (mHasMemoryCache) {
            return mMemoryCache.get(key);
        } else {
            return null;
        }
    }

    /**
     * Get value from memory cache
     *
     * @param key the key to get value
     * @return the value you get, null for miss or no memory cache or get error
     */
    public V getFromDisk(String key) {
        if (mHasDiskCache) {
            // Wait for pause
            waitUntilUnpaused();

            String diskKey = hashKeyForDisk(key);

            synchronized (mDiskCacheLock) {
                while (mDiskCacheStarting) {
                    try {
                        mDiskCacheLock.wait();
                    } catch (InterruptedException e) {
                        // Just ignore
                    }
                }
                if (mDiskCache != null) {
                    return mDiskCache.get(diskKey);
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    /**
     * Get value from memory cache and disk cache. If miss in memory cache and
     * get in disk cache, it will put value from disk cache to memory cache.
     *
     * @param key the key to get value
     * @return the value you get
     */
    public V get(String key) {
        V value = getFromMemory(key);

        if (value != null) {
            // Get it in memory cache
            return value;
        }

        value = getFromDisk(key);

        if (value != null) {
            // Get it in disk cache
            putToMemory(key, value);
            return value;
        }

        return null;
    }

    /**
     * Put value to memory cache
     *
     * @param key the key
     * @param value the value
     * @return false if no memory cache
     */
    public boolean putToMemory(String key, V value) {
        if (mHasMemoryCache) {
            mMemoryCache.put(key, value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Put value to disk cache
     *
     * @param key the key
     * @param value the value
     * @return false if no disk cache or get error
     */
    public boolean putToDisk(String key, V value) {
        if (mHasDiskCache) {
            // Wait for pause
            waitUntilUnpaused();

            String diskKey = hashKeyForDisk(key);

            synchronized (mDiskCacheLock) {
                while (mDiskCacheStarting) {
                    try {
                        mDiskCacheLock.wait();
                    } catch (InterruptedException e) {
                        // Just ignore
                    }
                }
                if (mDiskCache != null) {
                    mDiskCache.put(diskKey, value);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Put value to memory cache and disk cache
     *
     * @param key the key
     * @param value the value
     */
    public void put(String key, V value) {
        putToMemory(key, value);
        putToDisk(key, value);
    }

    /**
     * Used to temporarily pause the disk cache while the user is scrolling to
     * improve scrolling.
     *
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(final boolean pause) {
        synchronized (mPauseLock) {
            if (mPauseDiskAccess != pause) {
                mPauseDiskAccess = pause;
                if (!pause) {
                    mPauseLock.notify();
                }
            }
        }
    }

    private void waitUntilUnpaused() {
        synchronized (mPauseLock) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                while (mPauseDiskAccess) {
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        // ignored, we'll start waiting again
                    }
                }
            }
        }
    }

    /**
     * Evicts all of the items from the memory cache
     */
    public void clearMemory() {
        if (mHasMemoryCache) {
            mMemoryCache.evictAll();
        }
    }

    /**
     * Clear disk cache
     */
    public void clearDisk() {
        synchronized (mDiskCacheLock) {
            mDiskCacheStarting = true;
            if (mDiskCache != null && !mDiskCache.isClosed()) {
                try {
                    mDiskCache.delete();
                } catch (IOException e) {
                    Log.e(TAG, "AnyCache clearCache", e);
                }
                File cacheDir = mDiskCache.getCacheDir();
                int maxSize = mDiskCache.getMaxSize();
                mDiskCache = null;
                initDiskCache(cacheDir, maxSize);
            }
        }
    }

    public void flush() {
        synchronized (mDiskCacheLock) {
            if (mDiskCache != null) {
                try {
                    mDiskCache.flush();
                } catch (IOException e) {
                    Log.e(TAG, "AnyCache flush", e);
                }
            }
        }
    }

    /**
     * Evicts all of the items from the memory cache and lets the system know
     * now would be a good time to garbage collect
     */
    public void clear() {
        clearMemory();
        clearDisk();
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable
     * for using as a disk filename.
     *
     * @param key The key used to store the file
     */
    private static String hashKeyForDisk(final String key) {
        String cacheKey;
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (final NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    /**
     * http://stackoverflow.com/questions/332079
     *
     * @param bytes The bytes to convert.
     * @return A {@link String} converted from the bytes of a hashable key used
     *         to store a filename on the disk, to hex digits.
     */
    private static String bytesToHexString(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : bytes) {
            final String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    /**
     * A holder class that contains cache parameters.
     */
    public static class AnyCacheParams {

        public boolean hasMemoryCache = false;
        public int memoryCacheMaxSize = 0;
        public boolean hasDiskCache = false;
        public File diskCacheDir = null;
        public int diskCacheMaxSize = 0;

        /**
         * Check AnyCacheParams is valid
         *
         * @throws IllegalStateException
         */
        public void isValid() throws IllegalStateException {
            if (!hasMemoryCache && !hasDiskCache) {
                throw new IllegalStateException("No memory cache and no disk cache. What can I do for you?");
            }

            if (hasMemoryCache && memoryCacheMaxSize <= 0) {
                throw new IllegalStateException("Memory cache max size must be bigger than 0.");
            }

            if (hasDiskCache) {
                if (diskCacheDir == null) {
                    throw new IllegalStateException("Disk cache dir can't be null.");
                }
                if (diskCacheMaxSize <= 0) {
                    throw new IllegalStateException("Disk cache max size must be bigger than 0.");
                }
            }
        }
    }


    public class MemoryCahce<E> extends LruCache<String, E> {

        public AnyCache<E> mParent;

        public MemoryCahce(int maxSize, AnyCache<E> parent) {
            super(maxSize);
            mParent = parent;
        }

        @Override
        protected int sizeOf(String key, E value) {
            return mParent.sizeOf(key, value);
        }
    }


    public static class DiskCache<E> {

        private static final int IO_BUFFER_SIZE = 8 * 1024;

        private DiskLruCache mDiskLruCache;
        private AnyCache<E> mParent;

        private File mCacheDir;
        private int mMaxSize;

        public DiskCache(File cacheDir, int size, AnyCache<E> parent) throws IOException {
            mDiskLruCache = DiskLruCache.open(cacheDir, 1, 1, size);
            mParent = parent;

            mCacheDir = cacheDir;
            mMaxSize = size;
        }

        public File getCacheDir() {
            return mCacheDir;
        }

        public int getMaxSize() {
            return mMaxSize;
        }

        public boolean isClosed() {
            return mDiskLruCache.isClosed();
        }

        public void delete() throws IOException {
            mDiskLruCache.delete();
        }

        public void flush() throws IOException {
            mDiskLruCache.flush();
        }

        public E get(String key) {
            DiskLruCache.Snapshot snapshot = null;
            try {
                snapshot = mDiskLruCache.get(key);
                if ( snapshot == null ) {
                    // Miss
                    return null;
                }

                final InputStream in = snapshot.getInputStream(0);
                if (in != null) {
                    final BufferedInputStream buffIn =
                            new BufferedInputStream(in, IO_BUFFER_SIZE);
                    return mParent.read(buffIn);
                } else {
                    // Can't get InputStream
                    return null;
                }
            } catch (IOException e) {
                // e.printStackTrace();
                return null;
            } finally {
                Util.closeQuietly(snapshot);
            }
        }

        public boolean put(String key, E value) {
            DiskLruCache.Editor editor = null;
            OutputStream os = null;
            try {
                editor = mDiskLruCache.edit(key);
                if (editor == null) {
                    // The editor is in progress
                    return false;
                }

                os = editor.newOutputStream(0);
                if (os != null) {
                    final BufferedOutputStream buffOut =
                            new BufferedOutputStream(os, IO_BUFFER_SIZE);
                    boolean result = mParent.write(buffOut, value);
                    editor.commit();
                    return result;
                } else {
                    // Can't get OutputStream
                    editor.abort();
                    return false;
                }
            } catch (IOException e) {
                Util.closeQuietly(os);
                try {
                    if (editor != null) {
                        editor.abort();
                    }
                } catch (IOException ignored) {
                }
                return false;
            }
        }
    }
}
