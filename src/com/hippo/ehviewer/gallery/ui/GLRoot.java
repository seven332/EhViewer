/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.hippo.ehviewer.gallery.ui;

import android.content.Context;
import android.graphics.Matrix;

import com.hippo.ehviewer.gallery.anim.CanvasAnimation;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;

public interface GLRoot {

    // Listener will be called when GL is idle AND before each frame.
    // Mainly used for uploading textures.
    public static interface OnGLIdleListener {
        public boolean onGLIdle(
                GLCanvas canvas, boolean renderRequested);
    }

    public void addOnGLIdleListener(OnGLIdleListener listener);
    public void registerLaunchedAnimation(CanvasAnimation animation);
    public void requestRenderForced();
    public void requestRender();
    public void requestLayoutContentPane();

    public void lockRenderThread();
    public void unlockRenderThread();

    public void setContentPane(GLView content);
    public void setOrientationSource(OrientationSource source);
    public int getDisplayRotation();
    public int getCompensation();
    public Matrix getCompensationMatrix();
    public void freeze();
    public void unfreeze();
    public void setLightsOutMode(boolean enabled);

    public Context getContext();
}
