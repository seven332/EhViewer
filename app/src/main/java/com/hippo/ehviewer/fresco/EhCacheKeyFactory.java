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

package com.hippo.ehviewer.fresco;

import android.net.Uri;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.request.ImageRequest;

public class EhCacheKeyFactory implements CacheKeyFactory {

    private static EhCacheKeyFactory sInstance = null;

    private EhCacheKeyFactory() {
    }

    public static synchronized EhCacheKeyFactory getInstance() {
        if(sInstance == null) {
            sInstance = new EhCacheKeyFactory();
        }
        return sInstance;
    }

    private String getCommonKey(ImageRequest imageRequest) {
        if (imageRequest instanceof KeyImageRequest) {
            return ((KeyImageRequest) imageRequest).getKey();
        } else {
            return getCacheKeySourceUri(imageRequest.getSourceUri()).toString();
        }
    }

    @Override
    public CacheKey getBitmapCacheKey(ImageRequest imageRequest) {
        return new BitmapMemoryCacheKey(
                getCommonKey(imageRequest),
                imageRequest.getResizeOptions(),
                imageRequest.getAutoRotateEnabled(),
                imageRequest.getImageDecodeOptions());
    }

    @Override
    public CacheKey getEncodedCacheKey(ImageRequest imageRequest) {
        return new SimpleCacheKey(getCommonKey(imageRequest));
    }

    @Override
    public Uri getCacheKeySourceUri(Uri uri) {
        return uri;
    }

    public static String getThumbKey(int gid) {
        return "thumb:" + gid;
    }

    public static String getGalleryKey(int gid, int index) {
        return "gallery:" + gid + "-" + index;
    }
}
