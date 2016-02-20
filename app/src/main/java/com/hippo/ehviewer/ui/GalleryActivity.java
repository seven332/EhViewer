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

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.DirGalleryProvider;
import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProviderListener;
import com.hippo.ehviewer.gallery.ZipGalleryProvider;
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

    public static final String ACTION_DIR = "dir";
    public static final String ACTION_ZIP = "zip";
    public static final String ACTION_EH = "eh";

    public static final String KEY_ACTION = "action";
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_GALLERY_INFO = "gallery_info";

    private String mAction;
    private String mFilename;
    private GalleryInfo mGalleryInfo;

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

    private void buildProvider() {
        if (mGalleryProvider != null) {
            return;
        }

        if (ACTION_DIR.equals(mAction)) {
            if (mFilename != null) {
                mGalleryProvider = new DirGalleryProvider(new File(mFilename));
            }
        } else if (ACTION_ZIP.equals(mAction)) {
            if (mFilename != null) {
                mGalleryProvider = new ZipGalleryProvider(new File(mFilename));
            }
        } else if (ACTION_EH.equals(mAction)) {
            if (mGalleryInfo != null) {
                mGalleryProvider = new EhGalleryProvider(this, mGalleryInfo);
            }
        }
    }

    private void onInit() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        mAction = intent.getAction();
        mFilename = intent.getStringExtra(KEY_FILENAME);
        mGalleryInfo = intent.getParcelableExtra(KEY_GALLERY_INFO);
        buildProvider();
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mAction = savedInstanceState.getString(KEY_ACTION);
        mFilename = savedInstanceState.getString(KEY_FILENAME);
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
        buildProvider();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putString(KEY_FILENAME, mFilename);
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }

        if (mGalleryProvider == null) {
            finish();
            return;
        }
        mGalleryProvider.setGalleryProviderListener(this);
        mGalleryProvider.start();

        setContentView(R.layout.activity_gallery);
        GLRootView glRootView = (GLRootView) findViewById(R.id.gl_root_view);
        mGalleryProvider.setGLRoot(glRootView);
        mUploader = new ImageTexture.Uploader(glRootView);
        mIndexIdMap = new SparseIntArray();
        mPageTextTexture = MovableTextTexture.create(Typeface.DEFAULT,
                getResources().getDimensionPixelSize(R.dimen.gallery_page_text),
                getResources().getColor(R.color.secondary_text_dark),
                new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});

        mGalleryView = new GalleryView(this, new GalleryPageIterator(),
                mPageTextTexture, null, GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT);
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
            mGalleryProvider.setGalleryProviderListener(null);
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
    public void onDataChanged() {
        if (mGalleryView != null) {
            mGalleryView.onDataChanged();
        }
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
        } else {
            image.recycle();
        }
    }

    @Override
    public void onPageFailed(int index, String error) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null && mGalleryView != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_GONE);
            page.setError(error, mGalleryView);
        }
    }

    @Override
    public void onDataChanged(int index) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null && mGalleryProvider != null) {
            mGalleryProvider.request(index);
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
        public boolean isWaiting() {
            return mGalleryProvider != null && mGalleryProvider.size() == GalleryProvider.STATE_WAIT;
        }

        @Override
        public String getError() {
            if (mGalleryProvider == null) {
                return getString(R.string.error_no_provider);
            } else if (mGalleryProvider.size() == GalleryProvider.STATE_ERROR) {
                String error = mGalleryProvider.getError();
                if (error != null) {
                    return error;
                } else {
                    return getString(R.string.error_unknown);
                }
            } else if (mGalleryProvider.size() == 0) {
                return getString(R.string.error_empty);
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
                mGalleryProvider.request(mIndex);
                view.showInfo();
                view.setImage(null);
                view.setPage(mIndex + 1);
                view.setProgress(GalleryPageView.PROGRESS_INDETERMINATE);
                view.setError(null, null);
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
