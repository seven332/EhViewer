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

package com.hippo.ehviewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.ui.GalleryListActivity;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;

public class StartActivity extends Activity {

    @SuppressWarnings("unused")
    private static final String TAG = StartActivity.class.getSimpleName();

    private static final int NETWORK_STATE_NONE = 0x0;
    private static final int NETWORK_STATE_MOBILE = 0x1;
    private static final int NETWORK_STATE_WIFI = 0x2;

    private static final int CHECK_WARING = 0;
    private static final int CHECK_ANALYTICS = 1;
    private static final int CHECK_NETWORK = 2;
    private static final int CHECK_CRASH = 3;

    private String lastCrash;
    private boolean isAnimationOver = false;
    private boolean isCheckOver = false;

    private MaterialAlertDialog createWarningDialog() {
        return new MaterialAlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.dailog_waring_title)
                .setMessage(R.string.dailog_waring_plain)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(android.R.string.cancel)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        switch (which) {
                        case MaterialAlertDialog.POSITIVE:
                            Config.allowed();
                            check(CHECK_ANALYTICS);
                            break;
                        case MaterialAlertDialog.NEGATIVE:
                            finish();
                            break;
                        }
                        return true;
                    }
                }).create();
    }

    private AlertDialog createAllowAnalyicsDialog() {
        return new MaterialAlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.dailog_analyics_title)
                .setMessageAutoLink(Linkify.WEB_URLS)
                .setMessage(R.string.dailog_analyics_plain)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(android.R.string.cancel)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        switch (which) {
                        case MaterialAlertDialog.POSITIVE:
                            Config.setAllowAnalyics(true);
                            check(CHECK_NETWORK);
                            break;
                        case MaterialAlertDialog.NEGATIVE:
                            Config.setAllowAnalyics(false);
                            check(CHECK_NETWORK);
                            break;
                        }
                        return true;
                    }
                }).create();
    }

    private AlertDialog createNetworkDialog(int state) {
        int mesgId;
        switch (state) {
        case NETWORK_STATE_MOBILE:
            mesgId = R.string.dailog_network_mobile_title;
            break;
        case NETWORK_STATE_NONE:
        default:
            mesgId = R.string.dailog_network_none_title;
            break;
        }

        return new MaterialAlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.warning).setMessage(mesgId)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(android.R.string.cancel)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        switch (which) {
                        case MaterialAlertDialog.POSITIVE:
                            check(CHECK_CRASH);
                            break;
                        case MaterialAlertDialog.NEGATIVE:
                            finish();
                            break;
                        }
                        return true;
                    }
                }).create();
    }

    private AlertDialog createSendCrashDialog() {
        return new MaterialAlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.dialog_send_crash_title)
                .setMessage(R.string.dialog_send_crash_plain)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(android.R.string.cancel)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        switch (which) {
                        case MaterialAlertDialog.POSITIVE:
                            // A wait dialog
                            new MaterialAlertDialog.Builder(StartActivity.this)
                                    .setCancelable(false)
                                    .setTitle(R.string.wait)
                                    .setMessage(
                                            R.string.dialog_wait_send_crash_msg)
                                    .setPositiveButton(android.R.string.ok)
                                    .setButtonListener(
                                            new MaterialAlertDialog.OnClickListener() {
                                                @Override
                                                public boolean onClick(
                                                        MaterialAlertDialog dialog,
                                                        int which) {
                                                    checkOver();
                                                    return true;
                                                }
                                            }).show();
                            Intent i = new Intent(Intent.ACTION_SENDTO);
                            i.setData(Uri.parse("mailto:ehviewersu@gmail.com"));
                            i.putExtra(Intent.EXTRA_SUBJECT,
                                    "I found a bug in EhViewer !");
                            i.putExtra(Intent.EXTRA_TEXT, lastCrash);
                            startActivity(i);
                            lastCrash = null;
                            break;
                        case MaterialAlertDialog.NEGATIVE:
                            lastCrash = null;
                            checkOver();
                            break;
                        }
                        return true;
                    }
                }).create();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("InlinedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View wellcome = new View(this);
        wellcome.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.welcome));
        setContentView(wellcome);

        // For fullscreen
        if (Build.VERSION.SDK_INT >= 19) {
            wellcome.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        // Show welcome in progress
        AlphaAnimation aa = new AlphaAnimation(0.3f, 1.0f);
        aa.setDuration(2000);
        aa.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                if (isCheckOver)
                    redirectTo();
                isAnimationOver = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
        wellcome.startAnimation(aa);
        check(CHECK_WARING);
    }

    /**
     * Order is 1. check waring 2. check analyics 3. check network 4. check
     * crash 5. check external storage
     *
     * @param order
     */
    private void check(int order) {
        switch (order) {
        case CHECK_WARING:
            if (!Config.isAllowed()) {
                createWarningDialog().show();
                return;
            }
        case CHECK_ANALYTICS:
            if (!Config.getSetAnalyics()) {
                Config.setSetAnalyics(true);
                createAllowAnalyicsDialog().show();
                return;
            }
        case CHECK_NETWORK:
            int state = getNetworkState();
            if (state != NETWORK_STATE_WIFI) {
                createNetworkDialog(state).show();
                return;
            }
        case CHECK_CRASH:
            if ((lastCrash = Crash.getLastCrash()) != null) {
                createSendCrashDialog().show();
                return;
            }
        }
        checkOver();
    }

    private void checkOver() {
        if (isAnimationOver)
            redirectTo();
        isCheckOver = true;
    }

    private int getNetworkState() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMobile = cm
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (mWifi != null && mWifi.isConnected())
            return NETWORK_STATE_WIFI;
        else if (mMobile != null && mMobile.isConnected())
            return NETWORK_STATE_MOBILE;
        else
            return NETWORK_STATE_NONE;
    }

    private void redirectTo() {
        Intent intent = new Intent(this, GalleryListActivity.class);
        startActivity(intent);
        finish();
    }
}
