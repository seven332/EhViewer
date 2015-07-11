/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.ehviewer.ui.scene;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.util.Settings;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Scene;
import com.hippo.util.Messenger;

public final class SignInScene extends Scene implements View.OnClickListener,
        TextView.OnEditorActionListener {

    private View mMainView;
    private EditText mUsername;
    private EditText mPassword;
    private TextView mSignUp;
    private TextView mSignIn;

    private EhClient.EhListener<String> mSignInListener = new EhClient.EhListener<String>() {
        @Override
        public void onSuccess(String displayname) {
            Settings.putSignIn(true);
            Settings.putDisplayName(displayname);
            Messenger.getInstance().notify(Constants.MESSENGER_ID_SIGN_IN_OR_OUT, displayname);
        }

        @Override
        public void onFailure(Exception e) {
            e.printStackTrace();
        }

        @Override
        public void onCanceled() {

        }
    };

    @Override
    public int getLaunchMode() {
        return LAUNCH_MODE_SINGLE_TOP;
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        setContentView(R.layout.scene_sign_in);

        mMainView = findViewById(R.id.sigin_in_main);
        mUsername = (EditText) mMainView.findViewById(R.id.username);
        mPassword = (EditText) mMainView.findViewById(R.id.password);
        mSignUp = (TextView) mMainView.findViewById(R.id.sign_up);
        mSignIn = (TextView) mMainView.findViewById(R.id.sign_in);

        mPassword.setOnEditorActionListener(this);
        RippleSalon.addRipple(mSignUp, false);
        RippleSalon.addRipple(mSignIn, false);
        mSignUp.setOnClickListener(this);
        mSignIn.setOnClickListener(this);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mMainView.setPadding(mMainView.getPaddingLeft(),
                mMainView.getPaddingTop(),
                mMainView.getPaddingRight(), b);
    }

    @Override
    public void onClick(View v) {
        if (v == mSignIn) {
            String username = mUsername.getText().toString();
            String password = mPassword.getText().toString();
            EhClient client = EhApplication.getEhClient(getStageActivity());

            EhRequest request = new EhRequest();
            request.setMethod(EhClient.METHOD_SIGN_IN);
            request.setEhListener(mSignInListener);
            request.setArgs(username, password);
            client.execute(request);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mPassword) {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                onClick(mSignIn);
                return true;
            }
        }
        return false;
    }
}
