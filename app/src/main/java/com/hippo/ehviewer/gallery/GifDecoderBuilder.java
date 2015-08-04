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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.yorozuya.IOUtils;

import java.io.InputStream;

public class GifDecoderBuilder {

    private InputStream mInputStream;

    public GifDecoderBuilder(@NonNull InputStream is) {
        mInputStream = is;
    }

    public void close() {
        IOUtils.closeQuietly(mInputStream);
        mInputStream = null;
    }

    public @Nullable GifDecoder build() {
        if (mInputStream != null) {
            GifDecoder gifDecoder = GifDecoder.decodeStream(mInputStream);
            IOUtils.closeQuietly(mInputStream);
            mInputStream = null;
            return gifDecoder;
        } else {
            return null;
        }
    }
}
