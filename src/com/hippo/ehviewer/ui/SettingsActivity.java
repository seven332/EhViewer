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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.ehclient.EhInfo;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.preference.AutoListPreference;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.CategoryTable;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FileExplorerView;
import com.hippo.ehviewer.widget.MaterialToast;
import com.hippo.ehviewer.widget.SuperDialogUtil;

public class SettingsActivity extends AbsPreferenceActivity {
    @SuppressWarnings("unused")
    private static String TAG = SettingsActivity.class.getSimpleName();

    private List<TranslucentPreferenceFragment> mFragments;
    private ListView mListView;

    public void adjustPadding(int paddingTop, int paddingBottom) {
        mListView.setPadding(mListView.getPaddingLeft(), paddingTop,
                mListView.getPaddingRight(), paddingBottom);
    }

    @Override
    public void onOrientationChanged(int paddingTop, int paddingBottom) {
        for (TranslucentPreferenceFragment f : mFragments) {
            if (f.isVisible()) {
                f.adjustPadding(paddingTop, paddingBottom);
                return;
            }
        }

        // If no fragment is visible, just headers is shown
        adjustPadding(paddingTop, paddingBottom);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set random color
        int color = Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor();
        color = color & 0x00ffffff | 0xdd000000;
        Drawable drawable = new ColorDrawable(color);
        final ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(drawable);
        Ui.translucent(this, color);

        actionBar.setDisplayHomeAsUpEnabled(true);

        mFragments = new LinkedList<TranslucentPreferenceFragment>();

        mListView = getListView();
        mListView.setClipToPadding(false);
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

    public List<TranslucentPreferenceFragment> getFragments() {
        return mFragments;
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
    }

    private static final String[] ENTRY_FRAGMENTS = {
        DisplayFragment.class.getName(),
        EhFragment.class.getName(),
        DataFragment.class.getName(),
        AboutFragment.class.getName()
    };

    private static final int[] FRAGMENT_ICONS = {
        R.drawable.ic_setting_display,
        R.drawable.ic_action_panda,
        R.drawable.ic_setting_data,
        R.drawable.ic_action_info
    };

    @Override
    protected boolean isValidFragment(String fragmentName) {
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
        }
        return false;
    }

    public static abstract class TranslucentPreferenceFragment extends PreferenceFragment {

        protected SettingsActivity mActivity;
        private ListView mListView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mActivity = (SettingsActivity)getActivity();

            String fragmentName = getClass().getName();
            int fragmentIndex = -1;
            for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
                if (ENTRY_FRAGMENTS[i].equals(fragmentName)) {
                    fragmentIndex = i;
                    break;
                }
            }
            if (fragmentIndex >= 0)
                mActivity.getActionBar().setIcon(
                        FRAGMENT_ICONS[fragmentIndex]);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            SettingsActivity activity = (SettingsActivity)getActivity();
            activity.getFragments().add(this);

