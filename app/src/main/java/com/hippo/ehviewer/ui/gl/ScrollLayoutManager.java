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

package com.hippo.ehviewer.ui.gl;

import android.support.annotation.NonNull;
import android.view.MotionEvent;

public class ScrollLayoutManager extends GalleryView.LayoutManager {

    public ScrollLayoutManager(@NonNull GalleryView galleryView) {
        super(galleryView);
    }

    @Override
    public void onAttach(GalleryView.PageIterator iterator) {

    }

    @Override
    public GalleryView.PageIterator onDetach() {
        return null;
    }

    @Override
    public void onFill() {

    }

    @Override
    public void onDown() {

    }

    @Override
    public void onUp() {

    }

    @Override
    public void onScroll(float dx, float dy, float totalX, float totalY, float x, float y) {

    }

    @Override
    public void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

    }

    @Override
    public boolean canScale() {
        return false;
    }

    @Override
    public void onScale(float focusX, float focusY, float scale) {

    }

    @Override
    public boolean onUpdateAnimation(long time) {
        return false;
    }
}
