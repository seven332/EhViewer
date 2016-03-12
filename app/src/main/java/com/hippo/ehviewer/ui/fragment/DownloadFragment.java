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

package com.hippo.ehviewer.ui.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.hippo.app.ProgressDialog;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.spider.SpiderQueen;
import com.hippo.ehviewer.ui.DirPickerActivity;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    public static final int REQUEST_CODE_PICK_IMAGE_DIR = 0;
    public static final int REQUEST_CODE_PICK_IMAGE_DIR_L = 1;

    public static final String KEY_DOWNLOAD_LOCATION = "download_location";
    public static final String KEY_RESTORE_DOWNLOAD_ITEMS = "restore_download_items";

    @Nullable
    private Preference mDownloadLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.download_settings);

        mDownloadLocation = findPreference(KEY_DOWNLOAD_LOCATION);
        Preference restoreDownloadItems = findPreference(KEY_RESTORE_DOWNLOAD_ITEMS);

        onUpdateDownloadLocation();

        if (mDownloadLocation != null) {
            mDownloadLocation.setOnPreferenceClickListener(this);
        }
        restoreDownloadItems.setOnPreferenceClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDownloadLocation = null;
    }

    public void onUpdateDownloadLocation() {
        UniFile file = Settings.getDownloadLocation();
        if (mDownloadLocation != null) {
            if (file != null) {
                mDownloadLocation.setSummary(file.getUri().toString());
            } else {
                mDownloadLocation.setSummary(R.string.settings_download_invalid_download_location);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_DOWNLOAD_LOCATION.equals(key)) {
            int sdk = Build.VERSION.SDK_INT;
            if (sdk < Build.VERSION_CODES.KITKAT) {
                openDirPicker();
            } else if (sdk < Build.VERSION_CODES.LOLLIPOP) {
                showDirPickerDialogKK();
            } else {
                showDirPickerDialogL();
            }
            return true;
        } else if (KEY_RESTORE_DOWNLOAD_ITEMS.equals(key)) {
            ProgressDialog dialog = ProgressDialog.show(getActivity(), null,
                    getString(R.string.settings_download_restoring), true, false);
            new RestoreTask(dialog).execute();
            return true;
        }
        return false;
    }

    private void showDirPickerDialogKK() {
        new AlertDialog.Builder(getActivity()).setMessage(R.string.settings_download_pick_dir_kk)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openDirPicker();
                    }
                }).show();
    }

    private void showDirPickerDialogL() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        openDirPicker();
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        openDirPickerL();
                        break;
                }
            }
        };

        new AlertDialog.Builder(getActivity()).setMessage(R.string.settings_download_pick_dir_l)
                .setPositiveButton(android.R.string.ok, listener)
                .setNeutralButton(R.string.settings_download_document, listener)
                .show();
    }

    private void openDirPicker() {
        UniFile uniFile = Settings.getDownloadLocation();
        Intent intent = new Intent(getActivity(), DirPickerActivity.class);
        if (uniFile != null) {
            intent.putExtra(DirPickerActivity.KEY_FILE_URI, uniFile.getUri());
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE_DIR);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void openDirPickerL() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE_DIR_L);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_IMAGE_DIR: {
                if (resultCode == Activity.RESULT_OK) {
                    UniFile uniFile = UniFile.fromUri(getActivity(), data.getData());
                    if (uniFile != null) {
                        Settings.putDownloadLocation(uniFile);
                        onUpdateDownloadLocation();
                    } else {
                        Toast.makeText(getActivity(), R.string.settings_download_cant_get_download_location,
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case REQUEST_CODE_PICK_IMAGE_DIR_L: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri treeUri = data.getData();
                    getActivity().getContentResolver().takePersistableUriPermission(
                            treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    UniFile uniFile = UniFile.fromTreeUri(getActivity(), treeUri);
                    if (uniFile != null) {
                        Settings.putDownloadLocation(uniFile);
                        onUpdateDownloadLocation();
                    } else {
                        Toast.makeText(getActivity(), R.string.settings_download_cant_get_download_location,
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }


    private class RestoreTask extends AsyncTask<Void, Void, List<GalleryInfo>> {

        private final ProgressDialog mDialog;
        private final DownloadManager mManager;

        public RestoreTask(ProgressDialog dialog) {
            mDialog = dialog;
            mManager = EhApplication.getDownloadManager(getActivity());
        }

        private GalleryInfo getGalleryInfo(UniFile file) {
            if (null == file || !file.isDirectory()) {
                return null;
            }
            UniFile siFile = file.findFile(SpiderQueen.SPIDER_INFO_FILENAME);
            if (null == siFile) {
                return null;
            }

            InputStream is = null;
            try {
                is = siFile.openInputStream();
                // Skip start page
                IOUtils.readAsciiLine(is);
                long gid = Long.parseLong(IOUtils.readAsciiLine(is));
                if (mManager.containDownloadInfo(gid)) {
                    return null;
                }
                String token = IOUtils.readAsciiLine(is);
                GalleryInfo galleryInfo = new GalleryInfo();
                galleryInfo.gid = gid;
                galleryInfo.token = token;
                return galleryInfo;
            } catch (IOException e) {
                return null;
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        @Override
        protected List<GalleryInfo> doInBackground(Void... params) {
            UniFile dir = Settings.getDownloadLocation();
            if (null == dir) {
                return null;
            }

            List<GalleryInfo> galleryInfoList = new ArrayList<>();

            UniFile[] files = dir.listFiles();
            for (UniFile file: files) {
                GalleryInfo galleryInfo = getGalleryInfo(file);
                if (null != galleryInfo) {
                    galleryInfoList.add(galleryInfo);
                }
            }

            if (0 == galleryInfoList.size()) {
                return null;
            } else {
                return galleryInfoList;
            }
        }

        @Override
        protected void onPostExecute(List<GalleryInfo> galleryInfoList) {
            if (null == galleryInfoList) {
                Toast.makeText(getActivity(), R.string.settings_download_restore_not_found,
                        Toast.LENGTH_SHORT).show();
                mDialog.dismiss();
                return;
            }

            EhRequest request = new EhRequest();
            request.setMethod(EhClient.METHOD_FILL_GALLERY_LIST_BY_API);
            request.setArgs(galleryInfoList);
            request.setCallback(new EhClient.Callback<List<GalleryInfo>>() {
                @Override
                public void onSuccess(List<GalleryInfo> galleryInfoList) {
                    DownloadManager downloadManager = EhApplication.getDownloadManager(getActivity());

                    int count = 0;
                    for (int i = 0, n = galleryInfoList.size(); i < n; i++) {
                        GalleryInfo galleryInfo = galleryInfoList.get(i);
                        // Avoid failed gallery info
                        if (null != galleryInfo.title) {
                            downloadManager.addDownload(galleryInfo, null);
                            count++;
                        }
                    }

                    Toast.makeText(getActivity(),
                            getString(R.string.settings_download_restore_successfully, count),
                            Toast.LENGTH_SHORT).show();

                    mDialog.dismiss();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getActivity(), R.string.settings_download_restore_failed,
                            Toast.LENGTH_SHORT).show();
                    mDialog.dismiss();
                }

                @Override
                public void onCancel() {
                    mDialog.dismiss();
                }
            });

            EhClient client = EhApplication.getEhClient(getActivity());
            client.execute(request);
        }
    }
}
