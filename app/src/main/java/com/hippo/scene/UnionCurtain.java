/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.scene;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class UnionCurtain extends Curtain {

    private Curtain mOpenCurtain;
    private Curtain mCloseCurtain;

    public UnionCurtain(Curtain openCurtain, Curtain closeCurtain) {
        mOpenCurtain = openCurtain;
        mCloseCurtain = closeCurtain;
    }

    @Override
    protected void setPreviousScene(@Nullable Scene pScene) {
        super.setPreviousScene(pScene);

        mOpenCurtain.setPreviousScene(pScene);
        mCloseCurtain.setPreviousScene(pScene);
    }

    @Override
    protected boolean needSpecifyPreviousScene() {
        return mCloseCurtain.needSpecifyPreviousScene();
    }

    @Override
    protected void onRebirth() {
        mCloseCurtain.onRebirth();
    }

    @Override
    public void open(@NonNull Scene enter, @NonNull Scene exit) {
        mOpenCurtain.open(enter, exit);
    }

    @Override
    public void close(@NonNull Scene enter, @NonNull Scene exit) {
        mCloseCurtain.close(enter, exit);
    }

    @Override
    public void endAnimation() {
        mOpenCurtain.endAnimation();
        mCloseCurtain.endAnimation();
    }

    @Override
    public boolean isInAnimation() {
        return mOpenCurtain.isInAnimation() || mCloseCurtain.isInAnimation();
    }
}
