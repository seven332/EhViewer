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

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.DirGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProviderListener;
import com.hippo.ehviewer.gallery.gl.GalleryPageView;
import com.hippo.ehviewer.gallery.gl.GalleryView;
import com.hippo.gl.glrenderer.ImageTexture;
import com.hippo.gl.glrenderer.MovableTextTexture;
import com.hippo.gl.view.GLRootView;
import com.hippo.gl.view.GLView;
import com.hippo.image.Image;
import com.hippo.yorozuya.IntIdGenerator;

import java.io.File;

public class GalleryActivity extends AppCompatActivity implements GalleryProviderListener {

    @Nullable
    private GalleryView mGalleryView;

    @Nullable
    private MovableTextTexture mPageTextTexture;
    @Nullable
    private ImageTexture.Uploader mUploader;
    @Nullable
    private GalleryProvider mGalleryProvider;
    @Nullable
    private SparseIntArray mIndexIdMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);
        GLRootView glRootView = (GLRootView) findViewById(R.id.gl_root_view);

        mUploader = new ImageTexture.Uploader(glRootView);
        mIndexIdMap = new SparseIntArray();
        mGalleryProvider = new DirGalleryProvider(new File(Environment.getExternalStorageDirectory(), "nmb/image"));
        mGalleryProvider.addGalleryProviderListener(this);
        mGalleryProvider.start();

        mPageTextTexture = MovableTextTexture.create(Typeface.DEFAULT,
                getResources().getDimensionPixelSize(R.dimen.gallery_page_text),
                getResources().getColor(R.color.secondary_text_dark),
                new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});

        mGalleryView = new GalleryView(this, new GalleryPageIterator(),
                mPageTextTexture, null, GalleryView.LAYOUT_MODE_LEFT_TO_RIGHT);
        glRootView.setContentPane(mGalleryView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGalleryView = null;
        if (mUploader != null) {
            mUploader.clear();
            mUploader = null;
        }
        if (mPageTextTexture != null) {
            mPageTextTexture.recycle();
            mPageTextTexture = null;
        }
        if (mGalleryProvider != null) {
            mGalleryProvider.removeGalleryProviderListener(this);
            mGalleryProvider.stop();
            mGalleryProvider = null;
        }
        mIndexIdMap = null;
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

    private GalleryPageView findPageByIndex(int index) {
        if (mIndexIdMap == null || mGalleryView == null) {
            return null;
        }

        int id = mIndexIdMap.get(index, GLView.NO_ID);
        if (id == GLView.NO_ID) {
            return null;
        }

        return mGalleryView.findPageById(id);
    }

    @Override
    public void onPagePercent(int index, float percent) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(percent);
            page.setError(null, null);
        }
    }

    @Override
    public void onPageSucceed(int index, Image image) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null) {
            page.showImage();
            page.setImage(image);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_GONE);
            page.setError(null, null);
        }
    }

    @Override
    public void onPageFailed(int index, Exception e) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null && mGalleryView != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_GONE);
            page.setError("Error", mGalleryView);
        }
    }

    private class GalleryPageIterator extends GalleryView.PageIterator {

        private IntIdGenerator mIdGenerator = new IntIdGenerator();
        private int mIndex = 0;
        private int mBackup = mIndex;

        @Override
        public void mark() {
            mBackup = mIndex;
        }

        @Override
        public void reset() {
            mIndex = mBackup;
        }

        @Override
        public boolean isBusy() {
            return mGalleryProvider == null ||
                    mGalleryProvider.size() == GalleryProvider.SIZE_WAIT;
        }

        @Override
        public String isError() {
            if (mGalleryProvider != null) {
                int size = mGalleryProvider.size();
                if (size == 0) {
                    return "Empty Gallery"; // TODO hardcode
                } else if (size < 0 && size != GalleryProvider.SIZE_WAIT) {
                    return "Weird"; // TODO hardcode
                }
            }
            return null;
        }

        @Override
        public boolean hasNext() {
            return mGalleryProvider != null && mIndex < mGalleryProvider.size() - 1;
        }

        @Override
        public boolean hasPrevious() {
            return mIndex > 0;
        }

        @Override
        public void next() {
            if (mGalleryProvider != null && mIndex >= mGalleryProvider.size() - 1) {
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
        public int onBind(GalleryPageView view) {
            if (mGalleryProvider != null && mGalleryView != null) {
                switch (mGalleryProvider.request(mIndex)) {
                    case GalleryProvider.RESULT_WAIT:
                        view.showInfo();
                        view.setImage(null);
                        view.setPage(mIndex + 1);
                        view.setProgress(GalleryPageView.PROGRESS_INDETERMINATE);
                        view.setError(null, null);
                        break;
                    case GalleryProvider.RESULT_FAILED:
                        view.showInfo();
                        view.setImage(null);
                        view.setPage(mIndex + 1);
                        view.setProgress(GalleryPageView.PROGRESS_GONE);
                        view.setError("Failed", mGalleryView);
                        break;
                    case GalleryProvider.RESULT_ERROR:
                        view.showInfo();
                        view.setImage(null);
                        view.setPage(mIndex + 1);
                        view.setProgress(GalleryPageView.PROGRESS_GONE);
                        view.setError("Error", mGalleryView);
                        break;
                }
            }

            int id = mIdGenerator.nextId();
            if (mIndexIdMap != null) {
                mIndexIdMap.put(mIndex, id);
            }
            return id;
        }

        @Override
        public void onUnbind(GalleryPageView view) {
            if (mIndexIdMap != null) {
                int index = mIndexIdMap.indexOfKey(view.getId());
                if (index >= 0) {
                    mIndexIdMap.removeAt(index);
                }
            }

            view.setImage(null);
            view.setError(null, null);
        }
    }
}
