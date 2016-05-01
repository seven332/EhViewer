/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.hippo.gl.view;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;

final class EGLError {
    private EGLError() {}

    private static String getHex(int value) {
        return "0x" + Integer.toHexString(value);
    }

    public static String getErrorString(int error) {
        switch (error) {
        case EGL10.EGL_SUCCESS:
            return "EGL_SUCCESS";
        case EGL10.EGL_NOT_INITIALIZED:
            return "EGL_NOT_INITIALIZED";
        case EGL10.EGL_BAD_ACCESS:
            return "EGL_BAD_ACCESS";
        case EGL10.EGL_BAD_ALLOC:
            return "EGL_BAD_ALLOC";
        case EGL10.EGL_BAD_ATTRIBUTE:
            return "EGL_BAD_ATTRIBUTE";
        case EGL10.EGL_BAD_CONFIG:
            return "EGL_BAD_CONFIG";
        case EGL10.EGL_BAD_CONTEXT:
            return "EGL_BAD_CONTEXT";
        case EGL10.EGL_BAD_CURRENT_SURFACE:
            return "EGL_BAD_CURRENT_SURFACE";
        case EGL10.EGL_BAD_DISPLAY:
            return "EGL_BAD_DISPLAY";
        case EGL10.EGL_BAD_MATCH:
            return "EGL_BAD_MATCH";
        case EGL10.EGL_BAD_NATIVE_PIXMAP:
            return "EGL_BAD_NATIVE_PIXMAP";
        case EGL10.EGL_BAD_NATIVE_WINDOW:
            return "EGL_BAD_NATIVE_WINDOW";
        case EGL10.EGL_BAD_PARAMETER:
            return "EGL_BAD_PARAMETER";
        case EGL10.EGL_BAD_SURFACE:
            return "EGL_BAD_SURFACE";
        case EGL11.EGL_CONTEXT_LOST:
            return "EGL_CONTEXT_LOST";
        default:
            return getHex(error);
        }
    }
}
