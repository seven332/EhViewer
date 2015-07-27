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

package com.hippo.ehviewer.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.hippo.app.StatsActivity;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.gallery.GalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProviderListener;
import com.hippo.ehviewer.gallery.GallerySpider;
import com.hippo.ehviewer.gallery.ImageHandler;
import com.hippo.ehviewer.gallery.glrenderer.TextTexture;
import com.hippo.ehviewer.gallery.glrenderer.TiledTexture;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.gallery.ui.GLView;
import com.hippo.ehviewer.gallery.ui.GalleryPageView;
import com.hippo.ehviewer.gallery.ui.GalleryView;
import com.hippo.util.SystemUiHelper;
import com.hippo.yorozuya.LayoutUtils;

import java.io.IOException;

public class GalleryActivity extends StatsActivity implements TiledTexture.OnFreeBitmapListener, GalleryProviderListener {

    public static final String KEY_GALLERY_BASE = "gallery_base";

    private SystemUiHelper mSystemUiHelper;

    private Resources mResources;

    private TiledTexture.Uploader mUploader;

    private GalleryView mGalleryView;

    private GalleryAdapter mAdapter;

    private TextTexture mTextTexture;

    private GallerySpider mGallerySpider;

    private GalleryBase mGalleryBase;

    private boolean mDieYoung;

    private GalleryView.Mode mMode;

    private int mSize;

    private boolean handleIntent(Intent intent) {
        if (intent != null) {
            mGalleryBase = intent.getParcelableExtra(KEY_GALLERY_BASE);
            return mGalleryBase != null;
        } else {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!handleIntent(getIntent())) {
            mDieYoung = true;
            finish();
            return;
        }

        try {
            mGallerySpider = GallerySpider.obtain(mGalleryBase, ImageHandler.Mode.DOWNLOAD);
        } catch (IOException e) {
            e.printStackTrace();
            mDieYoung = true;
            finish();
            return;
        }
        mGallerySpider.addGalleryProviderListener(this);

        mSystemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE,
                SystemUiHelper.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES | SystemUiHelper.FLAG_IMMERSIVE_STICKY);
        mSystemUiHelper.hide();

        setContentView(R.layout.activity_gallery);
        GLRootView glRootView = (GLRootView) findViewById(R.id.gl_root_view);

