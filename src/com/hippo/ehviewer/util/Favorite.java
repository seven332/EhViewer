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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.widget.MaterialToast;

public class Favorite {

    private static Context mContext;
    private static Resources mResources;

    private static AlertDialog lastAddToFavoriteDialog;

    public static String[] FAVORITE_TITLES;

    public static void init(Context context) {
        mContext = context;
        mResources = mContext.getResources();

        FAVORITE_TITLES = new String[EhClient.FAVORITE_SLOT_NUM + 1];
        FAVORITE_TITLES[0] = mResources.getString(R.string.local_favorite);
        for (int i = 1; i < EhClient.FAVORITE_SLOT_NUM + 1; i++) {
            FAVORITE_TITLES[i] = mResources.getString(R.string.favourite) + " "+ (i - 1);
        }
    }

    public static void addToFavorite(final Context context, GalleryInfo gi) {
        int defaultFavorite = Config.getDefaultFavorite();
        switch (defaultFavorite) {
        case -2:
            Favorite.getAddToFavoriteDialog(context, gi).show();
            break;
        case -1:
            Data.getInstance().addLocalFavourite(gi);
            MaterialToast.showToast(R.string.toast_add_favourite);
            break;
        default:
            EhClient.getInstance().addToFavorite(gi.gid,
                    gi.token, defaultFavorite, null, new EhClient.OnAddToFavoriteListener() {
                @Override
                public void onSuccess() {
                    MaterialToast.showToast(R.string.toast_add_favourite);
                }
                @Override
                public void onFailure(String eMsg) {
                    MaterialToast.showToast(R.string.failed_to_add);
                }
            });
        }
    }

    @SuppressLint("InflateParams")
    public static AlertDialog getAddToFavoriteDialog(final Context context, final GalleryInfo gi) {

        // TODO List do not work final

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.add_to_favorite, null);
        final CheckBox cb = (CheckBox) view.findViewById(R.id.set_default);
        ListView lv = (ListView) view.findViewById(R.id.list);
        lv.setAdapter(new ArrayAdapter<String>(mContext,
                R.layout.select_dialog_item, FAVORITE_TITLES));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                lastAddToFavoriteDialog.dismiss();
                lastAddToFavoriteDialog = null;

                if (cb.isChecked())
                    Config.setDefaultFavorite(position - 1);

                switch (position) {
                case 0:
                    Data.getInstance().addLocalFavourite(gi);
                    MaterialToast.showToast(R.string.toast_add_favourite);
                    break;
                default:
                    EhClient.getInstance().addToFavorite(gi.gid,
                            gi.token, position - 1, null, new EhClient.OnAddToFavoriteListener() {
                        @Override
                        public void onSuccess() {
                            MaterialToast.showToast(R.string.toast_add_favourite);
                        }
                        @Override
                        public void onFailure(String eMsg) {
                            MaterialToast.showToast(R.string.failed_to_add);
                        }
                    });
                }
            }
        });

        return lastAddToFavoriteDialog = new MaterialAlertDialog.Builder(context)
                .setTitle(R.string.where_to_add)
                .setView(view, false).setNegativeButton(android.R.string.cancel)
                .setPositiveButton(android.R.string.ok).create();
    }
}
