package com.hippo.ehviewer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.widget.MaterialToast;

public class RestPatternReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Config.setPatternProtection("");
        MaterialToast.showToast(R.string.reset_pattern_protection);
    }
}
