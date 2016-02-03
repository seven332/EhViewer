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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.text.TextUtils;
import android.widget.Toast;

import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser;
import com.hippo.ehviewer.client.parser.GalleryListUrlParser;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.scene.GalleryDetailScene;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.scene.StageActivity;
import com.hippo.util.CustomTabsHelper;

public final class OpenUrlHelper {

    private OpenUrlHelper() {
    }

    public static void openUrl(Activity activity, String url, boolean inApp, boolean customTabs) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        Intent intent;
        Uri uri = Uri.parse(url);

        if (inApp) {
            ListUrlBuilder listUrlBuilder = GalleryListUrlParser.parse(url);
            if (listUrlBuilder != null) {
                Bundle args = new Bundle();
                args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_LIST_URL_BUILDER);
                args.putParcelable(GalleryListScene.KEY_LIST_URL_BUILDER, listUrlBuilder);
                intent = new Intent(activity, MainActivity.class);
                intent.putExtra(StageActivity.KEY_SCENE_NAME, GalleryListScene.class.getName());
                intent.putExtra(StageActivity.KEY_SCENE_ARGS, args);
                activity.startActivity(intent);
                return;
            }

            int gid = -1;
            String token = null;
            int page = -1;
            GalleryDetailUrlParser.Result result1 = GalleryDetailUrlParser.parse(url);
            if (result1 != null) {
                gid = result1.gid;
                token = result1.token;
            } else {
                /* TODO
                GalleryUrlParser.Result result2 = GalleryUrlParser.parse(url);
                if (result2 != null) {
                    gid = result2.gid;
                    token = result2.token;
                    page = result2.index;
                }
                */
            }
            if (gid != -1 && token != null) {
                Bundle args = new Bundle();
                args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN);
                args.putInt(GalleryDetailScene.KEY_GID, gid);
                args.putString(GalleryDetailScene.KEY_TOKEN, token);
                args.putInt(GalleryDetailScene.KEY_PAGE, page);
                intent = new Intent(activity, MainActivity.class);
                intent.putExtra(StageActivity.KEY_SCENE_NAME, GalleryDetailScene.class.getName());
                intent.putExtra(StageActivity.KEY_SCENE_ARGS, args);
                activity.startActivity(intent);
                return;
            }
        }

        // CustomTabs
        if (customTabs) {
            String packageName = CustomTabsHelper.getPackageNameToUseFixed(activity);
            if (packageName != null) {
                new CustomTabsIntent.Builder()
                        .setToolbarColor(activity.getResources().getColor(R.color.colorPrimary))
                        .setShowTitle(true)
                        .build()
                        .launchUrl(activity, uri);
                return;
            }
        }

        // Intent.ACTION_VIEW
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        PackageManager pm = activity.getPackageManager();
        ResolveInfo ri = pm.resolveActivity(intent, 0);
        if (ri != null) {
            activity.startActivity(intent);
            return;
        }

        Toast.makeText(activity, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show();
    }
}
