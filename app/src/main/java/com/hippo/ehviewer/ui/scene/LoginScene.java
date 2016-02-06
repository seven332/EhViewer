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
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.util.ExceptionUtils;

public final class LoginScene extends BaseScene implements EditText.OnEditorActionListener,
        View.OnClickListener {

    private View mProgress;
    private TextInputLayout mUsernameLayout;
    private TextInputLayout mPasswordLayout;
    private EditText mUsername;
    private EditText mPassword;
    private View mRegister;
    private View mSignIn;
    private TextView mSignInViaWebview;
    private TextView mSignInViaCookies;
    private TextView mSkipSigningIn;

    private EhClient mClient;
    private EhCookieStore mCookieStore;
    private EhRequest mSignInRequest;

    @Override
    public int getSoftInputMode() {
        return WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClient = EhApplication.getEhClient(getContext());
        mCookieStore = EhApplication.getEhCookieStore(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_login, container, false);

        View loginForm = view.findViewById(R.id.login_form);
        mProgress = view.findViewById(R.id.progress);
        mUsernameLayout = (TextInputLayout) loginForm.findViewById(R.id.username_layout);
        mUsername = mUsernameLayout.getEditText();
        mPasswordLayout = (TextInputLayout) loginForm.findViewById(R.id.password_layout);
        mPassword = mPasswordLayout.getEditText();
        mRegister = loginForm.findViewById(R.id.register);
        mSignIn = loginForm.findViewById(R.id.sign_in);
        mSignInViaWebview = (TextView) loginForm.findViewById(R.id.sign_in_via_webview);
        mSignInViaCookies = (TextView) loginForm.findViewById(R.id.sign_in_via_cookies);
        mSkipSigningIn = (TextView) loginForm.findViewById(R.id.skip_signing_in);

        mSignInViaWebview.setPaintFlags(mSignInViaWebview.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mSignInViaCookies.setPaintFlags(mSignInViaCookies.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mSkipSigningIn.setPaintFlags(mSignInViaCookies.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        mPassword.setOnEditorActionListener(this);

        mRegister.setOnClickListener(this);
        mSignIn.setOnClickListener(this);
        mSignInViaWebview.setOnClickListener(this);
        mSignInViaCookies.setOnClickListener(this);
        mSkipSigningIn.setOnClickListener(this);

        RippleSalon.addRipple(mRegister, true);
        RippleSalon.addRipple(mSignIn, true);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSignInRequest != null) {
            mSignInRequest.cancel();
            mSignInRequest = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (mRegister == v) {

        } else if (mSignIn == v) {
            signIn();
        } else if (mSignInViaWebview == v) {

        } else if (mSignInViaCookies == v) {

        } else if (mSkipSigningIn == v) {
            redirectTo();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mPassword) {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                signIn();
                return true;
            }
        }

        return false;
    }

    private void signIn() {
        if (mSignInRequest != null) {
            return;
        }

        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();

        if (username.isEmpty()) {
            mUsernameLayout.setError(getString(R.string.error_username_cannot_empty));
            return;
        } else {
            mUsernameLayout.setError(null);
        }

        if (password.isEmpty()) {
            mPasswordLayout.setError(getString(R.string.error_password_cannot_empty));
            return;
        } else {
            mPasswordLayout.setError(null);
        }

        // Hide ime keyboard
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        mProgress.setAlpha(0.0f);
        mProgress.setVisibility(View.VISIBLE);
        mProgress.animate().alpha(1.0f).setDuration(500).start();

        // Clean up for sign in
        mCookieStore.cleanUpForSignIn();

        // TODO Make callback static
        mSignInRequest = new EhRequest()
                .setMethod(EhClient.METHOD_SIGN_IN)
                .setArgs(username, password)
                .setCallback(new EhClient.Callback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        mSignInRequest = null;
                        Settings.putDisplayName(result);

                        if (mCookieStore.hasSignedIn()) {
                            // Has signed in
                            redirectTo();
                        } else {
                            mProgress.setVisibility(View.GONE);
                            whetherToSkip();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                        mSignInRequest = null;
                        mProgress.setVisibility(View.GONE);
                        Toast.makeText(getContext(), ExceptionUtils.getReadableString(getContext(), e),
                                Toast.LENGTH_SHORT).show();
                        whetherToSkip();
                    }

                    @Override
                    public void onCancel() {
                        mSignInRequest = null;
                        // User close this Scene
                    }
                });
        mClient.execute(mSignInRequest);
    }

    private void redirectTo() {
        Bundle args = new Bundle();
        args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
        startScene(new Announcer(GalleryListScene.class).setArgs(args));
        finish();
        // Enable drawer
        setDrawerLayoutEnable(true);
    }

    private void whetherToSkip() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.skip_signing_in)
                .setMessage(R.string.skip_signing_in_plain)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == AlertDialog.BUTTON_POSITIVE) {
                            redirectTo();
                        }
                    }
                }).show();
    }
}
