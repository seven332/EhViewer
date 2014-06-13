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
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FileExplorerView;
import com.hippo.ehviewer.widget.SuperDialogUtil;
import com.hippo.ehviewer.widget.SuperToast;
import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.preference.AutoListPreference;
import com.readystatesoftware.systembartint.SystemBarTintManager.SystemBarConfig;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsActivity extends AbstractPreferenceActivity {
    @SuppressWarnings("unused")
    private static String TAG = "Settings";
    
    private SystemBarConfig mSystemBarConfig;
    private List<TranslucentPreferenceFragment> mFragments;
    private ListView mListView;
    private int originPaddingRight;
    private int originPaddingBottom;
    
    public void adjustPadding() {
        if (mListView != null && mSystemBarConfig != null) {
            
            switch (Ui.getOrientation(this)) {
            case Ui.ORIENTATION_PORTRAIT:
                
                mListView.setPadding(mListView.getPaddingLeft(),
                        mSystemBarConfig.getStatusBarHeight() + mSystemBarConfig.getActionBarHeight(),
                        originPaddingRight,
                        mSystemBarConfig.getNavigationBarHeight());
                break;
            case Ui.ORIENTATION_LANDSCAPE:
                
                mListView.setPadding(mListView.getPaddingLeft(),
                        mSystemBarConfig.getStatusBarHeight() + mSystemBarConfig.getActionBarHeight(),
                        originPaddingRight + mSystemBarConfig.getNavigationBarWidth(),
                        originPaddingBottom);
                break;
            }
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        for (TranslucentPreferenceFragment f : mFragments) {
            if (f.isVisible()) {
                f.adjustPadding();
                return;
            }
        }
        
        // If no fragment is visible, just headers is shown
        adjustPadding();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set random color
        int color = Config.getRandomThemeColor() ? Theme.getRandomDeepColor() : Config.getThemeColor();
        color = color & 0x00ffffff | 0xdd000000;
        Drawable drawable = new ColorDrawable(color);
        final ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(drawable);
        mSystemBarConfig = Ui.translucent(this, color);
        
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mFragments = new LinkedList<TranslucentPreferenceFragment>();
        
        mListView = getListView();
        mListView.setClipToPadding(false);
        originPaddingRight = mListView.getPaddingRight();
        originPaddingBottom = mListView.getPaddingBottom();
        
        adjustPadding();
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
    
    public SystemBarConfig getSystemBarConfig() {
        return mSystemBarConfig;
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
        DataFragment.class.getName(),
        AboutFragment.class.getName()
    };
    
    private static final int[] FRAGMENT_ICONS = {
        R.drawable.ic_setting_display,
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
        private SystemBarConfig mSystemBarConfig;
        private ListView mListView;
        private int originPaddingRight;
        private int originPaddingBottom;
        
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
                originPaddingRight = mListView.getPaddingRight();
                originPaddingBottom = mListView.getPaddingBottom();
                mSystemBarConfig = activity.getSystemBarConfig();
                adjustPadding();
            }
        }
        
        public void adjustPadding() {
            if (mListView != null && mSystemBarConfig != null) {
                switch (Ui.getOrientation(getActivity())) {
                case Ui.ORIENTATION_PORTRAIT:
                    mListView.setPadding(mListView.getPaddingLeft(),
                            mSystemBarConfig.getStatusBarHeight() + mSystemBarConfig.getActionBarHeight(),
                            originPaddingRight,
                            mSystemBarConfig.getNavigationBarHeight());
                    break;
                    
                case Ui.ORIENTATION_LANDSCAPE:
                    mListView.setPadding(mListView.getPaddingLeft(),
                            mSystemBarConfig.getStatusBarHeight() + mSystemBarConfig.getActionBarHeight(),
                            originPaddingRight + mSystemBarConfig.getNavigationBarWidth(),
                            originPaddingBottom);
                    break;
                }
            }
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
            mThemeColor = (Preference)findPreference(KEY_THEME_COLOR);
            
            mThemeColor.setEnabled(!mRandomThemeColor.isChecked());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String key = preference.getKey();
            if (KEY_SCREEN_ORIENTATION.equals(key)) {
                mActivity.setRequestedOrientation(
                        Config.screenOriPre2Value(Integer.parseInt((String) newValue)));
                
            } else if (KEY_RANDOM_THEME_COLOR.equals(key)) {
                new SuperToast(mActivity, R.string.restart_to_take_effect).show();
                mThemeColor.setEnabled(!(Boolean)newValue);
            }
            
            return true;
        }
    }
    
    public static class DataFragment extends TranslucentPreferenceFragment
            implements Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {
        
        private static final String KEY_CACHE_SIZE = "cache_size";
        private static final String KEY_CLEAR_CACHE = "clear_cache";
        private static final String KEY_DOWNLOAD_PATH = "download_path";
        private static final String KEY_MEDIA_SCAN = "media_scan";
        
        private AlertDialog mDirSelectDialog;
        
        private EditTextPreference mCacheSize;
        private Preference mClearCache;
        private Preference mDownloadPath;
        private CheckBoxPreference mMediaScan;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.data_settings);
            
            mCacheSize = (EditTextPreference)findPreference(KEY_CACHE_SIZE);
            mCacheSize.setOnPreferenceChangeListener(this);
            mClearCache = (Preference)findPreference(KEY_CLEAR_CACHE);
            mClearCache.setOnPreferenceClickListener(this);
            mDownloadPath = (Preference)findPreference(KEY_DOWNLOAD_PATH);
            mDownloadPath.setOnPreferenceClickListener(this);
            mMediaScan = (CheckBoxPreference)findPreference(KEY_MEDIA_SCAN);
            mMediaScan.setOnPreferenceChangeListener(this);
            
            // Set summary
            updateClearCacheSummary();
            mDownloadPath.setSummary(Config.getDownloadPath());
        }
        
        private void updateClearCacheSummary() {
            if (Cache.diskCache != null) {
                mClearCache.setSummary(String.format(
                        getString(R.string.clear_cache_summary_on),
                        Cache.diskCache.size() / 1024 / 1024f,
                        Cache.diskCache.maxSize() / 1024 / 1024));
                mClearCache.setEnabled(true);
            } else {
                mClearCache
                        .setSummary(getString(R.string.clear_cache_summary_off));
                mClearCache.setEnabled(false);
            }
        }
        
        @Override
        public boolean onPreferenceChange(Preference preference, Object objValue) {
            final String key = preference.getKey();
            if (KEY_CACHE_SIZE.equals(key)) {
                long cacheSize = 0;
                try {
                    cacheSize = Integer.parseInt((String) objValue) * 1024 * 1024;
                } catch (Exception e) {
                    new SuperToast(mActivity).setIcon(R.drawable.ic_warning)
                            .setMessage(R.string.input_error).show();
                    return false;
                }
                
                if (cacheSize <= 0 && Cache.diskCache != null) {
                    Cache.diskCache.clear();
                    Cache.diskCache.close();
                    Cache.diskCache = null;
                } else if (Cache.diskCache == null) {
                    try {
                        Cache.diskCache = new DiskCache(mActivity,
                                Cache.cpCachePath, cacheSize);
                    } catch (Exception e) {
                        new SuperToast(mActivity).setIcon(R.drawable.ic_warning)
                                .setMessage(R.string.create_cache_error).show();
                        e.printStackTrace();
                    }
                } else
                    Cache.diskCache.setMaxSize(cacheSize);
                updateClearCacheSummary();
                
            } else if (KEY_MEDIA_SCAN.equals(key)) {
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

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_CLEAR_CACHE.equals(key)) {
                Cache.diskCache.clear();
                updateClearCacheSummary();
                
            } else if (KEY_DOWNLOAD_PATH.equals(key)) {
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
                            new SuperToast(mActivity).setIcon(R.drawable.ic_warning)
                                    .setMessage(R.string.cur_dir_not_writable).show();
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
            implements Preference.OnPreferenceClickListener{
        
        private static final String KEY_AUTHOR = "author";
        private static final String KEY_CHANGELOG = "changelog";
        private static final String KEY_THANKS = "thanks";
        private static final String KEY_WEBSITE = "website";
        private static final String KEY_SOURCE = "source";
        private static final String KEY_CHECK_UPDATE = "check_for_update";
        
        private Preference mAuthor;
        private Preference mChangelog;
        private Preference mThanks;
        private Preference mWebsite;
        private Preference mSource;
        private Preference mCheckUpdate;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.about_settings);
            
            mAuthor = (Preference)findPreference(KEY_AUTHOR);
            mAuthor.setOnPreferenceClickListener(this);
            mChangelog = (Preference)findPreference(KEY_CHANGELOG);
            mChangelog.setOnPreferenceClickListener(this);
            mThanks = (Preference)findPreference(KEY_THANKS);
            mThanks.setOnPreferenceClickListener(this);
            mWebsite = (Preference)findPreference(KEY_WEBSITE);
            mWebsite.setOnPreferenceClickListener(this);
            mSource = (Preference)findPreference(KEY_SOURCE);
            mSource.setOnPreferenceClickListener(this);
            mCheckUpdate = (Preference)findPreference(KEY_CHECK_UPDATE);
            mCheckUpdate.setOnPreferenceClickListener(this);
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
                        .setLongMessage(Util.InputStream2String(is, "utf-8"))
                        .setSimpleNegativeButton().create().show();
                
            } else if (KEY_THANKS.equals(key)) {
                InputStream is = mActivity.getResources()
                        .openRawResource(R.raw.thanks);
                final WebView webView = new WebView(mActivity);
                webView.loadData(Util.InputStream2String(is, "utf-8"), "text/html; charset=UTF-8", null);
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
                                String sizeStr = Util.sizeToString(size);
                                AlertDialog dialog = SuperDialogUtil.createUpdateDialog(
                                        mActivity, version, sizeStr, info,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                ((AlertButton)v).dialog.dismiss();
                                                // TODO
                                                try {
                                                    Downloader downloader = new Downloader(mActivity);
                                                    downloader.resetData(Config.getDownloadPath(), fileName, url);
                                                    downloader.setOnDownloadListener(
                                                            new UpdateHelper.UpdateListener(mActivity,
                                                                    fileName));
                                                    new Thread(downloader).start();
                                                } catch (MalformedURLException e) {
                                                    mCheckUpdate.setSummary(R.string.em_url_format_error);
                                                    UpdateHelper.setEnabled(true);
                                                }
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
            }
            return true;
        }
    }
}
