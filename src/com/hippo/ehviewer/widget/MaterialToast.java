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

package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.R;

public class MaterialToast extends Toast {

    private static Context sContext;

    public static void setContext(Context context) {
        sContext = context;
    }

    @SuppressLint("InflateParams")
    public MaterialToast(String mesg) {
        super(sContext);
        init(mesg);
    }

    @SuppressLint("InflateParams")
    public MaterialToast(int resId) {
        this(sContext.getString(resId));
    }

    @SuppressLint("InflateParams")
    private void init(String mesg) {
        View v = LayoutInflater.from(sContext)
                .inflate(R.layout.material_toast, null);
        TextView tv = (TextView)v.findViewById(R.id.message);
        tv.setText(mesg);
        setView(v);
        setDuration(LENGTH_SHORT);
    }

    public static final void showToast(String mesg) {
        new MaterialToast(mesg).show();
    }

    public static final void showToast(int resId) {
        new MaterialToast(resId).show();
    }
}
