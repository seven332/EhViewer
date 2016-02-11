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

package com.hippo.ehviewer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.gl.GalleryPageView;
import com.hippo.ehviewer.ui.gl.GalleryView;
import com.hippo.gl.glrenderer.ImageTexture;
import com.hippo.gl.view.GLRootView;
import com.hippo.image.Image;
import com.hippo.util.Dpad;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class GalleryActivity extends AppCompatActivity {

    private GalleryView mGalleryView;

    private ImageTexture.Uploader mUploader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);
        GLRootView glRootView = (GLRootView) findViewById(R.id.gl_root_view);
        mGalleryView = new GalleryView(this, new InvalidPageIterator(), null);
        glRootView.setContentPane(mGalleryView);
        mUploader = new ImageTexture.Uploader(glRootView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGalleryView = null;
        mUploader = null;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        switch (Dpad.getDirectionPressed(event)) {
            case Dpad.LEFT:
                // TODO do something
                return true;
            case Dpad.UP:
                // TODO do something
                return true;
            case Dpad.RIGHT:
                // TODO do something
                return true;
            case Dpad.DOWN:
                // TODO do something
                return true;
            case Dpad.CENTER:
                // TODO do something
                return true;
        }

        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check volume
        if (false) { // TODO Setting.getVolumePage()
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                // TODO do something
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                // TODO do something
                return true;
            }
        }

        // Check keyboard and Dpad
        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_UP:
                // TODO do something
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                // TODO do something
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // TODO do something
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                // TODO do something
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // TODO do something
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // TODO do something
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // TODO do something
                return true;
            case KeyEvent.KEYCODE_SPACE:
                // TODO do something
                return true;
            case KeyEvent.KEYCODE_MENU:
                // TODO do something
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Check volume
        if (false) { // TODO Setting.getVolumePage()
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                    keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }
        }

        // Check keyboard and Dpad
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_SPACE ||
                keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }


    private static final int MAX_SIZE = 10;

    private class InvalidPageIterator implements GalleryView.PageIterator {

        private int mIndex = 0;
        private int mBackup = mIndex;

        public ImageTexture mImageTexture;

        public InvalidPageIterator() {
            try {
                mImageTexture = new ImageTexture(Image.decode(new FileInputStream("/sdcard/1.jpg"), false));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void mark() {
            mBackup = mIndex;
        }

        @Override
        public void reset() {
            mIndex = mBackup;
        }

        @Override
        public boolean hasNext() {
            return mIndex < MAX_SIZE - 1;
        }

        @Override
        public boolean hasPrevious() {
            return mIndex > 0;
        }

        @Override
        public boolean isValid() {
            return mIndex >= 0 && mIndex < MAX_SIZE;
        }

        @Override
        public void next() {
            if (mIndex >= MAX_SIZE - 1) {
                throw new IndexOutOfBoundsException();
            }
            mIndex++;
        }

        @Override
        public void previous() {
            if (mIndex <= 0) {
                throw new IndexOutOfBoundsException();
            }
            mIndex--;
        }

        @Override
        public void bind(GalleryPageView view) {
            //view.showImage();
            view.getImageView().setTexture(mImageTexture);
        }

        @Override
        public void unbind(GalleryPageView view) {
            view.getImageView().setTexture(null);
        }
    }

}
