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

package com.hippo.preference;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.hippo.ehviewer.R;
import com.hippo.text.Html;

// TODO Add html attr
// TODO Add url clickable
public class SimpleDialogPreference extends DialogPreference {

    private CharSequence mMessage;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SimpleDialogPreference(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public SimpleDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public SimpleDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SimpleDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SimpleDialogPreference, defStyleAttr, defStyleRes);
        setMessage(a.getString(R.styleable.SimpleDialogPreference_message));
        a.recycle();
    }

    public void setMessage(String message) {
        mMessage = Html.fromHtml(message);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setMessage(mMessage);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(null, null);
    }
}
