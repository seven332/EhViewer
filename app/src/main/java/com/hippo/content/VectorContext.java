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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.content;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.VectorResources;
import android.os.Build;

public class VectorContext extends ContextWrapper {

    private VectorResources mVectorResources;

    public VectorContext(Context base) {
        super(base);
    }

    @Override
    public Resources getResources() {
        final Resources superResources = super.getResources();
        if (mVectorResources == null || mVectorResources.isBase(superResources)) {
            mVectorResources = new VectorResources(this, superResources);
        }
        return mVectorResources;
    }

    /**
     * In {@link android.content.ContextWrapper#attachBaseContext(Context)},
     * do <code>super.attachBaseContext(Vector.wrapContext(newBase));</code>.
     *
     * @param context the context
     * @return new context
     */
    public static Context wrapContext(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? new VectorContext(context) : context;
    }
}
