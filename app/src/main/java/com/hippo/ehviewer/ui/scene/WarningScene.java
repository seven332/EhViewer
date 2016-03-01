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

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.util.ActivityHelper;

public final class WarningScene extends BaseScene implements View.OnClickListener {

    private View mCancel;
    private View mOk;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_warning, container, false);

        mCancel = view.findViewById(R.id.cancel);
        mOk = view.findViewById(R.id.ok);

        mCancel.setOnClickListener(this);
        mOk.setOnClickListener(this);

        RippleSalon.addRipple(mCancel, true);
        RippleSalon.addRipple(mOk, true);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide IME
        ActivityHelper.hideSoftInput(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mCancel = null;
        mOk = null;
    }

    @Override
    public void onClick(View v) {
        if (mCancel == v) {
            finishStage();
        } else if (mOk == v) {
            // Never show this warning anymore
            Settings.putShowWarning(false);

            if (EhUtils.hasSignedIn(getContext())) {
                Bundle args = new Bundle();
                args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
                startScene(new Announcer(GalleryListScene.class).setArgs(args));
                finish();
                // Enable drawer
                setDrawerLayoutEnable(true);
            } else {
                startScene(new Announcer(LoginScene.class));
                finish();
            }
        }
    }
}
