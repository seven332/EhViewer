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

package com.hippo.ehviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.conaco.ValueHelper;
import com.hippo.conaco.ValueHolder;
import com.hippo.yorozuya.io.InputStreamPipe;

import java.io.IOException;

public class BitmapHelper implements ValueHelper<Bitmap> {

    @Nullable
    @Override
    public Bitmap decode(@NonNull InputStreamPipe isPipe) {
        try {
            isPipe.obtain();
            return BitmapFactory.decodeStream(isPipe.open());
        } catch (OutOfMemoryError e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            isPipe.close();
            isPipe.release();
        }
    }

    @Override
    public int sizeOf(@NonNull String key, @NonNull Bitmap value) {
        return value.getByteCount();
    }

    @Override
    public void onRemove(@NonNull String key, @NonNull ValueHolder<Bitmap> oldValue) {}

    @Override
    public boolean useMemoryCache(@NonNull String key, ValueHolder<Bitmap> holder) {
        return true;
    }
}