        mResources = getResources();
        // Prepare
        TiledTexture.prepareResources();
        TiledTexture.setOnFreeBitmapListener(this);
        mUploader = new TiledTexture.Uploader(glRootView);
        Typeface tf = Typeface.createFromAsset(mResources.getAssets(), "fonts/number.ttf");
        mTextTexture = TextTexture.create(tf,
                mResources.getDimensionPixelSize(R.dimen.gallery_index_text),
                mResources.getColor(R.color.secondary_text_dark),
                new char[]{'.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
        mUploader.addTexture(mTextTexture);
        GalleryPageView.setTextTexture(mTextTexture);

        mGalleryView = new GalleryView(this);
        mAdapter = new GalleryAdapter();
        mGalleryView.setAdapter(mAdapter);
        mGalleryView.setProgressSize(LayoutUtils.dp2pix(this, 56));
        mGalleryView.setInterval(LayoutUtils.dp2pix(this, 48));

        mSize = mGallerySpider.size();
        if (mSize <= 0) {
            setMode(GalleryView.Mode.NONE);
        } else {
            setMode(GalleryView.Mode.LEFT_TO_RIGHT);
        }

        glRootView.setContentPane(mGalleryView);
    }

    private void setMode(GalleryView.Mode mode) {
        mMode = mode;
        mGalleryView.setMode(mode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!mDieYoung) {
            GalleryPageView.setTextTexture(null);
            mTextTexture.recycle();
            TiledTexture.setOnFreeBitmapListener(null);
            TiledTexture.freeResources();
            mGallerySpider.removeGalleryProviderListener(this);
            GallerySpider.release(mGallerySpider);
            mGallerySpider = null;
        }
        // TODO free all TileTexture in GalleryView
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSystemUiHelper.hide();
    }

    @Override
    public void onFreeBitmapListener(Bitmap bitmap) {
        if (mGallerySpider != null) {
            mGallerySpider.releaseBitmap(bitmap);
        }
    }

    @Override
    public void onTotallyFailed(Exception e) {
        //e.printStackTrace(); // TODO
    }

    @Override
    public void onPartlyFailed(Exception e) {
        //e.printStackTrace(); // TODO
    }

    @Override
    public void onGetSize(int size) {
        if (mSize != size) {
            mSize = size;
            setMode(GalleryView.Mode.LEFT_TO_RIGHT);
        }
    }

    @Override
    public void onGetBitmap(int index, @Nullable Bitmap bitmap) {
        GalleryPageView page = mGalleryView.getPage(index);
        if (page != null) {
            if (bitmap == null) {
                bindBadBitmap(page);
            } else {
                bindBitmap(page, bitmap);
            }
        } else {
            if (mGallerySpider != null) {
                mGallerySpider.releaseBitmap(bitmap);
            }
        }
    }

    @Override
    public void onPagePercent(int index, float percent) {
        GalleryPageView page = mGalleryView.getPage(index);
        if (page != null) {
            bindPercent(page, percent);
        }
    }

    @Override
    public void onPageSucceed(int index) {
        GalleryPageView page = mGalleryView.getPage(index);
        if (page != null) {
            bind(page, index);
        }
    }

    @Override
    public void onPageFailed(int index, Exception e) {
        //e.printStackTrace(); // TODO
        GalleryPageView page = mGalleryView.getPage(index);
        if (page != null) {
            bindFailed(page);
        }
    }

    private void clearTiledTextureInPage(GalleryPageView view) {
        TiledTexture tiledTexture = view.mImageView.getTiledTexture();
        if (tiledTexture != null) {
            view.mImageView.setTiledTexture(null);
            tiledTexture.recycle();
        }
    }

    private void bindBadBitmap(GalleryPageView view) {
        clearTiledTextureInPage(view);
        view.mProgressView.setVisibility(GLView.INVISIBLE);
        view.mIndexView.setVisibility(GLView.VISIBLE);
        view.mIndexView.setText(".3");
    }

    private void bindBitmap(GalleryPageView view, Bitmap bitmap) {
        TiledTexture tiledTexture = new TiledTexture(bitmap);
        mUploader.addTexture(tiledTexture);

        clearTiledTextureInPage(view);
        view.mProgressView.setVisibility(GLView.INVISIBLE);
        view.mIndexView.setVisibility(GLView.INVISIBLE);
        view.mImageView.setTiledTexture(tiledTexture);
    }

    private void bindPercent(GalleryPageView view, float precent) {
        clearTiledTextureInPage(view);
        view.mProgressView.setVisibility(GLView.VISIBLE);
        view.mProgressView.setIndeterminate(false);
        view.mProgressView.setProgress(precent);
        view.mIndexView.setVisibility(GLView.VISIBLE);
    }

    private void bindNone(GalleryPageView view) {
        clearTiledTextureInPage(view);
        view.mProgressView.setVisibility(GLView.VISIBLE);
        view.mProgressView.setIndeterminate(true);
        view.mIndexView.setVisibility(GLView.VISIBLE);
    }

    private void bindFailed(GalleryPageView view) {
        clearTiledTextureInPage(view);
        view.mProgressView.setVisibility(GLView.INVISIBLE);
        view.mIndexView.setVisibility(GLView.VISIBLE);
        view.mIndexView.setText(".1");
    }

    private void bindUnknown(GalleryPageView view) {
        clearTiledTextureInPage(view);
        view.mProgressView.setVisibility(GLView.INVISIBLE);
        view.mIndexView.setVisibility(GLView.VISIBLE);
        view.mIndexView.setText(".2");
    }

    private void bind(GalleryPageView view, int index) {
        Object result = mGallerySpider.request(index);
        if (result instanceof Float) {
            bindPercent(view, (Float) result);
        } else if (result == GalleryProvider.RESULT_WAIT) {
            // Just wait
        } else if (result == GalleryProvider.RESULT_NONE) {
            bindNone(view);
        } else if (result == GalleryProvider.RESULT_FAILED) {
            bindFailed(view);
        } else {
            bindUnknown(view);
        }
    }

    class GalleryAdapter extends GalleryView.Adapter {

        @Override
        public int getPages() {
            return mMode == GalleryView.Mode.NONE ? 1 : mSize;
        }

        @Override
        public GalleryPageView createPage() {
            return new GalleryPageView(GalleryActivity.this);
        }

        @Override
        public void bindPage(GalleryPageView view, int index) {
            view.mProgressView.setColor(mResources.getColor(R.color.theme_accent));
            view.mIndexView.setText(Integer.toString(index + 1));
            bind(view, index);
        }

        @Override
        public void unbindPage(GalleryPageView view, int index) {
            view.mProgressView.setIndeterminate(false);
            clearTiledTextureInPage(view);
        }
    }
}
