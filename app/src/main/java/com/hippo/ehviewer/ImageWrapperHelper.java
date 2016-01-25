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

package com.hippo.ehviewer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.conaco.ValueHelper;
import com.hippo.conaco.ValueHolder;
import com.hippo.drawable.ImageWrapper;
import com.hippo.image.Image;
import com.hippo.yorozuya.io.InputStreamPipe;

public class ImageWrapperHelper implements ValueHelper<ImageWrapper> {

    private static final String TAG = ImageWrapperHelper.class.getSimpleName();

    @Nullable
    @Override
    public ImageWrapper decode(@NonNull InputStreamPipe isPipe) {
        try {
            isPipe.obtain();
            Image image = Image.decode(isPipe.open(), false);
            if (image != null) {
                return new ImageWrapper(image);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            isPipe.close();
            isPipe.release();
        }
    }

    @Override
    public int sizeOf(@NonNull String key, @NonNull ImageWrapper value) {
        return value.getWidth() * value.getHeight() * 4;
    }

    @Override
    public void onRemove(@NonNull String key, @NonNull ValueHolder<ImageWrapper> oldValue) {
        if (oldValue.isFree()) {
            oldValue.getValue().recycle();
        }
    }

    @Override
    public boolean useMemoryCache(@NonNull String key, ValueHolder<ImageWrapper> holder) {
        return holder == null || !holder.getValue().isLarge();
    }
}
