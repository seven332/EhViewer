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

package com.hippo.ehviewer.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.SearchRecentSuggestions;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.SimpleSuggestionProvider;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.app.MaterialProgressDialog;
import com.hippo.ehviewer.data.ApiGalleryInfo;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.DownloadInfo;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable.Stroke;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.EhInfo;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.preference.ListPreference;
import com.hippo.ehviewer.util.BgThread;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.DialogUtils;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.widget.CategoryTable;
import com.hippo.ehviewer.widget.FileExplorerView;
import com.hippo.ehviewer.widget.MaterialToast;
import com.hippo.ehviewer.widget.SuggestionHelper;

public class SettingsActivity extends AbsPreferenceActivity {
    @SuppressWarnings("unused")
    private static String TAG = SettingsActivity.class.getSimpleName();

    private int mThemeColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set random color
        mThemeColor = Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor();
        getActionBar().setBackgroundDrawable(new ColorDrawable(mThemeColor));
        Ui.colorStatusBarL(this, mThemeColor);

        // Menu
        MaterialIndicatorDrawable materialIndicator = new MaterialIndicatorDrawable(this, Color.WHITE, Stroke.THIN);
        materialIndicator.setIconState(MaterialIndicatorDrawable.IconState.ARROW);
        Ui.setMaterialIndicator(getActionBar(), materialIndicator);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
    }

    private static final String[] ENTRY_FRAGMENTS = { DisplayFragment.class.getName(), EhFragment.class.getName(),
            ReadFragment.class.getName(), DownloadFragment.class.getName(), AdvancedFragment.class.getName(),
            AboutFragment.class.getName() };

    private static final int[] FRAGMENT_ICONS = { R.drawable.ic_setting_display, R.drawable.ic_action_panda,
            R.drawable.ic_setting_read, R.drawable.ic_setting_download, R.drawable.ic_setting_advanced,
            R.drawable.ic_setting_about };

    @Override
    protected boolean isValidFragment(String fragmentName) {
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName))
                return true;
        }
        return false;
    }

    public static class DisplayFragment extends PreferenceFragment implements
            Preference.OnPreferenceChangeListener {

        private static final String KEY_SCREEN_ORIENTATION = "screen_orientation";
        private static final String KEY_RANDOM_THEME_COLOR = "random_theme_color";
        private static final String KEY_THEME_COLOR = "theme_color";

        private ListPreference mScreenOrientation;
        private CheckBoxPreference mRandomThemeColor;
        private Preference mThemeColor;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.display_settings);

            mScreenOrientation = (ListPreference) findPreference(KEY_SCREEN_ORIENTATION);
            mScreenOrientation.setOnPreferenceChangeListener(this);
            mRandomThemeColor = (CheckBoxPreference) findPreference(KEY_RANDOM_THEME_COLOR);
            mRandomThemeColor.setOnPreferenceChangeListener(this);
            mThemeColor = findPreference(KEY_THEME_COLOR);

            mThemeColor.setEnabled(!mRandomThemeColor.isChecked());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String key = preference.getKey();
            if (KEY_SCREEN_ORIENTATION.equals(key)) {
                getActivity().setRequestedOrientation(Config.screenOriPre2Value(Integer.parseInt((String) newValue)));

            } else if (KEY_RANDOM_THEME_COLOR.equals(key)) {
                MaterialToast.showToast(R.string.restart_to_take_effect);
                mThemeColor.setEnabled(!(Boolean) newValue);
            }

            return true;
        }
    }

    public static class EhFragment extends PreferenceFragment implements
            Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        private static final int[] EXCULDE_TAG_GROUP_RESID = { R.id.tag_group_reclass, R.id.tag_group_language,
                R.id.tag_group_parody, R.id.tag_group_character, R.id.tag_group_group, R.id.tag_group_artist,
                R.id.tag_group_male, R.id.tag_group_female };

        private static final int[] EXCULDE_TAG_GROUP_ID = { 0x1, 0x2, 0x4, 0x8, 0x10, 0x20, 0x40, 0x80 };

        private static final int[] EXCULDE_LANGUAGE_RESID = { R.id.el_japanese_translated, R.id.el_japanese_rewrite,
                R.id.el_english_original, R.id.el_english_translated, R.id.el_english_rewrite,
                R.id.el_chinese_original, R.id.el_chinese_translated, R.id.el_chinese_rewrite, R.id.el_dutch_original,
                R.id.el_dutch_translated, R.id.el_dutch_rewrite, R.id.el_french_original, R.id.el_french_translated,
                R.id.el_french_rewrite, R.id.el_german_original, R.id.el_german_translated, R.id.el_german_rewrite,
                R.id.el_hungarian_original, R.id.el_hungarian_translated, R.id.el_hungarian_rewrite,
                R.id.el_italian_original, R.id.el_italian_translated, R.id.el_italian_rewrite, R.id.el_korean_original,
                R.id.el_korean_translated, R.id.el_korean_rewrite, R.id.el_polish_original, R.id.el_polish_translated,
                R.id.el_polish_rewrite, R.id.el_portuguese_original, R.id.el_portuguese_translated,
                R.id.el_portuguese_rewrite, R.id.el_russian_original, R.id.el_russian_translated,
                R.id.el_russian_rewrite, R.id.el_spanish_original, R.id.el_spanish_translated, R.id.el_spanish_rewrite,
                R.id.el_thai_original, R.id.el_thai_translated, R.id.el_thai_rewrite, R.id.el_vietnamese_original,
                R.id.el_vietnamese_translated, R.id.el_vietnamese_rewrite, R.id.el_other_original,
                R.id.el_other_translated, R.id.el_other_rewrite, };

        private static final String[] EXCULDE_LANGUAGE_ID = { "1024", "2048", "1", "1025", "2049", "10", "1034",
                "2058", "20", "1044", "2068", "30", "1054", "2078", "40", "1064", "2088", "50", "1074", "2098", "60",
                "1084", "2108", "70", "1094", "2118", "80", "1104", "2128", "90", "1114", "2138", "100", "1124",
                "2148", "110", "1134", "2158", "120", "1144", "2168", "130", "1154", "2178", "255", "1279", "2303" };

        private static final String KEY_LIST_DEFAULT_CATEGORY = "list_default_category";
        private static final String KEY_EXCULDE_TAG_GROUP = "exculde_tag_group";
        private static final String KEY_EXCULDE_LANGUAGE = "exculde_language";
        private static final String KEY_CLEAR_SUGGESTIONS = "clear_suggestions";
        private static final String KEY_PREVIEW_MODE = "preview_mode";
        private static final String KEY_DEFAULT_FAVORITE = "default_favorite";

        private Preference mListDefaultCategory;
        private Preference mExculdeTagGroup;
        private Preference mExculdeLanguage;
        private Preference mClearSuggestions;
        private ListPreference mPreviewMode;
        private ListPreference mDefaultFavorite;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.eh_settings);

            mListDefaultCategory = findPreference(KEY_LIST_DEFAULT_CATEGORY);
            mListDefaultCategory.setOnPreferenceClickListener(this);
            mExculdeTagGroup = findPreference(KEY_EXCULDE_TAG_GROUP);
            mExculdeTagGroup.setOnPreferenceClickListener(this);
            mExculdeLanguage = findPreference(KEY_EXCULDE_LANGUAGE);
            mExculdeLanguage.setOnPreferenceClickListener(this);
            mClearSuggestions = findPreference(KEY_CLEAR_SUGGESTIONS);
            mClearSuggestions.setOnPreferenceClickListener(this);
            mPreviewMode = (ListPreference) findPreference(KEY_PREVIEW_MODE);
            mPreviewMode.setOnPreferenceChangeListener(this);
            mDefaultFavorite = (ListPreference) findPreference(KEY_DEFAULT_FAVORITE);

            int i = 0;
            String[] entrise = new String[Favorite.FAVORITE_TITLES.length + 1];
            entrise[i++] = getString(R.string.none);
            for (String str : Favorite.FAVORITE_TITLES)
                entrise[i++] = str;
            mDefaultFavorite.setEntries(entrise);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String key = preference.getKey();
            if (KEY_PREVIEW_MODE.equals(key)) {
                String newPreviewMode = (String) newValue;
                EhInfo.getInstance(getActivity()).setPreviewMode(newPreviewMode);
            }
            return true;
        }

        @SuppressLint("InflateParams")
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_LIST_DEFAULT_CATEGORY.equals(key)) {
                int defaultCat = Config.getDefaultCat();
                final CategoryTable ct = new CategoryTable(getActivity());
                ct.setCategory(defaultCat);

                new MaterialAlertDialog.Builder(getActivity()).setTitle(R.string.list_default_category_title)
                        .setView(ct, true).setPositiveButton(android.R.string.ok)
                        .setNegativeButton(android.R.string.cancel)
                        .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                            @Override
                            public boolean onClick(MaterialAlertDialog dialog, int which) {
                                if (which == MaterialAlertDialog.POSITIVE) {
                                    int defaultCat = ct.getCategory();
                                    Config.setDefaultCat(defaultCat);
                                    EhInfo.getInstance(getActivity()).setDefaultCat(defaultCat);
                                }
                                return true;
                            }
                        }).show();

            } else if (KEY_EXCULDE_TAG_GROUP.equals(key)) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                final TableLayout tl = (TableLayout) inflater.inflate(R.layout.exculde_tag_group, null);
                setExculdeTagGroup(tl, Config.getExculdeTagGroup());

                new MaterialAlertDialog.Builder(getActivity()).setTitle(R.string.exculde_tag_group_title).setView(tl, true)
                        .setPositiveButton(android.R.string.ok).setNegativeButton(android.R.string.cancel)
                        .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                            @Override
                            public boolean onClick(MaterialAlertDialog dialog, int which) {
                                int newValue = getExculdeTagGroup(tl);
                                Config.setExculdeTagGroup(newValue);
                                EhInfo.getInstance(getActivity()).setExculdeTagGroup(newValue);
                                return true;
                            }
                        }).show();

            } else if (KEY_EXCULDE_LANGUAGE.equals(key)) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                final TableLayout tl = (TableLayout) inflater.inflate(R.layout.exculde_language, null);
                setExculdeLanguage(tl, Config.getExculdeLanguage());

                new MaterialAlertDialog.Builder(getActivity()).setTitle(R.string.exculde_language_title).setView(tl, true)
                        .setPositiveButton(android.R.string.ok).setNegativeButton(android.R.string.cancel)
                        .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                            @Override
                            public boolean onClick(MaterialAlertDialog dialog, int which) {
                                String newValue = getExculdeLanguage(tl);
                                Config.setExculdeLanguage(newValue);
                                EhInfo.getInstance(getActivity()).setExculdeLanguage(newValue);
                                return true;
                            }
                        }).show();

            } else if (KEY_CLEAR_SUGGESTIONS.equals(key)) {
                SearchRecentSuggestions suggestions = SuggestionHelper.getInstance(getActivity(),
                        SimpleSuggestionProvider.AUTHORITY, SimpleSuggestionProvider.MODE);
                suggestions.clearHistory();
            }
            return true;
        }

        private static void setExculdeTagGroup(TableLayout tl, int value) {
            for (int i = 0; i < EXCULDE_TAG_GROUP_RESID.length; i++) {
                CheckBox cb = (CheckBox) tl.findViewById(EXCULDE_TAG_GROUP_RESID[i]);
                cb.setChecked(Utils.int2boolean(value & EXCULDE_TAG_GROUP_ID[i]));
            }
        }

        private static int getExculdeTagGroup(TableLayout tl) {
            int newValue = 0;
            for (int i = 0; i < EXCULDE_TAG_GROUP_RESID.length; i++) {
                CheckBox cb = (CheckBox) tl.findViewById(EXCULDE_TAG_GROUP_RESID[i]);
                if (cb.isChecked())
                    newValue |= EXCULDE_TAG_GROUP_ID[i];
            }
            return newValue;
        }

        private static int getLanguage(String id) {
            for (int i = 0; i < EXCULDE_LANGUAGE_ID.length; i++) {
                if (EXCULDE_LANGUAGE_ID[i].equals(id))
                    return EXCULDE_LANGUAGE_RESID[i];
            }
            return 0;
        }

        private static void setExculdeLanguage(TableLayout tl, String value) {
            String[] items = value.split("x");
            for (String item : items) {
                int resId = getLanguage(item);
                if (resId != 0)
                    ((CheckBox) tl.findViewById(resId)).setChecked(true);
            }
        }

        private static String getExculdeLanguage(TableLayout tl) {
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;
            for (int i = 0; i < EXCULDE_LANGUAGE_RESID.length; i++) {
                if (((CheckBox) tl.findViewById(EXCULDE_LANGUAGE_RESID[i])).isChecked()) {
                    if (isFirst)
                        isFirst = false;
                    else
                        sb.append('x');
                    sb.append(EXCULDE_LANGUAGE_ID[i]);
                }
            }
            return sb.toString();
        }
    }

    public static class ReadFragment extends PreferenceFragment implements
            Preference.OnPreferenceClickListener {

        private static final String KEY_CUSTOM_CODEC = "custom_codec";
        private static final String KEY_CLEAN_REDUNDANCY = "clean_redundancy";

        private CheckBoxPreference mCustomCodec;
        private Preference mCleanRedundancy;

        private MaterialProgressDialog mMaterialProgressDialog;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.read_settings);

            mCustomCodec = (CheckBoxPreference) findPreference(KEY_CUSTOM_CODEC);
            mCleanRedundancy = findPreference(KEY_CLEAN_REDUNDANCY);
            mCleanRedundancy.setOnPreferenceClickListener(this);

            if (!Utils.SUPPORT_IMAGE) {
                mCustomCodec.setEnabled(false);
                mCustomCodec.setChecked(Config.getCustomCodec());
                mCustomCodec.setSummary(R.string.custom_codec_summary_not);
            } else {
                mCustomCodec.setChecked(Config.getCustomCodec());
            }
        }

        private boolean isInDownloadList(List<DownloadInfo> diList, String filename) {
            for (DownloadInfo di : diList)
                if (filename.startsWith(String.valueOf(di.galleryInfo.gid)))
                    return true;
            return false;
        }

        private class CleanResponder implements Runnable {

            public static final int STATE_NONE = 0x0;
            public static final int STATE_START = 0x1;
            public static final int STATE_DOING = 0x2;
            public static final int STATE_DONE = 0x3;

            private final int mState;
            private final int mMax;
            private final int mProgress;

            public CleanResponder(int state, int max, int progress) {
                mState = state;
                mMax = max;
                mProgress = progress;
            }

            @Override
            public void run() {

                switch (mState) {
                case STATE_NONE:
                    MaterialToast.showToast(R.string.no_redundancy);
                    break;
                case STATE_START:
                    if (!getActivity().isFinishing()) {
                        mMaterialProgressDialog = MaterialProgressDialog.create(getActivity(), getActivity().getString(R.string.clean_redundancy_title), false);
                        mMaterialProgressDialog.show();
                    }
                    break;
                case STATE_DOING:
                    if (mMaterialProgressDialog != null) {
                        mMaterialProgressDialog.setMax(mMax);
                        mMaterialProgressDialog.setProgress(mProgress);
                    }
                    break;
                case STATE_DONE:
                    if (mMaterialProgressDialog != null) {
                        mMaterialProgressDialog.dismiss();
                        MaterialToast.showToast(String.format(getString(R.string.clean_redundancy), mMax));
                    }
                    mMaterialProgressDialog = null;
                    break;
                }
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_CLEAN_REDUNDANCY.equals(key)) {

                // Create a thread to do clean task
                new BgThread() {
                    @Override
                    public void run() {
                        Data data = Data.getInstance();
                        List<DownloadInfo> diList = data.getAllDownloads();
                        File downloadDir = new File(Config.getDownloadPath());
                        String[] files = downloadDir.list();
                        Handler handler = AppHandler.getInstance();
                        List<File> targetDirList = new ArrayList<File>();

                        if (files == null) {
                            // Check files null
                            handler.post(new CleanResponder(CleanResponder.STATE_NONE, 0, 0));
                            return;
                        }

                        for (String filename : files) {
                            // If in download list, just continue
                            if (isInDownloadList(diList, filename))
                                continue;
                            File dir = new File(downloadDir, filename);
                            // If there is no tag file, just continue
                            if (!new File(dir, EhUtils.EH_DOWNLOAD_FILENAME).exists())
                                continue;
                            // Add to list
                            targetDirList.add(dir);
                        }

                        if (targetDirList.isEmpty()) {
                            handler.post(new CleanResponder(CleanResponder.STATE_NONE, 0, 0));
                            return;
                        } else {
                            handler.post(new CleanResponder(CleanResponder.STATE_START, 0, 0));
                        }
                        // Do delete
                        for (int i = 0; i < targetDirList.size(); i++) {
                            File dir = targetDirList.get(i);
                            Utils.deleteFile(dir);
                            handler.post(new CleanResponder(CleanResponder.STATE_DOING, targetDirList.size(), i + 1));
                        }
                        // Close windows
                        handler.post(new CleanResponder(CleanResponder.STATE_DONE, targetDirList.size(), targetDirList
                                .size()));
                    }
                }.start();
            }
            return true;
        }
    }

    public static class DownloadFragment extends PreferenceFragment implements
            Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        private static final String KEY_DOWNLOAD_PATH = "download_path";
        private static final String KEY_MEDIA_SCAN = "media_scan";

        private AlertDialog mDirSelectDialog;

        private Preference mDownloadPath;
        private CheckBoxPreference mMediaScan;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.download_settings);

            mDownloadPath = findPreference(KEY_DOWNLOAD_PATH);
            mDownloadPath.setOnPreferenceClickListener(this);
            mMediaScan = (CheckBoxPreference) findPreference(KEY_MEDIA_SCAN);
            mMediaScan.setOnPreferenceChangeListener(this);

            mDownloadPath.setSummary(Config.getDownloadPath());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String key = preference.getKey();
            if (KEY_MEDIA_SCAN.equals(key)) {
                boolean value = (Boolean) newValue;
                File nomedia = new File(Config.getDownloadPath(), ".nomedia");
                if (value) {
                    nomedia.delete();
                } else {
                    try {
                        nomedia.createNewFile();
                    } catch (IOException e) {
                    }
                }
            }
            return true;
        }

        @Override
        @SuppressLint("InflateParams")
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_DOWNLOAD_PATH.equals(key)) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.dir_selection, null);
                final FileExplorerView fileExplorerView = (FileExplorerView) view.findViewById(R.id.file_list);
                final TextView warning = (TextView) view.findViewById(R.id.warning);

                String downloadPath = Config.getDownloadPath();
                fileExplorerView.setPath(downloadPath);
                fileExplorerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        fileExplorerView.onItemClick(parent, view, position, id);
                        mDirSelectDialog.setTitle(fileExplorerView.getCurPath());
                        if (fileExplorerView.canWrite())
                            warning.setVisibility(View.GONE);
                        else
                            warning.setVisibility(View.VISIBLE);
                    }
                });
                if (fileExplorerView.canWrite())
                    warning.setVisibility(View.GONE);
                else
                    warning.setVisibility(View.VISIBLE);

                mDirSelectDialog = new MaterialAlertDialog.Builder(getActivity())
                        .setTitle(downloadPath)
                        .setView(view, false,
                                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp2pix(360)))
                        .setActionButton(R.string._new).setPositiveButton(android.R.string.ok)
                        .setNegativeButton(android.R.string.cancel)
                        .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                            @Override
                            public boolean onClick(MaterialAlertDialog dialog, int which) {
                                switch (which) {
                                case MaterialAlertDialog.ACTION:
                                    final EditText et = new EditText(getActivity());
                                    et.setText("New folder"); // TODO
                                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    int x = Ui.dp2pix(8);
                                    lp.leftMargin = x;
                                    lp.rightMargin = x;
                                    lp.topMargin = x;
                                    lp.bottomMargin = x;
                                    new MaterialAlertDialog.Builder(getActivity()).setTitle(R.string.new_folder)
                                            .setView(et, true, lp).setPositiveButton(R.string._new)
                                            .setNegativeButton(android.R.string.cancel)
                                            .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                                                @Override
                                                public boolean onClick(MaterialAlertDialog dialog, int which) {
                                                    if (which == MaterialAlertDialog.POSITIVE) {
                                                        File dir = new File(fileExplorerView.getCurPath(), et.getText()
                                                                .toString());
                                                        dir.mkdirs();
                                                        fileExplorerView.refresh();
                                                    }
                                                    return true;
                                                }
                                            }).show();
                                    return false;
                                case MaterialAlertDialog.POSITIVE:
                                    if (!fileExplorerView.canWrite()) {
                                        MaterialToast.showToast(R.string.cur_dir_not_writable);
                                        return false;
                                    }
                                    String downloadPath = fileExplorerView.getCurPath();
                                    // Update .nomedia file
                                    // TODO Should I delete .nomedia in old
                                    // download dir ?
                                    if (!Config.getMediaScan()) {
                                        try {
                                            new File(Config.getDownloadPath(), ".nomedia").createNewFile();
                                        } catch (IOException e) {
                                        }
                                    }
                                    Config.setDownloadPath(downloadPath);
                                    mDownloadPath.setSummary(downloadPath);
                                    return true;
                                case MaterialAlertDialog.NEGATIVE:
                                default:
                                    return true;
                                }
                            }
                        }).create();
                mDirSelectDialog.show();
            }

            return true;
        }
    }

    public static class AdvancedFragment extends PreferenceFragment implements
            Preference.OnPreferenceClickListener {

        private static final String KEY_FIX_DIRNAME = "fix_dirname";

        private Preference mFixDirname;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.advanced_settings);

            mFixDirname = findPreference(KEY_FIX_DIRNAME);
            mFixDirname.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_FIX_DIRNAME.equals(key)) {
                mFixDirname.setEnabled(false);
                new BgThread() {
                    @Override
                    public void run() {
                        File downloadDir = new File(Config.getDownloadPath());
                        List<File> targetDirList = new ArrayList<File>();

                        String[] list = downloadDir.list();
                        if (list != null) {
                            for (String str : list) {
                                if (str.contains("-"))
                                    continue;
                                File dir = new File(downloadDir, str);
                                if (new File(dir, EhUtils.EH_DOWNLOAD_FILENAME).exists())
                                    targetDirList.add(dir);
                            }
                        }

                        if (targetDirList.size() == 0) {
                            AppHandler.getInstance().post(new Runnable() {
                                @Override
                                public void run() {
                                    mFixDirname.setEnabled(true);
                                    MaterialToast.showToast("未发现可修正项"); // TODO
                                }
                            });
                            return;
                        } else {
                            AppHandler.getInstance().post(new Runnable() {
                                @Override
                                public void run() {
                                    MaterialToast.showToast("开始修正"); // TODO
                                }
                            });
                        }

                        int handleNum = 0;
                        List<File> dirs = new ArrayList<File>();
                        List<Integer> gids = new ArrayList<Integer>();
                        List<String> tokens = new ArrayList<String>();
                        // Do get title from api
                        for (int i = 0; i < targetDirList.size(); i++) {
                            File dir = targetDirList.get(i);
                            File info = new File(dir, EhUtils.EH_DOWNLOAD_FILENAME);
                            InputStream is = null;
                            int gid;
                            String token;
                            try {
                                is = new BufferedInputStream(new FileInputStream(info), 128);
                                // skip read index
                                Utils.readAsciiLine(is);
                                gid = Integer.parseInt(Utils.readAsciiLine(is));
                                token = Utils.readAsciiLine(is);
                                Utils.closeQuietly(is);
                                dirs.add(dir);
                                gids.add(gid);
                                tokens.add(token);
                                // Post api when to 25 or the end
                                if (gids.size() == 25 || i == targetDirList.size() - 1) {
                                    ApiGalleryInfo[] agiArray = EhClient.getInstance().getApiGalleryInfo(
                                            Utils.toIntArray(gids), tokens.toArray(new String[tokens.size()]));
                                    // Change dirname
                                    if (agiArray != null) {
                                        for (int j = 0; j < agiArray.length; j++) {
                                            ApiGalleryInfo agi = agiArray[j];
                                            if (agi != null) {
                                                handleNum++;
                                                dirs.get(j).renameTo(EhUtils.generateGalleryDir(agi.gid, agi.title));
                                            }
                                        }
                                    }
                                    dirs.clear();
                                    gids.clear();
                                    tokens.clear();
                                    // Wait for CD, if not last
                                    if (i != targetDirList.size() - 1)
                                        Thread.sleep(3000);
                                }
                            } catch (Throwable e) {
                                continue;
                            }
                        }
                        final int _handleNum = handleNum;
                        AppHandler.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                mFixDirname.setEnabled(true);
                                MaterialToast.showToast("共修正 " + _handleNum + " 项"); // TODO
                            }
                        });
                    }
                }.start();
            }
            return true;
        }
    }

    public static class AboutFragment extends PreferenceFragment implements
            Preference.OnPreferenceClickListener {

        private static final String KEY_AUTHOR = "author";
        private static final String KEY_TWITTER = "twitter";
        private static final String KEY_CHANGELOG = "changelog";
        private static final String KEY_THANKS = "thanks";
        private static final String KEY_WEBSITE = "website";
        private static final String KEY_SOURCE = "source";
        private static final String KEY_CHECK_UPDATE = "check_for_update";
        private static final String KEY_CLOUD_DRIVE = "cloud_drive";
        private static final String KEY_ABOUT_ANALYICS = "about_analyics";

        private Preference mAuthor;
        private Preference mTwitter;
        private Preference mChangelog;
        private Preference mThanks;
        private Preference mWebsite;
        private Preference mSource;
        private Preference mCheckUpdate;
        private Preference mCloudDrive;
        private Preference mAboutAnalyics;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.about_settings);

            mAuthor = findPreference(KEY_AUTHOR);
            mAuthor.setOnPreferenceClickListener(this);
            mTwitter = findPreference(KEY_TWITTER);
            mTwitter.setOnPreferenceClickListener(this);
            mChangelog = findPreference(KEY_CHANGELOG);
            mChangelog.setOnPreferenceClickListener(this);
            mThanks = findPreference(KEY_THANKS);
            mThanks.setOnPreferenceClickListener(this);
            mWebsite = findPreference(KEY_WEBSITE);
            mWebsite.setOnPreferenceClickListener(this);
            mSource = findPreference(KEY_SOURCE);
            mSource.setOnPreferenceClickListener(this);
            mCheckUpdate = findPreference(KEY_CHECK_UPDATE);
            mCheckUpdate.setOnPreferenceClickListener(this);
            mCloudDrive = findPreference(KEY_CLOUD_DRIVE);
            mCloudDrive.setOnPreferenceClickListener(this);
            mAboutAnalyics = findPreference(KEY_ABOUT_ANALYICS);
            mAboutAnalyics.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_AUTHOR.equals(key)) {
                Intent i = new Intent(Intent.ACTION_SENDTO);
                i.setData(Uri.parse("mailto:ehviewersu@gmail.com"));
                i.putExtra(Intent.EXTRA_SUBJECT, "About EhViewer");
                startActivity(i);

            } else if (KEY_TWITTER.equals(key)) {
                new MaterialAlertDialog.Builder(getActivity())
                        .setTitle(R.string.twitter_title)
                        .setItems(new CharSequence[] { "@EhViewer", "@jkjvinn" },
                                new MaterialAlertDialog.OnClickListener() {
                                    @Override
                                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                                        Uri uri = null;
                                        switch (which) {
                                        case 0:
                                            uri = Uri.parse("https://twitter.com/EhViewer");
                                            break;
                                        case 1:
                                            uri = Uri.parse("https://twitter.com/jkjvinn");
                                            break;
                                        }
                                        if (uri != null) {
                                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                            startActivity(intent);
                                        }
                                        return true;
                                    }
                                }).setNegativeButton(android.R.string.cancel).show();

            } else if (KEY_CHANGELOG.equals(key)) {
                InputStream is = getActivity().getResources().openRawResource(R.raw.change_log);
                new MaterialAlertDialog.Builder(getActivity()).setTitle(R.string.changelog)
                        .setMessage(Utils.InputStream2String(is, "utf-8")).setNegativeButton(android.R.string.cancel)
                        .show();

            } else if (KEY_THANKS.equals(key)) {
                InputStream is = getActivity().getResources().openRawResource(R.raw.thanks);
                final WebView webView = new WebView(getActivity());
                webView.loadData(Utils.InputStream2String(is, "utf-8"), "text/html; charset=UTF-8", null);
                new MaterialAlertDialog.Builder(getActivity())// .setTitle(R.string.thanks)
                        .setView(webView, true).setNegativeButton(android.R.string.cancel).show();

            } else if (KEY_WEBSITE.equals(key)) {
                Uri uri = Uri.parse("http://www.ehviewer.com");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

            } else if (KEY_SOURCE.equals(key)) {
                Uri uri = Uri.parse("https://github.com/seven332/EhViewer");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

            } else if (KEY_CHECK_UPDATE.equals(key)) {
                mCheckUpdate.setSummary(R.string.checking_update);
                mCheckUpdate.setEnabled(false);
                new UpdateHelper((AppContext) getActivity().getApplication()).SetOnCheckUpdateListener(
                        new UpdateHelper.OnCheckUpdateListener() {
                            @Override
                            public void onSuccess(String version, long size, final String url, final String fileName,
                                    String info) {
                                mCheckUpdate.setSummary(R.string.found_update);
                                String sizeStr = Utils.sizeToString(size);
                                AlertDialog dialog = DialogUtils.createUpdateDialog(getActivity(), version, sizeStr, info,
                                        new MaterialAlertDialog.OnClickListener() {
                                            @Override
                                            public boolean onClick(MaterialAlertDialog dialog, int which) {
                                                if (which == MaterialAlertDialog.POSITIVE) {
                                                    HttpHelper hh = new HttpHelper(getActivity());
                                                    hh.downloadInThread(url, new File(Config.getDownloadPath()),
                                                            fileName, false, null, new UpdateHelper.UpdateListener(
                                                                    getActivity(), fileName));
                                                }
                                                return true;
                                            }
                                        }).create();
                                if (!getActivity().isFinishing())
                                    dialog.show();
                            }

                            @Override
                            public void onNoUpdate() {
                                mCheckUpdate.setSummary(R.string.up_to_date);
                                mCheckUpdate.setEnabled(true);
                                UpdateHelper.setEnabled(true);
                            }

                            @Override
                            public void onFailure(String eMsg) {
                                mCheckUpdate.setSummary(eMsg);
                                mCheckUpdate.setEnabled(true);
                                UpdateHelper.setEnabled(true);
                            }
                        }).checkUpdate();

            } else if (KEY_CLOUD_DRIVE.equals(key)) {
                Uri uri = Uri.parse("https://mega.co.nz/#F!xkMChYrI!Q85i3d7kNkhVkwvDePbahw");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

            } else if (KEY_ABOUT_ANALYICS.equals(key)) {
                new MaterialAlertDialog.Builder(getActivity()).setTitle(R.string.about_analyics_title)
                        .setMessageAutoLink(Linkify.WEB_URLS).setMessage(R.string.about_analyics_comment)
                        .setNegativeButton(android.R.string.cancel).show();

            }
            return true;
        }
    }
}
