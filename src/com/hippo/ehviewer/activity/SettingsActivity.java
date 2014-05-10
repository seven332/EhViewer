package com.hippo.ehviewer.activity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.SuperDialogUtil;
import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.preference.AutoListPreference;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v4.util.LruCache;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.Toast;


// TODO 添加服务器选择

public class SettingsActivity extends PreferenceActivity {
    @SuppressWarnings("unused")
    private static String TAG = "Settings";

    private Context mContext;

    public LruCache<String, Bitmap> memoryCache;

    private static final String CACHE = "preference_cache";
    private static final String CLEAR_CACHE = "preference_clear_cache";
    private static final String AUTHOR = "preference_author";
    private static final String SCREEN_ORI = "preference_screen_ori";
    private static final String CHANGELOG = "preference_changelog";
    private static final String THANKS = "preference_thanks";
    private static final String UPDATE = "preference_update";
    private static final String RAN = "preference_remove_all_noification";
    private static final String MEDIA_SCAN = "preference_media_scan";

    private EditTextPreference cachePre;
    private Preference clearCachePre;
    private Preference authorPer;
    private AutoListPreference screenOriPer;
    private Preference changelogPer;
    private Preference thanksPer;
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
    }
    
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        mContext = getApplicationContext();
        
        // Tint
        ListView listView = getListView();
        listView.setFitsSystemWindows(true);
        listView.setClipToPadding(false);
        // TODO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
            winParams.flags |= bits;
            win.setAttributes(winParams);
        }
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(android.R.color.holo_blue_dark);
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setNavigationBarAlpha(0.0f);
        
        
        // Set PreferenceActivity
        PreferenceScreen screen = getPreferenceScreen();

        cachePre = (EditTextPreference) screen.findPreference(CACHE);
        cachePre
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        long cpCacheSize = 0;
                        try {
                            int cpCacheSizeInMB = Integer
                                    .parseInt((String) newValue);
                            if (cpCacheSizeInMB > 1024) {
                                Toast.makeText(mContext,
                                        getString(R.string.cache_large),
                                        Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            cpCacheSize = cpCacheSizeInMB * 1024 * 1024;
                        } catch (Exception e) {
                            Toast.makeText(mContext,
                                    getString(R.string.input_error),
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (cpCacheSize <= 0 && Cache.cpCache != null) {
                            Cache.cpCache.clear();
                            Cache.cpCache.close();
                            Cache.cpCache = null;
                        } else if (Cache.cpCache == null) {
                            try {
                                Cache.cpCache = new DiskCache(mContext,
                                        Cache.cpCachePath, cpCacheSize);
                            } catch (Exception e) {
                                Toast.makeText(mContext,
                                        getString(R.string.create_cache_error),
                                        Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        } else
                            Cache.cpCache.setMaxSize(cpCacheSize);
                        updateCpCacheSummary();
                        return true;
                    }
                });

        clearCachePre = (Preference) screen.findPreference(CLEAR_CACHE);
        updateCpCacheSummary();
        clearCachePre
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Cache.cpCache.clear();
                        updateCpCacheSummary();
                        return true;
                    }
                });
        
        
        // Connect me !
        authorPer = screen.findPreference(AUTHOR);
        authorPer.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_SENDTO);
                i.setData(Uri.parse("mailto:ehviewersu@gmail.com"));
                i.putExtra(Intent.EXTRA_SUBJECT, "About EhViewer");
                startActivity(i);
                return true;
            }
        });
        
        // Screen Orientation
        screenOriPer = (AutoListPreference) screen.findPreference(SCREEN_ORI);
        screenOriPer
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                setRequestedOrientation(Config.screenOriPre2Value(Integer.parseInt((String) newValue)));
                return true;
            }
        });
        
        // Changelog
        changelogPer = (Preference) screen.findPreference(CHANGELOG);
        changelogPer.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Get changelog string
                InputStream is = SettingsActivity.this.getResources()
                        .openRawResource(R.raw.change_log);
                new DialogBuilder(SettingsActivity.this).setTitle(R.string.changelog)
                        .setLongMessage(Util.InputStream2String(is, "utf-8")).create().show();
                return true;
            }
        });
        
        // Thanks
        thanksPer = (Preference) screen.findPreference(THANKS);
        thanksPer.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                InputStream is = SettingsActivity.this.getResources()
                        .openRawResource(R.raw.thanks);
                final WebView webView = new WebView(SettingsActivity.this);
                webView.loadData(Util.InputStream2String(is, "utf-8"), "text/html; charset=UTF-8", null);
                new DialogBuilder(SettingsActivity.this).setTitle(R.string.thanks)
                        .setView(webView, false).create().show();
                return true;
            }
        });
        
        final Preference update = (Preference)screen.findPreference(UPDATE);
        update.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                update.setSummary(R.string.checking_update);
                new UpdateHelper((AppContext)SettingsActivity.this.getApplication())
                        .SetOnCheckUpdateListener(new UpdateHelper.OnCheckUpdateListener() {
                            @Override
                            public void onSuccess(String version, long size,
                                    final String url, final String fileName, String info) {
                                update.setSummary(R.string.found_update);
                                
                                String sizeStr = Util.sizeToString(size);
                                AlertDialog dialog = SuperDialogUtil.createUpdateDialog(SettingsActivity.this,
                                        version, sizeStr, info,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                ((AlertButton)v).dialog.dismiss();
                                                // TODO
                                                try {
                                                    Downloader downloader = new Downloader(SettingsActivity.this);
                                                    downloader.resetData(Config.getDownloadPath(), fileName, url);
                                                    downloader.setOnDownloadListener(
                                                            new UpdateHelper.UpdateListener(SettingsActivity.this,
                                                                    fileName));
                                                    new Thread(downloader).start();
                                                } catch (MalformedURLException e) {
                                                    update.setSummary(R.string.em_url_format_error);
                                                    UpdateHelper.setEnabled(true);
                                                }
                                                update.setEnabled(true);
                                            }
                                        }, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                ((AlertButton)v).dialog.dismiss();
                                                update.setEnabled(true);
                                                UpdateHelper.setEnabled(true);
                                            }
                                        });
                                if (!SettingsActivity.this.isFinishing())
                                    dialog.show();
                            }
                            @Override
                            public void onNoUpdate() {
                                update.setSummary(R.string.up_to_date);
                                update.setEnabled(true);
                                UpdateHelper.setEnabled(true);
                            }
                            @Override
                            public void onFailure(String eMsg) {
                                update.setSummary(eMsg);
                                update.setEnabled(true);
                                UpdateHelper.setEnabled(true);
                            }
                        }).checkUpdate();
                
                update.setEnabled(false);
                return true;
            }
        });
        
        Preference ran = (Preference)screen.findPreference(RAN);
        ran.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                NotificationManager mNotifyManager = (NotificationManager)
                        getSystemService(Context.NOTIFICATION_SERVICE);
                mNotifyManager.cancelAll();
                return true;
            }
        });
        
        CheckBoxPreference mediaScan = (CheckBoxPreference)screen.findPreference(MEDIA_SCAN);
        mediaScan.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                boolean value = (Boolean)newValue;
                File nomedia = new File(Config.getDownloadPath(), ".nomedia");
                if (value) {
                    nomedia.delete();
                } else {
                    try {
                        nomedia.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
    }
    
    private void updateCpCacheSummary() {
        if (Cache.cpCache != null) {
            clearCachePre.setSummary(String.format(
                    getString(R.string.preference_cache_summary),
                    Cache.cpCache.size() / 1024 / 1024f,
                    Cache.cpCache.maxSize() / 1024 / 1024));
            clearCachePre.setEnabled(true);
        } else {
            clearCachePre
                    .setSummary(getString(R.string.preference_cache_summary_no));
            clearCachePre.setEnabled(false);
        }
    }
}
