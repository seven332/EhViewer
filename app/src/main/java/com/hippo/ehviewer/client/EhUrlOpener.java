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

package com.hippo.ehviewer.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser;
import com.hippo.ehviewer.client.parser.GalleryListUrlParser;
import com.hippo.ehviewer.client.parser.GalleryUrlParser;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.scene.GalleryDetailScene;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.ehviewer.ui.scene.ProgressScene;
import com.hippo.scene.StageActivity;

public class EhUrlOpener {

    private static final String TAG = EhUrlOpener.class.getSimpleName();

    public static boolean openUrl(Activity activity, String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        Intent intent;

        ListUrlBuilder listUrlBuilder = GalleryListUrlParser.parse(url);
        if (listUrlBuilder != null) {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_LIST_URL_BUILDER);
            args.putParcelable(GalleryListScene.KEY_LIST_URL_BUILDER, listUrlBuilder);
            intent = new Intent(activity, MainActivity.class);
            intent.putExtra(StageActivity.KEY_SCENE_NAME, GalleryListScene.class.getName());
            intent.putExtra(StageActivity.KEY_SCENE_ARGS, args);
            activity.startActivity(intent);
            return true;
        }

        GalleryDetailUrlParser.Result result1 = GalleryDetailUrlParser.parse(url);
        if (result1 != null) {
            Bundle args = new Bundle();
            args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN);
            args.putInt(GalleryDetailScene.KEY_GID, result1.gid);
            args.putString(GalleryDetailScene.KEY_TOKEN, result1.token);
            intent = new Intent(activity, MainActivity.class);
            intent.putExtra(StageActivity.KEY_SCENE_NAME, GalleryDetailScene.class.getName());
            intent.putExtra(StageActivity.KEY_SCENE_ARGS, args);
            activity.startActivity(intent);
            return true;
        }

        GalleryUrlParser.Result result2 = GalleryUrlParser.parse(url);
        if (result2 != null) {
            Bundle args = new Bundle();
            args.putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN);
            args.putInt(ProgressScene.KEY_GID, result2.gid);
            args.putString(ProgressScene.KEY_PTOKEN, result2.pToken);
            args.putInt(ProgressScene.KEY_PAGE, result2.page);
            intent = new Intent(activity, MainActivity.class);
            intent.putExtra(StageActivity.KEY_SCENE_NAME, ProgressScene.class.getName());
            intent.putExtra(StageActivity.KEY_SCENE_ARGS, args);
            activity.startActivity(intent);
            return true;
        }

        Log.i(TAG, "Can't parse url: " + url);

        return false;
    }
}
