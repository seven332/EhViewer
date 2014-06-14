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

import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ui.MangaDetailActivity;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.SuperToast;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

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
    
    public static AlertDialog getAddToFavoriteDialog(final Context context, final GalleryInfo gi) {
        DialogBuilder db = new DialogBuilder(context).setView(R.layout.add_to_favorite, false);
        db.setTitle(R.string.where_to_add);
        db.setSimpleNegativeButton();
        ViewGroup vg = db.getCustomLayout();
        final CheckBox cb = (CheckBox)vg.findViewById(R.id.set_default);
        ListView lv = (ListView)vg.findViewById(R.id.list);
        lv.setAdapter(new ArrayAdapter<String>(mContext,
                R.layout.list_item_text, FAVORITE_TITLES));
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
                    ((AppContext)mContext).getData().addLocalFavourite(gi);
                    new SuperToast(context).setMessage(R.string.toast_add_favourite).show();
                    break;
                default:
                    ((AppContext)mContext).getEhClient().addToFavorite(gi.gid,
                            gi.token, position - 1, null, new EhClient.OnAddToFavoriteListener() {
                        @Override
                        public void onSuccess() {
                            new SuperToast(context).setMessage(R.string.toast_add_favourite).show();
                            // Analytics
                            Analytics.addToFavoriteGallery(context, gi);
                        }
                        @Override
                        public void onFailure(String eMsg) {
                            new SuperToast(context).setMessage(R.string.failed_to_add).show();
                        }
                    });
                }
            }
        });
        return lastAddToFavoriteDialog = db.create();
    }
}
