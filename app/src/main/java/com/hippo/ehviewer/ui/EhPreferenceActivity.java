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

package com.hippo.ehviewer.ui;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.WindowManager;
import com.google.analytics.tracking.android.EasyTracker;
import com.hippo.app.AppCompatPreferenceActivity;
import com.hippo.content.ContextLocalWrapper;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.Settings;
import java.util.Locale;

public abstract class EhPreferenceActivity extends AppCompatPreferenceActivity {

    private boolean mTrackStarted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((EhApplication) getApplication()).registerActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ((EhApplication) getApplication()).unregisterActivity(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (Settings.getEnableAnalytics()) {
            EasyTracker.getInstance(this).activityStart(this);
            mTrackStarted = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mTrackStarted) {
            EasyTracker.getInstance(this).activityStop(this);
            mTrackStarted = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Settings.getEnabledSecurity()){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }else{
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale locale = null;
        String language = Settings.getAppLanguage();
        if (language != null && !language.equals("system")) {
            String[] split = language.split("-");
            if (split.length == 1) {
                locale = new Locale(split[0]);
            } else if (split.length == 2) {
                locale = new Locale(split[0], split[1]);
            } else if (split.length == 3) {
                locale = new Locale(split[0], split[1], split[2]);
            }
        }

        if (locale == null) {
            locale = Resources.getSystem().getConfiguration().locale;
        }

        newBase = ContextLocalWrapper.wrap(newBase, locale);
        super.attachBaseContext(newBase);
    }
}
