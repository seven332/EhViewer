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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.ehviewer.Crash;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ripple.Ripple;
import com.hippo.util.AppHelper;
import com.hippo.yorozuya.ViewUtils;

public class CrashScene extends SolidScene implements View.OnClickListener {

    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private String mCrash;

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private View mCancel;
    @Nullable
    private View mSend;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCrash = Crash.getCrashContent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCrash = null;
    }

    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_crash, container, false);

        mCancel = ViewUtils.$$(view, R.id.cancel);
        mSend = ViewUtils.$$(view, R.id.send);

        mCancel.setOnClickListener(this);
        mSend.setOnClickListener(this);

        Ripple.addRipple(mCancel, true);
        Ripple.addRipple(mSend, true);

        return view;
    }

    @Override
    public void onClick(View v) {
        MainActivity activity = getActivity2();

        if (mSend == v && null != mCrash && null != activity) {
            AppHelper.sendEmail(activity, EhApplication.getDeveloperEmail(),
                    "I found a bug in EhViewer !", mCrash);
        }
        Crash.resetCrashFile();

        // Start new scene and finish it self
        if (null != activity) {
            startSceneForCheckStep(CHECK_STEP_CRASH, getArguments());
        }
        finish();
    }
}