            View child;
            if (view instanceof ViewGroup
                    && (child = ((ViewGroup)view).getChildAt(0)) != null
                    && child instanceof ListView) {
                mListView = (ListView)child;
                mListView.setClipToPadding(false);

                int[] padding = new int[2];
                Ui.getWindowPadding(mActivity.getResources(), padding);
                adjustPadding(padding[0], padding[1]);
            }
        }

        public void adjustPadding(int paddingTop, int paddingBottom) {
            mListView.setPadding(mListView.getPaddingLeft(), paddingTop,
                    mListView.getPaddingRight(), paddingBottom);
        }
    }

    public static class DisplayFragment extends TranslucentPreferenceFragment
            implements Preference.OnPreferenceChangeListener {

        private static final String KEY_SCREEN_ORIENTATION = "screen_orientation";
        private static final String KEY_RANDOM_THEME_COLOR = "random_theme_color";
        private static final String KEY_THEME_COLOR = "theme_color";

        private AutoListPreference mScreenOrientation;
        private CheckBoxPreference mRandomThemeColor;
        private Preference mThemeColor;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.display_settings);

            mScreenOrientation = (AutoListPreference)findPreference(KEY_SCREEN_ORIENTATION);
            mScreenOrientation.setOnPreferenceChangeListener(this);
            mRandomThemeColor = (CheckBoxPreference)findPreference(KEY_RANDOM_THEME_COLOR);
            mRandomThemeColor.setOnPreferenceChangeListener(this);
            mThemeColor = findPreference(KEY_THEME_COLOR);

            mThemeColor.setEnabled(!mRandomThemeColor.isChecked());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String key = preference.getKey();
            if (KEY_SCREEN_ORIENTATION.equals(key)) {
                mActivity.setRequestedOrientation(
                        Config.screenOriPre2Value(Integer.parseInt((String) newValue)));

            } else if (KEY_RANDOM_THEME_COLOR.equals(key)) {
                MaterialToast.showToast(R.string.restart_to_take_effect);
                mThemeColor.setEnabled(!(Boolean)newValue);
            }

            return true;
        }
    }

    public static class EhFragment extends TranslucentPreferenceFragment
            implements Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {

        private static final int[] EXCULDE_TAG_GROUP_RESID = {
            R.id.tag_group_reclass,
            R.id.tag_group_language,
            R.id.tag_group_parody,
            R.id.tag_group_character,
            R.id.tag_group_group,
            R.id.tag_group_artist,
            R.id.tag_group_male,
            R.id.tag_group_female
        };

        private static final int[] EXCULDE_TAG_GROUP_ID = {
            0x1, 0x2, 0x4, 0x8, 0x10, 0x20, 0x40, 0x80
        };

        private static final int[] EXCULDE_LANGUAGE_RESID = {
            R.id.el_japanese_translated, R.id.el_japanese_rewrite,
            R.id.el_english_original, R.id.el_english_translated, R.id.el_english_rewrite,
            R.id.el_chinese_original, R.id.el_chinese_translated, R.id.el_chinese_rewrite,
            R.id.el_dutch_original, R.id.el_dutch_translated, R.id.el_dutch_rewrite,
            R.id.el_french_original, R.id.el_french_translated, R.id.el_french_rewrite,
            R.id.el_german_original, R.id.el_german_translated, R.id.el_german_rewrite,
            R.id.el_hungarian_original, R.id.el_hungarian_translated, R.id.el_hungarian_rewrite,
            R.id.el_italian_original, R.id.el_italian_translated, R.id.el_italian_rewrite,
            R.id.el_korean_original, R.id.el_korean_translated, R.id.el_korean_rewrite,
            R.id.el_polish_original, R.id.el_polish_translated, R.id.el_polish_rewrite,
            R.id.el_portuguese_original, R.id.el_portuguese_translated, R.id.el_portuguese_rewrite,
            R.id.el_russian_original, R.id.el_russian_translated, R.id.el_russian_rewrite,
            R.id.el_spanish_original, R.id.el_spanish_translated, R.id.el_spanish_rewrite,
            R.id.el_thai_original, R.id.el_thai_translated, R.id.el_thai_rewrite,
            R.id.el_vietnamese_original, R.id.el_vietnamese_translated, R.id.el_vietnamese_rewrite,
            R.id.el_other_original, R.id.el_other_translated, R.id.el_other_rewrite,
        };

        private static final String[] EXCULDE_LANGUAGE_ID = {
            "1024", "2048", "1", "1025", "2049", "10", "1034", "2058", "20", "1044", "2068",
            "30", "1054", "2078", "40", "1064", "2088", "50", "1074", "2098",
            "60", "1084", "2108", "70", "1094", "2118", "80", "1104", "2128",
            "90", "1114", "2138", "100", "1124", "2148", "110", "1134", "2158",
            "120", "1144", "2168", "130", "1154", "2178", "255", "1279", "2303"
        };

        private static final String KEY_LIST_DEFAULT_CATEGORY = "list_default_category";
        private static final String KEY_EXCULDE_TAG_GROUP = "exculde_tag_group";
        private static final String KEY_EXCULDE_LANGUAGE = "exculde_language";
        private static final String KEY_PREVIEW_MODE = "preview_mode";

        private Preference mListDefaultCategory;
        private Preference mExculdeTagGroup;
        private Preference mExculdeLanguage;
        private AutoListPreference mPreviewMode;

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
            mPreviewMode = (AutoListPreference)findPreference(KEY_PREVIEW_MODE);
            mPreviewMode.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String key = preference.getKey();
            if (KEY_PREVIEW_MODE.equals(key)) {
                String newPreviewMode = (String)newValue;
                EhInfo.getInstance(mActivity).setPreviewMode(newPreviewMode);
            }
            return true;
        }

        @SuppressLint("InflateParams")
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_LIST_DEFAULT_CATEGORY.equals(key)) {
                int defaultCat = Config.getDefaultCat();
                final CategoryTable ct = new CategoryTable(mActivity);
                ct.setCategory(defaultCat);

                new DialogBuilder(mActivity)
                .setTitle(R.string.list_default_category_title)
                .setView(ct, 12).setSimpleNegativeButton()
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        int defaultCat = ct.getCategory();
                        Config.setDefaultCat(defaultCat);
                        EhInfo.getInstance(mActivity).setDefaultCat(defaultCat);
                    }
                }).create().show();

            } else if (KEY_EXCULDE_TAG_GROUP.equals(key)) {
                LayoutInflater inflater = LayoutInflater.from(mActivity);
                final TableLayout tl = (TableLayout)inflater.inflate(R.layout.exculde_tag_group, null);
                setExculdeTagGroup(tl, Config.getExculdeTagGroup());

                new DialogBuilder(mActivity)
                .setTitle(R.string.exculde_tag_group_title)
                .setView(tl, 12).setSimpleNegativeButton()
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        int newValue = getExculdeTagGroup(tl);
                        Config.setExculdeTagGroup(newValue);
                        EhInfo.getInstance(mActivity).setExculdeTagGroup(newValue);
                    }
                }).create().show();

            } else if (KEY_EXCULDE_LANGUAGE.equals(key)) {
                LayoutInflater inflater = LayoutInflater.from(mActivity);
                final TableLayout tl = (TableLayout)inflater.inflate(R.layout.exculde_language, null);
                setExculdeLanguage(tl, Config.getExculdeLanguage());

                new DialogBuilder(mActivity)
                .setTitle(R.string.exculde_language_title)
                .setView(tl, 12).setSimpleNegativeButton()
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        String newValue = getExculdeLanguage(tl);
                        Config.setExculdeLanguage(newValue);
                        EhInfo.getInstance(mActivity).setExculdeLanguage(newValue);
                    }
                }).create().show();
            }
            return true;
        }

        private static void setExculdeTagGroup(TableLayout tl, int value) {
            for (int i = 0; i < EXCULDE_TAG_GROUP_RESID.length; i++) {
                CheckBox cb = (CheckBox)tl.findViewById(EXCULDE_TAG_GROUP_RESID[i]);
                cb.setChecked(Utils.int2boolean(value & EXCULDE_TAG_GROUP_ID[i]));
            }
        }

        private static int getExculdeTagGroup(TableLayout tl) {
            int newValue = 0;
            for (int i = 0; i < EXCULDE_TAG_GROUP_RESID.length; i++) {
                CheckBox cb = (CheckBox)tl.findViewById(EXCULDE_TAG_GROUP_RESID[i]);
                if (cb.isChecked()) newValue |= EXCULDE_TAG_GROUP_ID[i];
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
                if (resId != 0) ((CheckBox)tl.findViewById(resId)).setChecked(true);
            }
        }

        private static String getExculdeLanguage(TableLayout tl) {
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;
            for (int i = 0; i < EXCULDE_LANGUAGE_RESID.length; i++) {
                if (((CheckBox)tl.findViewById(EXCULDE_LANGUAGE_RESID[i])).isChecked()) {
                    if (isFirst) isFirst = false; else sb.append('x');
                    sb.append(EXCULDE_LANGUAGE_ID[i]);
                }
            }
            return sb.toString();
        }
    }

    public static class DataFragment extends TranslucentPreferenceFragment
            implements Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {

        private static final String KEY_CACHE_SIZE = "cache_size";
        private static final String KEY_CLEAR_CACHE = "clear_cache";
        private static final String KEY_DOWNLOAD_PATH = "download_path";
        private static final String KEY_MEDIA_SCAN = "media_scan";
        private static final String KEY_DEFAULT_FAVORITE = "default_favorite";

        private AlertDialog mDirSelectDialog;

        private Preference mDownloadPath;
        private CheckBoxPreference mMediaScan;
        private ListPreference mDefaultFavorite;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.data_settings);

            mDownloadPath = findPreference(KEY_DOWNLOAD_PATH);
            mDownloadPath.setOnPreferenceClickListener(this);
            mMediaScan = (CheckBoxPreference)findPreference(KEY_MEDIA_SCAN);
            mMediaScan.setOnPreferenceChangeListener(this);
            mDefaultFavorite = (ListPreference)findPreference(KEY_DEFAULT_FAVORITE);

            mDownloadPath.setSummary(Config.getDownloadPath());

            int i = 0;
            String[] entrise = new String[Favorite.FAVORITE_TITLES.length + 1];
            entrise[i++] = getString(R.string.none);
            for (String str : Favorite.FAVORITE_TITLES)
                entrise[i++] = str;
            mDefaultFavorite.setEntries(entrise);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object objValue) {
            final String key = preference.getKey();
            if (KEY_MEDIA_SCAN.equals(key)) {
                boolean value = (Boolean)objValue;
                File nomedia = new File(Config.getDownloadPath(), ".nomedia");
                if (value) {
                    nomedia.delete();
                } else {
                    try {
                        nomedia.createNewFile();
                    } catch (IOException e) {}
                }
            }

            return true;
        }

        @SuppressLint("InflateParams")
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_DOWNLOAD_PATH.equals(key)) {
                View view = LayoutInflater.from(mActivity)
                        .inflate(R.layout.dir_selection, null);
                final FileExplorerView fileExplorerView =
                        (FileExplorerView)view.findViewById(R.id.file_list);
                final TextView warning =
                        (TextView)view.findViewById(R.id.warning);

                String downloadPath = Config.getDownloadPath();
                DialogBuilder dialogBuilder = new DialogBuilder(mActivity)
                .setView(view, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, Ui.dp2pix(360)), false)
                .setTitle(downloadPath)
                .setAction(R.drawable.ic_action_new_folder,
                        new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        final EditText et = new EditText(mActivity);
                        et.setText("New folder");
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        int x = Ui.dp2pix(8);
                        lp.leftMargin = x;
                        lp.rightMargin = x;
                        lp.topMargin = x;
                        lp.bottomMargin = x;
                        new DialogBuilder(mActivity).setView(et, lp)
                        .setTitle(R.string.new_folder)
                        .setPositiveButton(R.string._new, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((AlertButton)v).dialog.dismiss();
                                File dir = new File(fileExplorerView.getCurPath(),
                                        et.getText().toString());
                                dir.mkdirs();
                                fileExplorerView.refresh();
                                // TODO check if the directory was created
                            }
                        }).setSimpleNegativeButton().create().show();
                    }
                }).setPositiveButton(android.R.string.ok,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!fileExplorerView.canWrite())
                            MaterialToast.showToast(R.string.cur_dir_not_writable);
                        else {
                            String downloadPath = fileExplorerView.getCurPath();

                            // Update .nomedia file
                            // TODO Should I delete .nomedia in old download dir ?
                            if (!Config.getMediaScan()) {
                                try {
                                    new File(Config.getDownloadPath(), ".nomedia").createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            Config.setDownloadPath(downloadPath);
                            mDownloadPath.setSummary(downloadPath);
                            ((AlertButton)v).dialog.dismiss();
                        }
                    }
                }).setSimpleNegativeButton();
                final TextView title = dialogBuilder.getTitleView();

                if (fileExplorerView.canWrite())
                    warning.setVisibility(View.GONE);
                else
                    warning.setVisibility(View.VISIBLE);

                fileExplorerView.setPath(downloadPath);
                fileExplorerView.setOnItemClickListener(
                        new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        fileExplorerView.onItemClick(parent, view, position, id);
                        title.setText(fileExplorerView.getCurPath());
                        if (fileExplorerView.canWrite())
                            warning.setVisibility(View.GONE);
                        else
                            warning.setVisibility(View.VISIBLE);
                    }
                });

                mDirSelectDialog = dialogBuilder.create();
                mDirSelectDialog.show();
            }

            return true;
        }
    }

    public static class AboutFragment extends TranslucentPreferenceFragment
            implements Preference.OnPreferenceClickListener,
            Preference.OnPreferenceChangeListener {

        private static final String KEY_AUTHOR = "author";
        private static final String KEY_CHANGELOG = "changelog";
        private static final String KEY_THANKS = "thanks";
        private static final String KEY_WEBSITE = "website";
        private static final String KEY_SOURCE = "source";
        private static final String KEY_CHECK_UPDATE = "check_for_update";
        private static final String KEY_ALLOW_ANALYICS = "allow_analyics";
        private static final String KEY_ABOUT_ANALYICS = "about_analyics";

        private Preference mAuthor;
        private Preference mChangelog;
        private Preference mThanks;
        private Preference mWebsite;
        private Preference mSource;
        private Preference mCheckUpdate;
        private CheckBoxPreference mAllowAnalyics;
        private Preference mAboutAnalyics;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.about_settings);

            mAuthor = findPreference(KEY_AUTHOR);
            mAuthor.setOnPreferenceClickListener(this);
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
            mAllowAnalyics = (CheckBoxPreference)findPreference(KEY_ALLOW_ANALYICS);
            mAllowAnalyics.setOnPreferenceChangeListener(this);
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

            } else if (KEY_CHANGELOG.equals(key)) {
                InputStream is = mActivity.getResources()
                        .openRawResource(R.raw.change_log);
                new DialogBuilder(mActivity).setTitle(R.string.changelog)
                        .setLongMessage(Utils.InputStream2String(is, "utf-8"))
                        .setSimpleNegativeButton().create().show();

            } else if (KEY_THANKS.equals(key)) {
                InputStream is = mActivity.getResources()
                        .openRawResource(R.raw.thanks);
                final WebView webView = new WebView(mActivity);
                webView.loadData(Utils.InputStream2String(is, "utf-8"), "text/html; charset=UTF-8", null);
                new DialogBuilder(mActivity).setTitle(R.string.thanks)
                        .setView(webView, false).setSimpleNegativeButton()
                        .create().show();

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
                new UpdateHelper((AppContext)mActivity.getApplication())
                        .SetOnCheckUpdateListener(new UpdateHelper.OnCheckUpdateListener() {
                            @Override
                            public void onSuccess(String version, long size,
                                    final String url, final String fileName, String info) {
                                mCheckUpdate.setSummary(R.string.found_update);
                                String sizeStr = Utils.sizeToString(size);
                                AlertDialog dialog = SuperDialogUtil.createUpdateDialog(
                                        mActivity, version, sizeStr, info,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                ((AlertButton)v).dialog.dismiss();
                                                // TODO
                                                HttpHelper hh = new HttpHelper(mActivity);
                                                hh.downloadInThread(url, new File(Config.getDownloadPath()), fileName, false, null,
                                                        new UpdateHelper.UpdateListener(mActivity, fileName));
                                                mCheckUpdate.setEnabled(true);
                                            }
                                        });
                                if (!mActivity.isFinishing())
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

            } else if (KEY_ABOUT_ANALYICS.equals(key)) {
                new DialogBuilder(mActivity).setTitle(R.string.about_analyics_title)
                .setMessageAutoLink(Linkify.WEB_URLS)
                .setLongMessage(R.string.about_analyics_comment)
                .setSimpleNegativeButton()
                .create().show();
            }
            return true;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String key = preference.getKey();
            if (KEY_ALLOW_ANALYICS.equals(key)) {
                if (!(Boolean)newValue)
                    Config.setPopularWarning(true);
            }
            return true;
        }
    }
}
