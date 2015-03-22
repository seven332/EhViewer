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

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.network.Network;
import com.hippo.ehviewer.ui.AbsActivity;
import com.hippo.ehviewer.ui.GalleryListActivity;
import com.hippo.ehviewer.util.AppUtils;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;
import com.hippo.ehviewer.util.Secret;
import com.hippo.ehviewer.util.ViewUtils;
import com.larvalabs.svgandroid.SVGBuilder;

public class StartActivity extends AbsActivity {

    @SuppressWarnings("unused")
    private static final String TAG = StartActivity.class.getSimpleName();

    private static final int CHECK_WARING = 0;
    private static final int CHECK_ANALYTICS = 1;
    private static final int CHECK_NETWORK = 2;
    private static final int CHECK_CRASH = 3;

    private static final String KEY_ALLOWED = "allowed";
    private static final String KEY_SET_ANALYTICS = "set_analyics";

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
                            Config.setBoolean(KEY_ALLOWED, true);
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
        case Network.NETWORK_STATE_MOBILE:
            mesgId = R.string.dailog_network_mobile_title;
            break;
        case Network.NETWORK_STATE_NONE:
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
        View view = ViewUtils.inflateDialogView(R.layout.send_crash, false);
        final EditText et = (EditText) view.findViewById(R.id.description_of_user);
        return new MaterialAlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.dialog_send_crash_title)
                .setView(view, true)
                .setDefaultButton(MaterialAlertDialog.POSITIVE | MaterialAlertDialog.NEGATIVE)
                .setButtonListener(new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int which) {
                        switch (which) {
                        case MaterialAlertDialog.POSITIVE:
                            // A wait dialog
                            new MaterialAlertDialog.Builder(StartActivity.this)
                                    .setCancelable(false)
                                    .setTitle(R.string.wait)
                                    .setMessage(R.string.dialog_wait_send_crash_msg)
                                    .setPositiveButton(android.R.string.ok)
                                    .setButtonListener(
                                            new MaterialAlertDialog.OnClickListener() {
                                                @Override
                                                public boolean onClick(MaterialAlertDialog dialog, int which) {
                                                    checkOver();
                                                    return true;
                                                }
                                            }).show();
                            AppUtils.sendEmail(StartActivity.this, "ehviewersu@gmail.com",
                                    "I found a bug in EhViewer !",
                                    lastCrash + "======== Description ========\n" + et.getText().toString());
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Disable animate
        overridePendingTransition(0, 0);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);

        TextView text = (TextView) findViewById(R.id.text);
        text.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/GloriaHallelujah.ttf"));
        ImageView image = (ImageView) findViewById(R.id.image);
        image.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        image.setImageDrawable(new SVGBuilder().readFromResource(getResources(), R.raw.sad_pandroid).build().getDrawable());

        // Update secret image here
        int state = Network.getNetworkState(this);
        if (state == Network.NETWORK_STATE_WIFI) {
            Secret.updateSecretImage(this);
        }

        AppHandler.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isCheckOver)
                    redirectTo();
                isAnimationOver = true;
            }
        }, 3000);
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
            if (!Config.getBoolean(KEY_ALLOWED, false)) {
                createWarningDialog().show();
                return;
            }
        case CHECK_ANALYTICS:
            if (!Config.getBoolean(KEY_SET_ANALYTICS, false)) {
                Config.setBoolean(KEY_SET_ANALYTICS, true);
                createAllowAnalyicsDialog().show();
                return;
            }
        case CHECK_NETWORK:
            int state = Network.getNetworkState(this);
            if (state != Network.NETWORK_STATE_WIFI) {
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

    private void redirectTo() {
        Intent intent = new Intent(this, GalleryListActivity.class);
        startActivity(intent);
        finish();
    }
}
