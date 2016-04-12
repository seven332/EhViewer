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

package com.hippo.ehviewer.preference;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.network.InetValidator;
import com.hippo.preference.DialogPreference;
import com.hippo.yorozuya.ViewUtils;

public class HatHProxyPreference extends DialogPreference implements View.OnClickListener {

    @Nullable
    private SwitchCompat mEnable;
    @Nullable
    private TextInputLayout mIpInputLayout;
    @Nullable
    private EditText mIp;
    @Nullable
    private TextInputLayout mPortInputLayout;
    @Nullable
    private EditText mPort;
    @Nullable
    private TextInputLayout mPasskeyInputLayout;
    @Nullable
    private EditText mPasskey;

    public HatHProxyPreference(Context context) {
        super(context);
        init();
    }

    public HatHProxyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HatHProxyPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.preference_dialog_hath_proxy);
        updateSummary(Settings.getHathProxy(), Settings.getHathIp(), Settings.getHathPort());
    }

    private void updateSummary(boolean hathProxy, String hathIp, int hathPort) {
        if (InetValidator.isValidInet4Address(hathIp) && InetValidator.isValidInetPort(hathPort)) {
            Context context = getContext();
            setSummary(context.getString(R.string.settings_eh_hath_proxy_summary_1,
                    context.getString(hathProxy ? R.string.enabled : R.string.disabled), hathIp, hathPort));
        } else {
            setSummary(R.string.settings_eh_hath_proxy_summary_2);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(android.R.string.ok, null);
    }

    @Override
    @SuppressLint("SetTextI18n")
    protected void onDialogCreated(AlertDialog dialog) {
        super.onDialogCreated(dialog);

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);

        mEnable = (SwitchCompat) ViewUtils.$$(dialog, R.id.enable);
        mIpInputLayout = (TextInputLayout) ViewUtils.$$(dialog, R.id.ip_input_layout);
        mIp = (EditText) ViewUtils.$$(dialog, R.id.ip);
        mPortInputLayout = (TextInputLayout) ViewUtils.$$(dialog, R.id.port_input_layout);
        mPort = (EditText) ViewUtils.$$(dialog, R.id.port);
        mPasskeyInputLayout = (TextInputLayout) ViewUtils.$$(dialog, R.id.passkey_input_layout);
        mPasskey = (EditText) ViewUtils.$$(dialog, R.id.passkey);

        mEnable.setChecked(Settings.getHathProxy());
        String ip = Settings.getHathIp();
        if (!InetValidator.isValidInet4Address(ip)) {
            ip = null;
        }
        String portString;
        int port = Settings.getHathPort();
        if (!InetValidator.isValidInetPort(port)) {
            portString = null;
        } else {
            portString = Integer.toString(Settings.getHathPort());
        }
        mIp.setText(ip);
        mPort.setText(portString);
        mPasskey.setText(Settings.getHathPasskey());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        mEnable = null;
        mIpInputLayout = null;
        mIp = null;
        mPortInputLayout = null;
        mPort = null;
        mPasskeyInputLayout = null;
        mPasskey = null;
    }

    @Override
    public void onClick(View v) {
        Dialog dialog = getDialog();
        Context context = getContext();
        if (null == dialog || null == context || null == mEnable ||
                null == mIpInputLayout || null == mIp ||
                null == mPortInputLayout || null == mPort ||
                null == mPasskeyInputLayout || null == mPasskey) {
            return;
        }

        boolean enable = mEnable.isChecked();

        String ip = mIp.getText().toString().trim();
        if (ip.isEmpty()) {
            if (enable) {
                mIpInputLayout.setError(context.getString(R.string.text_is_empty));
                return;
            }
        } else if (!InetValidator.isValidInet4Address(ip)) {
            mIpInputLayout.setError(context.getString(R.string.domain_not_supported));
            return;
        } else {
            mIpInputLayout.setError(null);
        }

        int port;
        String portString = mPort.getText().toString().trim();
        if (portString.isEmpty()) {
            if (enable) {
                mPortInputLayout.setError(context.getString(R.string.text_is_empty));
                return;
            } else {
                port = -1;
            }
        } else {
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                port = -1;
            }
            if (!InetValidator.isValidInetPort(port)) {
                mPortInputLayout.setError(context.getString(R.string.invalid_port));
                return;
            }
        }

        String passkey = mPasskey.getText().toString();

        Settings.putHathProxy(enable);
        Settings.putHathIp(ip);
        Settings.putHathPort(port);
        Settings.putHathPasskey(passkey);

        updateSummary(enable, ip, port);

        dialog.dismiss();
    }
}
