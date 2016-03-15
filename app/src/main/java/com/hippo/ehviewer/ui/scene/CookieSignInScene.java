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

package com.hippo.ehviewer.ui.scene;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.rippleold.RippleSalon;
import com.hippo.util.ActivityHelper;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ViewUtils;

import okhttp3.Cookie;

public class CookieSignInScene extends BaseScene implements EditText.OnEditorActionListener,
        View.OnClickListener {

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private TextInputLayout mIpbMemberIdLayout;
    @Nullable
    private TextInputLayout mIpbPassHashLayout;
    @Nullable
    private EditText mIpbMemberId;
    @Nullable
    private EditText mIpbPassHash;
    @Nullable
    private View mOk;

    @Override
    public boolean needShowLeftDrawer() {
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_cookie_sign_in, container, false);
        mIpbMemberIdLayout = (TextInputLayout) ViewUtils.$$(view, R.id.ipb_member_id_layout);
        mIpbMemberId = mIpbMemberIdLayout.getEditText();
        AssertUtils.assertNotNull(mIpbMemberId);
        mIpbPassHashLayout = (TextInputLayout) ViewUtils.$$(view, R.id.ipb_pass_hash_layout);
        mIpbPassHash = mIpbPassHashLayout.getEditText();
        AssertUtils.assertNotNull(mIpbPassHash);
        mOk = ViewUtils.$$(view, R.id.ok);

        mIpbPassHash.setOnEditorActionListener(this);

        mOk.setOnClickListener(this);

        RippleSalon.addRipple(mOk, true);

        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ActivityHelper.showSoftInput(getActivity(), mIpbMemberId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mIpbMemberIdLayout = null;
        mIpbPassHashLayout = null;
        mIpbMemberId = null;
        mIpbPassHash = null;
    }

    @Override
    public void onClick(View v) {
        if (mOk == v) {
            enter();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (mIpbPassHash == v) {
            enter();
        }
        return true;
    }

    // false for error
    private static boolean checkIpbMemberId(String id) {
        for (int i = 0, n = id.length(); i < n; i++) {
            char ch = id.charAt(i);
            if (!(ch >= '0' && ch <= '9')) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkIpbPassHash(String hash) {
        if (32 != hash.length()) {
            return false;
        }

        for (int i = 0, n = hash.length(); i < n; i++) {
            char ch = hash.charAt(i);
            if (!(ch >= '0' && ch <= '9') && !(ch >= 'a' && ch <= 'z')) {
                return false;
            }
        }
        return true;
    }

    public void enter() {
        if (null == mIpbMemberIdLayout || null == mIpbPassHashLayout ||
                null == mIpbMemberId || null == mIpbPassHash) {
            return;
        }

        final String ipbMemberId = mIpbMemberId.getText().toString().trim();
        final String ipbPassHash = mIpbPassHash.getText().toString().trim();

        if (TextUtils.isEmpty(ipbMemberId)) {
            mIpbMemberIdLayout.setError(getString(R.string.text_is_empty));
            return;
        } else {
            mIpbMemberIdLayout.setError(null);
        }
        if (TextUtils.isEmpty(ipbPassHash)) {
            mIpbPassHashLayout.setError(getString(R.string.text_is_empty));
            return;
        } else {
            mIpbPassHashLayout.setError(null);
        }

        ActivityHelper.hideSoftInput(getActivity());

        if (!checkIpbMemberId(ipbMemberId) || !(checkIpbPassHash(ipbPassHash))) {
            new AlertDialog.Builder(getContext()).setTitle(R.string.waring)
                    .setMessage(R.string.wrong_cookie_warning)
                    .setNegativeButton(R.string.i_dont_think_so, null)
                    .setPositiveButton(R.string.i_will_check_it, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            storeCookie(ipbMemberId, ipbPassHash);
                            setResult(RESULT_OK, null);
                            finish();
                        }
                    }).show();
        } else {
            storeCookie(ipbMemberId, ipbPassHash);
            setResult(RESULT_OK, null);
            finish();
        }
    }

    private static Cookie newCookie(String name, String value, String domain) {
        return new Cookie.Builder().name(name).value(value)
                .domain(domain).expiresAt(Long.MAX_VALUE).build();
    }

    private void storeCookie(String id, String hash) {
        Context context = getContext();
        if (null == context) {
            return;
        }

        EhUtils.signOut(getContext());

        EhCookieStore store = EhApplication.getEhCookieStore(context);
        store.add(newCookie(EhCookieStore.KEY_IPD_MEMBER_ID, id, EhUrl.DOMAIN_E));
        store.add(newCookie(EhCookieStore.KEY_IPD_MEMBER_ID, id, EhUrl.DOMAIN_EX));
        store.add(newCookie(EhCookieStore.KEY_IPD_PASS_HASH, hash, EhUrl.DOMAIN_E));
        store.add(newCookie(EhCookieStore.KEY_IPD_PASS_HASH, hash, EhUrl.DOMAIN_EX));
    }
}
