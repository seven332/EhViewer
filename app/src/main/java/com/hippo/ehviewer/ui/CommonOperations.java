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

package com.hippo.ehviewer.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import com.hippo.app.ListCheckBoxDialogBuilder;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.DownloadLabelRaw;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.download.DownloadService;

import java.util.List;

public final class CommonOperations {

    private static void doAddToFavorites(Activity activity, GalleryInfo galleryInfo,
            int slot, EhClient.Callback<Void> listener) {
        if (slot == -1) {
            EhDB.addLocalFavorites(galleryInfo);
            listener.onSuccess(null);
        } else if (slot >= 0 && slot <= 9) {
            EhClient client = EhApplication.getEhClient(activity);
            EhRequest request = new EhRequest();
            request.setMethod(EhClient.METHOD_ADD_FAVORITES);
            request.setArgs(galleryInfo.gid, galleryInfo.token, slot, "");
            request.setCallback(listener);
            client.execute(request);
        } else {
            listener.onFailure(new Exception()); // TODO Add text
        }
    }

    public static void addToFavorites(final Activity activity, final GalleryInfo galleryInfo,
            final EhClient.Callback<Void> listener) {
        int slot = Settings.getDefaultFavSlot();
        if (slot >= -1 && slot <= 9) {
            doAddToFavorites(activity, galleryInfo, slot, listener);
        } else {
            String[] items = new String[11];
            items[0] = activity.getString(R.string.local_favorites);
            String[] favCat = Settings.getFavCat();
            System.arraycopy(favCat, 0, items, 1, 10);
            new ListCheckBoxDialogBuilder(activity, items,
                    new ListCheckBoxDialogBuilder.OnItemClickListener() {
                        @Override
                        public void onItemClick(ListCheckBoxDialogBuilder builder, AlertDialog dialog, int position) {
                            int slot = position - 1;
                            doAddToFavorites(activity, galleryInfo, slot, listener);
                            if (builder.isChecked()) {
                                Settings.putDefaultFavSlot(slot);
                            } else {
                                Settings.putDefaultFavSlot(Settings.INVALID_DEFAULT_FAV_SLOT);
                            }
                        }
                    }, activity.getString(R.string.remember_favorite_slot), false)
                    .setTitle(R.string.add_favorites_dialog_title)
                    .show();
        }
    }

    public static void removeFromFavorites(Activity activity, GalleryInfo galleryInfo,
            final EhClient.Callback<Void> listener) {
        EhClient client = EhApplication.getEhClient(activity);
        EhRequest request = new EhRequest();
        request.setMethod(EhClient.METHOD_ADD_FAVORITES);
        request.setArgs(galleryInfo.gid, galleryInfo.token, -1, "");
        request.setCallback(listener);
        client.execute(request);
    }

    public static void startDownload(final Activity activity, final GalleryInfo galleryInfo) {
        final DownloadManager dm = EhApplication.getDownloadManager(activity);

        boolean justStart = dm.containDownloadInfo(galleryInfo.gid);
        String label = null;
        if (!justStart && Settings.getHasDefaultDownloadLabel()) {
            label = Settings.getDefaultDownloadLabel();
            justStart = label == null || dm.containLabel(label);
        }

        if (justStart) {
            // Already in download list or get default label
            Intent intent = new Intent(activity, DownloadService.class);
            intent.setAction(DownloadService.ACTION_START);
            intent.putExtra(DownloadService.KEY_LABEL, label);
            intent.putExtra(DownloadService.KEY_GALLERY_INFO, galleryInfo);
            activity.startService(intent);
        } else {
            // Let use chose label
            List<DownloadLabelRaw> list = dm.getLabelList();
            final String[] items = new String[list.size() + 1];
            items[0] = activity.getString(R.string.default_download_label_name);
            for (int i = 0, n = list.size(); i < n; i++) {
                items[i + 1] = list.get(i).getLabel();
            }

            new ListCheckBoxDialogBuilder(activity, items,
                    new ListCheckBoxDialogBuilder.OnItemClickListener() {
                        @Override
                        public void onItemClick(ListCheckBoxDialogBuilder builder, AlertDialog dialog, int position) {
                            String label;
                            if (position == 0) {
                                label = null;
                            } else {
                                label = items[position];
                                if (!dm.containLabel(label)) {
                                    label = null;
                                }
                            }
                            // Start download
                            Intent intent = new Intent(activity, DownloadService.class);
                            intent.setAction(DownloadService.ACTION_START);
                            intent.putExtra(DownloadService.KEY_LABEL, label);
                            intent.putExtra(DownloadService.KEY_GALLERY_INFO, galleryInfo);
                            activity.startService(intent);
                            // Save settings
                            if (builder.isChecked()) {
                                Settings.putHasDefaultDownloadLabel(true);
                                Settings.putDefaultDownloadLabel(label);
                            } else {
                                Settings.putHasDefaultDownloadLabel(false);
                            }
                        }
                    }, activity.getString(R.string.remember_download_label), false)
                    .setTitle(R.string.download)
                    .show();
        }
    }
}
