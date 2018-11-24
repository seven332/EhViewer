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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.util.LogCat;
import com.hippo.util.ReadableTime;

import java.io.File;
import java.util.Arrays;

public class AdvancedFragment extends PreferenceFragment
    implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String KEY_DUMP_LOGCAT = "dump_logcat";
    private static final String KEY_CLEAR_MEMORY_CACHE = "clear_memory_cache";
    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String KEY_EXPORT_DATA = "export_data";
    private static final String KEY_IMPORT_DATA = "import_data";

    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_settings);

        Preference dumpLogcat = findPreference(KEY_DUMP_LOGCAT);
        Preference clearMemoryCache = findPreference(KEY_CLEAR_MEMORY_CACHE);
        Preference appLanguage = findPreference(KEY_APP_LANGUAGE);
        Preference exportData = findPreference(KEY_EXPORT_DATA);
        Preference importData = findPreference(KEY_IMPORT_DATA);

        dumpLogcat.setOnPreferenceClickListener(this);
        clearMemoryCache.setOnPreferenceClickListener(this);
        exportData.setOnPreferenceClickListener(this);
        importData.setOnPreferenceClickListener(this);

        appLanguage.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_DUMP_LOGCAT.equals(key)) {
            boolean ok;
            File file = null;
            File dir = AppConfig.getExternalLogcatDir();
            if (dir != null) {
                file = new File(dir, "logcat-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".txt");
                ok = LogCat.save(file);
            } else {
                ok = false;
            }
            Resources resources = getResources();
            Toast.makeText(getActivity(),
                    ok ? resources.getString(R.string.settings_advanced_dump_logcat_to, file.getPath()) :
                            resources.getString(R.string.settings_advanced_dump_logcat_failed), Toast.LENGTH_SHORT).show();
            return true;
        } else if (KEY_CLEAR_MEMORY_CACHE.equals(key)) {
            ((EhApplication) getActivity().getApplication()).clearMemoryCache();
            Runtime.getRuntime().gc();
        } else if (KEY_EXPORT_DATA.equals(key)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                showExportDialog();
                return true;

            } else if (defaultExportData()){
                    return true;
            }
            Toast.makeText(getActivity(),R.string.settings_advanced_export_data_failed, Toast.LENGTH_SHORT).show();
            return true;
        } else if (KEY_IMPORT_DATA.equals(key)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                showImportDialog();
            } else {
                defaulfImportData();
            }

            getActivity().setResult(Activity.RESULT_OK);
            return true;
        }
        return false;
    }

    private void showExportDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.settings_advanced_export_data_location));
        final CharSequence[] items = new CharSequence[]{
                getString(R.string.settings_advanced_data_device_storage),
                getString(R.string.settings_advanced_data_document_storage)
        };
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i){
                    case 0:
                        defaultExportData();
                        break;
                    case 1:
                        customExportData();
                        break;
                }
            }
        });
        builder.show();
    }

    private boolean defaultExportData(){
        File dir = AppConfig.getExternalDataDir();
        if (dir != null) {
            File file = new File(dir, ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".db");
            if (EhDB.exportDB(getActivity(), file)) {
                Toast.makeText(getActivity(),
                        getString(R.string.settings_advanced_export_data_to, file.getPath()), Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }

    private void customExportData(){
        String filename = ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".db";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, WRITE_REQUEST_CODE);

    }

    private void showImportDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.settings_advanced_import_data_location));
        final CharSequence[] items = new CharSequence[]{
                getString(R.string.settings_advanced_data_device_storage),
                getString(R.string.settings_advanced_data_document_storage)
        };
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i){
                    case 0:
                        defaulfImportData();
                        break;
                    case 1:
                        customImportData();
                        break;
                }
            }
        });
        builder.show();
    }

    private void defaulfImportData(){
        Context context = getActivity();
        final File dir = AppConfig.getExternalDataDir();
        if (null == dir) {
            Toast.makeText(context, R.string.cant_get_data_dir, Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] files = dir.list();
        if (null == files || files.length <= 0) {
            Toast.makeText(context, R.string.cant_find_any_data, Toast.LENGTH_SHORT).show();
            return;
        }
        Arrays.sort(files);
        new AlertDialog.Builder(context).setItems(files, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File file = new File(dir, files[which]);
                String error = EhDB.importDB(context, file);
                if (null == error) {
                    error = context.getString(R.string.settings_advanced_import_data_successfully);
                }
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    private void customImportData(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                if (EhDB.exportDB(getActivity(), uri)) {
                    Toast.makeText(getActivity(),
                            getString(R.string.settings_advanced_export_data_to, uri.toString()), Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                String error = EhDB.importDB(getActivity(), uri);
                if (null == error) {
                    error = getString(R.string.settings_advanced_import_data_successfully);
                }
                Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_APP_LANGUAGE.equals(key)) {
            ((EhApplication) getActivity().getApplication()).recreate();
            return true;
        }
        return false;
    }
}
