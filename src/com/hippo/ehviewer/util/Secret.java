/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.util;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.hippo.ehviewer.network.HttpHelper;

public final class Secret {

    private static final String DIR_NAME = "images";
    private static final String IMAGE_NAME = "sercet.jpg";
    private static final String SERCET_IMAGE_URL = "http://www.ehviewer.com/static/images/pandas.jpg";

    private static volatile boolean sIsGetSecretImage = false;

    public static void updateSecretImage(Context context) {
        if (sIsGetSecretImage)
            return;
        sIsGetSecretImage = true;

        new UpdateSercetImageThread(context).start();
    }

    public static boolean hasSecretImage(Context context) {
        if (sIsGetSecretImage)
            return false;

        File dir = context.getExternalFilesDir(DIR_NAME);
        File image = new File(dir, IMAGE_NAME);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image.getAbsolutePath(), opts);
        return opts.outWidth > 0 && opts.outHeight > 0;
    }

    public static Bitmap getSecretImage(Context context) {
        if (sIsGetSecretImage)
            return null;

        File dir = context.getExternalFilesDir(DIR_NAME);
        File image = new File(dir, IMAGE_NAME);
        return BitmapFactory.decodeFile(image.getAbsolutePath(), null);
    }

    private static class UpdateSercetImageThread extends BgThread {

        private final Context mContext;

        public UpdateSercetImageThread(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            // TODO check usable space
            if (!hasSecretImage(mContext)) {
                // Get image error, try to get image from internet
                HttpHelper hh = new HttpHelper(mContext);
                hh.download(SERCET_IMAGE_URL, mContext.getExternalFilesDir(DIR_NAME),
                        IMAGE_NAME, false, null, null);
            }
            sIsGetSecretImage = false;
        }
    }
}
