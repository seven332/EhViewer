package com.hippo.ehviewer.activity;

import java.io.InputStream;

import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UpdateHelper;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.view.MangaImage;
import com.hippo.ehviewer.dialog.DialogBuilder;
import com.hippo.ehviewer.preference.AutoListPreference;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v4.util.LruCache;
import android.webkit.WebView;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {
    @SuppressWarnings("unused")
    private static String TAG = "Settings";

    private Context mContext;

    public LruCache<String, Bitmap> memoryCache;

    private static final String CP_CACHE = "preference_cp_cache";
    private static final String CLEAR_CP_CACHE = "preference_clear_cp_cache";
    private static final String PAGE_CACHE = "preference_page_cache";
    //private static final String START_POSITION = "preference_start_position";
    private static final String CLEAR_PAGE_CACHE = "preference_clear_page_cache";
    private static final String PAGE_SCALING = "preference_page_scaling";
    private static final String AUTHOR = "preference_author";
    private static final String SCREEN_ORI = "preference_screen_ori";
    private static final String CHANGELOG = "preference_changelog";
    private static final String THANKS = "preference_thanks";
    private static final String UPDATE = "preference_update";
    private static final String RAN = "preference_remove_all_noification";

    private EditTextPreference cpCachePre;
    private Preference clearCpCachePre;
    private EditTextPreference pageCachePre;
    private Preference clearPageCachePre;
    private AutoListPreference pageScalingListPre;
    private Preference authorPer;
    private AutoListPreference screenOriPer;
    private Preference changelogPer;
    private Preference thanksPer;
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.fixColour(this);
        }
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

        // Colourfy Screen
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.ColourfyScreen(this);
            this.getListView().setFitsSystemWindows(true);
        }

        // Set PreferenceActivity
        PreferenceScreen screen = getPreferenceScreen();

        cpCachePre = (EditTextPreference) screen.findPreference(CP_CACHE);
        cpCachePre
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

        clearCpCachePre = (Preference) screen.findPreference(CLEAR_CP_CACHE);
        updateCpCacheSummary();
        clearCpCachePre
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Cache.cpCache.clear();
                        updateCpCacheSummary();
                        return true;
                    }
                });

        pageCachePre = (EditTextPreference) screen.findPreference(PAGE_CACHE);
        pageCachePre
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        int pageCacheSize = 0;
                        try {
                            int pageCacheSizeInMB = Integer
                                    .parseInt((String) newValue);
                            if (pageCacheSizeInMB > 1024) {
                                Toast.makeText(mContext,
                                        getString(R.string.cache_large),
                                        Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            pageCacheSize = pageCacheSizeInMB * 1024 * 1024;
                        } catch (Exception e) {
                            Toast.makeText(mContext,
                                    getString(R.string.input_error),
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (pageCacheSize <= 0 && Cache.pageCache != null) {
                            Cache.pageCache.clear();
                            Cache.pageCache.close();
                            Cache.pageCache = null;
                        } else if (Cache.pageCache == null) {
                            try {
                                Cache.pageCache = new DiskCache(
                                        SettingsActivity.this.getApplication()
                                                .getApplicationContext(),
                                        Cache.pageCachePath, pageCacheSize);
                            } catch (Exception e) {
                                Toast.makeText(mContext,
                                        getString(R.string.create_cache_error),
                                        Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        } else
                            Cache.pageCache.setMaxSize(pageCacheSize);
                        updatePageCacheSummary();
                        return true;
                    }
                });

        clearPageCachePre = (Preference) screen
                .findPreference(CLEAR_PAGE_CACHE);
        updatePageCacheSummary();
        clearPageCachePre
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Cache.pageCache.clear();
                        updatePageCacheSummary();
                        return true;
                    }
                });

        pageScalingListPre = (AutoListPreference) screen
                .findPreference(PAGE_SCALING);
        pageScalingListPre
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        MangaImage.setMode(Integer.parseInt((String) newValue));
                        return true;
                    }
                });
        
        /*
        final AutoListPreference start_position =
                (AutoListPreference)screen.findPreference(START_POSITION);
        start_position.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                // TODO Auto-generated method stub
                return false;
            }
        });*/
        
        
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
                new UpdateHelper(SettingsActivity.this)
                        .SetOnCheckUpdateListener(new UpdateHelper.OnCheckUpdateListener() {
                            @Override
                            public void onSuccess(String pageContext) {
                                update.setSummary(R.string.found_update);
                                update.setEnabled(true);
                            }
                            @Override
                            public void onNoUpdate() {
                                update.setSummary(R.string.up_to_date);
                                update.setEnabled(true);
                            }
                            @Override
                            public void onFailure(int eMesgId) {
                                update.setSummary(eMesgId);
                                update.setEnabled(true);
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
    }
    
    private void updateCpCacheSummary() {
        if (Cache.cpCache != null) {
            clearCpCachePre.setSummary(String.format(
                    getString(R.string.preference_cache_summary),
                    Cache.cpCache.size() / 1024 / 1024f,
                    Cache.cpCache.maxSize() / 1024 / 1024));
            clearCpCachePre.setEnabled(true);
        } else {
            clearCpCachePre
                    .setSummary(getString(R.string.preference_cache_summary_no));
            clearCpCachePre.setEnabled(false);
        }
    }

    private void updatePageCacheSummary() {
        if (Cache.pageCache != null) {
            clearPageCachePre.setSummary(String.format(
                    getString(R.string.preference_cache_summary),
                    Cache.pageCache.size() / 1024 / 1024f,
                    Cache.pageCache.maxSize() / 1024 / 1024));
            clearPageCachePre.setEnabled(true);
        } else {
            clearPageCachePre
                    .setSummary(getString(R.string.preference_cache_summary_no));
            clearPageCachePre.setEnabled(false);
        }
    }
}
