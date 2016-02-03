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

package com.hippo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.text.TextUtils;

import com.hippo.ehviewer.R;

public final class OpenUrlHelper {

    private OpenUrlHelper() {
    }

    public static void openUrl(Context context, String url, boolean checkNMB) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        Intent intent;
        Uri uri = Uri.parse(url);

        /*
        // Is nmb url
        if (checkNMB) {
            NMBUriParser.PostResult result = NMBUriParser.parsePostUri(uri);
            if (result.site != null && result.id != null) {
                intent = new Intent(context, PostActivity.class);
                intent.setAction(PostActivity.ACTION_SITE_ID);
                intent.putExtra(PostActivity.KEY_SITE, result.site.getId());
                intent.putExtra(PostActivity.KEY_ID, result.id);
                if (!(context instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
                return;
            }
        }
        */

        // CustomTabs
        if (context instanceof Activity) {
            String packageName = CustomTabsHelper.getPackageNameToUseFixed(context);
            if (packageName != null) {
                new CustomTabsIntent.Builder()
                        .setToolbarColor(context.getResources().getColor(R.color.colorPrimary))
                        .setShowTitle(true)
                        .build()
                        .launchUrl((Activity) context, uri);
                return;
            }
        }

        // Intent.ACTION_VIEW
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        PackageManager pm = context.getPackageManager();
        ResolveInfo ri = pm.resolveActivity(intent, 0);
        if (ri != null) {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return;
        }

        /*
        // Use WebViewActivity
        intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(WebViewActivity.KEY_URL, url);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        */
    }
}
