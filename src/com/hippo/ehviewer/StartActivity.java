package com.hippo.ehviewer;

import com.hippo.ehviewer.activity.MangaListActivity;
import com.hippo.ehviewer.dialog.DialogBuilder;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;
import com.hippo.ehviewer.view.AlertButton;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.LinearLayout;

public class StartActivity extends Activity {
    
    private static final int CHECK_WARING = 0;
    private static final int CHECK_CRESH = 1;
    private static final int CHECK_NETWORK = 2;
    
    private String lastCrash;
    private boolean isAnimationOver = false;
    private boolean isCheckOver = false;
    
    private AlertDialog createWarningDialog() {
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.dailog_waring_title)
                .setMessage(R.string.dailog_waring_plain)
                .setPositiveButton(R.string.dailog_waring_yes,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        Config.allowed();
                        
                        check(CHECK_CRESH);
                    }
                }).setNegativeButton(R.string.dailog_waring_no,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        finish();
                    }
                }).create();
    }
    
    private AlertDialog createSendCrashDialog() {
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.dialog_send_crash_title)
                .setMessage(R.string.dialog_send_crash_plain)
                .setPositiveButton(R.string.dialog_send_crash_yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        Intent i = new Intent(Intent.ACTION_SENDTO);
                        i.setData(Uri.parse("mailto:ehviewersu@gmail.com"));
                        i.putExtra(Intent.EXTRA_SUBJECT, "I found a bug in EhViewer !");
                        i.putExtra(Intent.EXTRA_TEXT, lastCrash);
                        startActivity(i);
                        lastCrash = null;
                        
                        check(CHECK_NETWORK);
                    }
                }).setNegativeButton(R.string.dialog_send_crash_no, new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        lastCrash = null;
                        
                        check(CHECK_NETWORK);
                    }
                }).create();
    }
    
    private AlertDialog createNetworkErrorDialog() {
        return new DialogBuilder(this).setCancelable(false)
                .setTitle(R.string.error).setMessage(R.string.em_no_network)
                .setPositiveButton(R.string.dailog_network_error_yes,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        checkOver();
                    }
                })
                .setNegativeButton(R.string.dailog_network_error_no,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                        finish();
                    }
                }).create();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);
        
        LinearLayout wellcome = (LinearLayout)findViewById(R.id.app_start_view);
        // Show welcome in progress
        AlphaAnimation aa = new AlphaAnimation(0.3f,1.0f);
        aa.setDuration(2000);
        aa.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                synchronized (StartActivity.this) {
                    if (isCheckOver)
                        redirectTo();
                    isAnimationOver = true;
                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationStart(Animation animation) {}
        });
        wellcome.startAnimation(aa);
        check(CHECK_WARING);
    }
    
    private void check(int order) {
        switch (order) {
        case CHECK_WARING:
            if (!Config.isAllowed()) {
                createWarningDialog().show();
                return;
            }
        case CHECK_CRESH:
            if ((lastCrash = Crash.getLastCrash()) != null) {
                createSendCrashDialog().show();
                return;
            }
        case CHECK_NETWORK:
            if (!isNetworkAvailable()) {
                createNetworkErrorDialog().show();
                return;
            }
        }
        checkOver();
    }
    
    private synchronized void checkOver() {
        if (isAnimationOver)
            redirectTo();
        isCheckOver = true;
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = cm.getActiveNetworkInfo();
        if (mNetworkInfo == null || !mNetworkInfo.isAvailable())
            return false;
        else
            return true;
    }
    
    private void redirectTo(){        
        Intent intent = new Intent(this, MangaListActivity.class);
        startActivity(intent);
        finish();
    }
}
