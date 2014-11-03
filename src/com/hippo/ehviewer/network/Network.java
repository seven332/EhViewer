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

package com.hippo.ehviewer.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public final class Network {

    public static final int NETWORK_STATE_NONE = 0x0;
    public static final int NETWORK_STATE_MOBILE = 0x1;
    public static final int NETWORK_STATE_WIFI = 0x2;

    public static int getNetworkState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
}
