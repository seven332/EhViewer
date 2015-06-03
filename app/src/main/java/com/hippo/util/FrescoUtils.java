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

package com.hippo.util;

import android.annotation.IdRes;
import android.net.Uri;

import com.facebook.common.util.UriUtil;

public class FrescoUtils {

    public static Uri getResourcesDrawableUri(@IdRes int resId) {
        return new Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                .path(String.valueOf(resId)).build();
    }
}
