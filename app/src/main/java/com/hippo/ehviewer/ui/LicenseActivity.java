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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;

public class LicenseActivity extends ToolbarActivity {

    @Nullable
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWebView = new WebView(this);
        mWebView.loadUrl("file:///android_asset/NOTICE.html");
        setContentView(mWebView);

        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != mWebView) {
            mWebView.destroy();
            mWebView = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
}
