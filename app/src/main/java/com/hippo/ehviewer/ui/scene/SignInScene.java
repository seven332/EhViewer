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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene;

import android.annotation.Nullable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.hippo.effect.ripple.RippleSalon;
import com.hippo.ehviewer.R;
import com.hippo.scene.Scene;

public class SignInScene extends Scene {

    private View mMainView;
    private TextView mSignUp;
    private TextView mSignIn;

    @Override
    public int getLaunchMode() {
        return LAUNCH_MODE_SINGLE_TOP;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scene_sign_in);

        mMainView = findViewById(R.id.sigin_in_main);
        mSignUp = (TextView) mMainView.findViewById(R.id.sign_up);
        mSignIn = (TextView) mMainView.findViewById(R.id.sign_in);

        RippleSalon.addRipple(mSignUp, false);
        RippleSalon.addRipple(mSignIn, false);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mMainView.setPadding(mMainView.getPaddingLeft(),
                mMainView.getPaddingTop(),
                mMainView.getPaddingRight(), b);
    }
}
