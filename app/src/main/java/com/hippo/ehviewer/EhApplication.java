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

package com.hippo.ehviewer;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.network.StatusCodeException;
import com.hippo.okhttp.CookieDB;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class EhApplication extends Application {

    private EhCookieStore mEhCookieStore;
    private EhClient mEhClient;
    private OkHttpClient mOkHttpClient;

    @Override
    public void onCreate() {
        super.onCreate();

        GetText.initialize(this);
        CookieDB.initialize(this);
        StatusCodeException.initialize(this);
        Settings.initialize(this);
    }

    public static EhCookieStore getEhCookieStore(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhCookieStore == null) {
            application.mEhCookieStore = new EhCookieStore();
        }
        return application.mEhCookieStore;
    }

    @NonNull
    public static EhClient getEhClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhClient == null) {
            application.mEhClient = new EhClient(application);
        }
        return application.mEhClient;
    }

    public static OkHttpClient getOkHttpClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mOkHttpClient == null) {
            application.mOkHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .cookieJar(getEhCookieStore(application))
                    .build();
        }
        return application.mOkHttpClient;
    }
}
