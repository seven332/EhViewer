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
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.util.SparseArray;

import com.hippo.app.StatsActivity;
import com.hippo.conaco.BitmapPool;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.gallery.GalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProviderListener;
import com.hippo.ehviewer.gallery.GallerySpider;
import com.hippo.ehviewer.gallery.GifDecoder;
import com.hippo.ehviewer.gallery.GifDecoderBuilder;
import com.hippo.ehviewer.gallery.ImageHandler;
import com.hippo.ehviewer.gallery.ZipGalleryProvider;
import com.hippo.ehviewer.gallery.glrenderer.GifTexture;
import com.hippo.ehviewer.gallery.glrenderer.InfiniteThreadExecutor;
import com.hippo.ehviewer.gallery.glrenderer.TextTexture;
import com.hippo.ehviewer.gallery.glrenderer.TiledTexture;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.gallery.ui.GLView;
import com.hippo.ehviewer.gallery.ui.GalleryLayout;
import com.hippo.ehviewer.gallery.ui.GalleryPageView;
import com.hippo.ehviewer.gallery.ui.GalleryPanel;
import com.hippo.ehviewer.gallery.ui.GalleryView;
import com.hippo.util.SystemUiHelper;
import com.hippo.yorozuya.PriorityThreadFactory;
import com.hippo.yorozuya.SimpleHandler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class GalleryActivity extends StatsActivity implements TiledTexture.OnFreeBitmapListener,
        GalleryProviderListener, GalleryLayout.ActionListener {

    public static final String ACTION_GALLERY_FROM_GALLERY_BASE = "com.hippo.ehviewer.gallery.action.GALLERY_FROM_GALLERY_BASE";
    public static final String ACTION_GALLERY_FROM_ARCHIVE = "com.hippo.ehviewer.gallery.action.GALLERY_FROM_ARCHIVE";

    public static final String KEY_GALLERY_BASE = "gallery_base";
    public static final String KEY_ARCHIVE_URI = "archive_uri";

    public static final String KEY_HASH_CODE = "hash_code";
    public static final SparseArray<GalleryProvider> sGalleryProviderMap = new SparseArray<>();
    public static final SparseArray<CloseGalleryProviderTask> sCloseGalleryProviderTaskMap = new SparseArray<>();

    private SystemUiHelper mSystemUiHelper;

    private Resources mResources;

    private TiledTexture.Uploader mUploader;
    private InfiniteThreadExecutor mThreadExecutor;

    private GLRootView mGLRootView;
    private GalleryView mGalleryView;
    private GalleryPanel mGalleryPanel;

    private GalleryAdapter mAdapter;

    private TextTexture mGalleryIndexTextTexture;
    private TextTexture mGalleryProgressTextTexture;

    private GalleryProvider mGalleryProvider;

    private boolean mDieYoung;

    private GalleryView.Mode mMode;

    private int mSize;

    private BitmapPool mGifBitmapPool;

    private boolean getGalleryProvider(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_GALLERY_FROM_GALLERY_BASE.equals(action)) {
                GalleryBase galleryBase = intent.getParcelableExtra(KEY_GALLERY_BASE);
                if (galleryBase != null) {
                    try {
                        // TODO get mode
                        mGalleryProvider = GallerySpider.obtain(galleryBase, ImageHandler.Mode.DOWNLOAD);
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (ACTION_GALLERY_FROM_ARCHIVE.equals(action)) {
                Uri uri = intent.getParcelableExtra(KEY_ARCHIVE_URI);
                if (uri != null) {
                    try {
                        // TODO support other archive
                        mGalleryProvider = new ZipGalleryProvider(new File(uri.getPath()));
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            int hashCode = savedInstanceState.getInt(KEY_HASH_CODE, 0);
            if (hashCode != 0) {
                mGalleryProvider = sGalleryProviderMap.get(hashCode);
                sGalleryProviderMap.remove(hashCode);
                Runnable runnable = sCloseGalleryProviderTaskMap.get(hashCode);
                sCloseGalleryProviderTaskMap.remove(hashCode);
                if (runnable != null) {
                    SimpleHandler.getInstance().removeCallbacks(runnable);
                }
            }
        }

        if (mGalleryProvider == null && !getGalleryProvider(getIntent())) {
            mDieYoung = true;
            finish();
            return;
        }

        mSystemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE,
                SystemUiHelper.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES | SystemUiHelper.FLAG_IMMERSIVE_STICKY);
        mSystemUiHelper.hide();

        mResources = getResources();

        // Gallery index typeface
        Typeface galleryIndexTf = Typeface.createFromAsset(mResources.getAssets(), "fonts/gallery_index.ttf");
        mGalleryIndexTextTexture = TextTexture.create(galleryIndexTf,
                mResources.getDimensionPixelSize(R.dimen.gallery_index_text),
                mResources.getColor(R.color.secondary_text_dark),
                new char[]{'.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
        GalleryPageView.setTextTexture(mGalleryIndexTextTexture);
        // Gallery progress typeface
        Typeface galleryProgressTf = Typeface.createFromAsset(mResources.getAssets(), "fonts/gallery_progress.ttf");
        mGalleryProgressTextTexture = TextTexture.create(galleryProgressTf,
                mResources.getDimensionPixelSize(R.dimen.text_little_small),
                mResources.getColor(R.color.primary_text_dark),
                new char[]{'/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
        GalleryPanel.setTextTexture(mGalleryProgressTextTexture);

        setContentView(R.layout.activity_gallery);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);

        // Prepare
        TiledTexture.prepareResources();
        TiledTexture.setOnFreeBitmapListener(this);
        mUploader = new TiledTexture.Uploader(mGLRootView);
        mThreadExecutor = new InfiniteThreadExecutor(3000, new LinkedBlockingQueue<Runnable>(),
                new PriorityThreadFactory("GifDecode", Process.THREAD_PRIORITY_BACKGROUND));
        GifTexture.initialize(mUploader, mThreadExecutor);
        mGifBitmapPool = new BitmapPool();
        GifDecoder.setBitmapPool(mGifBitmapPool);

        mUploader.addTexture(mGalleryIndexTextTexture);
        mUploader.addTexture(mGalleryProgressTextTexture);

        GalleryLayout galleryLayout = new GalleryLayout(this, this);
        mGalleryView = galleryLayout.getGalleryView();
        mGalleryPanel = galleryLayout.getGalleryPanel();
        mAdapter = new GalleryAdapter();
        mGalleryView.setAdapter(mAdapter);

        mSize = mGalleryProvider.size();
        if (mSize <= 0) {
            setMode(GalleryView.Mode.NONE);
        } else {
            setMode(GalleryView.Mode.LEFT_TO_RIGHT);
        }
        mGalleryProvider.addGalleryProviderListener(this);

        mGLRootView.setContentPane(galleryLayout);
    }

    private void setMode(GalleryView.Mode mode) {
        mMode = mode;
        mGalleryView.setMode(mode);
    }

    @Override
    public void finish() {
        super.finish();
    }

    private static void closeGalleryProvider(GalleryProvider galleryProvider) {
        if (galleryProvider instanceof GallerySpider) {
            GallerySpider.release((GallerySpider) galleryProvider);
        } else if (galleryProvider instanceof ZipGalleryProvider) {
            ((ZipGalleryProvider) galleryProvider).stop();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_HASH_CODE, hashCode());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!mDieYoung) {
            GalleryPageView.setTextTexture(null);
            mUploader.clear();
            mGalleryIndexTextTexture.recycle();
            mGalleryProgressTextTexture.recycle();

            GifDecoder.setBitmapPool(null);
            GifTexture.uninitialize();
            TiledTexture.setOnFreeBitmapListener(null);
            TiledTexture.freeResources();
            mGalleryProvider.removeGalleryProviderListener(this);

            // Post close task
            int hashCode = hashCode();
            CloseGalleryProviderTask task = new CloseGalleryProviderTask(mGalleryProvider, hashCode);
            sGalleryProviderMap.put(hashCode, mGalleryProvider);
            sCloseGalleryProviderTaskMap.put(hashCode, task);
            SimpleHandler.getInstance().postDelayed(task, 3000);

            mGalleryProvider = null;

            // Free all TileTexture in GalleryView
            for (int i = 0, n = mGalleryView.getComponentCount(); i < n; i++) {
                GLView view = mGalleryView.getComponent(i);
                if (view instanceof  GalleryPageView) {
                    clearTiledTextureInPage((GalleryPageView) view);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSystemUiHelper.hide();
    }

    @Override
    public void onFreeBitmap(TiledTexture tiledTexture, Bitmap bitmap) {
        if (tiledTexture instanceof GifTexture) {
            if (mGifBitmapPool != null ) {
                mGifBitmapPool.addReusableBitmap(bitmap);
            }
        } else {
            if (mGalleryProvider != null) {
                mGalleryProvider.releaseBitmap(bitmap);
            }
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

    public void releaseImage(Object obj) {
        if (obj != null) {
            if (obj instanceof Bitmap) {
                if (mGalleryProvider != null) {
                    mGalleryProvider.releaseBitmap((Bitmap) obj);
                }
            } else if (obj instanceof Pair) {
                Pair pair = (Pair) obj;
                ((GifDecoderBuilder) pair.first).close();
                if (mGifBitmapPool != null) {
                    mGifBitmapPool.addReusableBitmap((Bitmap) pair.second);
                }
            }
        }
    }

    @Override
    public void onGetImage(int index, @Nullable Object obj) {
        GalleryPageView page = mGalleryView.getPage(index);
        if (page != null) {
            if (obj == null) {
                bindBadImage(page);
            } else {
                bindImage(page, obj);
            }
        } else {
            releaseImage(obj);
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

    private void bindBadImage(GalleryPageView view) {
        clearTiledTextureInPage(view);
        view.mProgressView.setVisibility(GLView.INVISIBLE);
        view.mIndexView.setVisibility(GLView.VISIBLE);
        view.mIndexView.setText(".3");
    }

    private void bindImage(GalleryPageView view, Object obj) {
        TiledTexture tiledTexture;
        if (obj instanceof Bitmap) {
            tiledTexture = new TiledTexture((Bitmap) obj);
            mUploader.addTexture(tiledTexture);
        } else if (obj instanceof Pair) {
            Pair pair = (Pair) obj;
            tiledTexture = new GifTexture((GifDecoderBuilder) pair.first, (Bitmap) pair.second);
            mUploader.addTexture(tiledTexture);
        } else {
            bindBadImage(view);
            return;
        }

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

    private void bindWait(GalleryPageView view) {
        clearTiledTextureInPage(view);
        view.mProgressView.setVisibility(GLView.INVISIBLE);
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
        Object result = mGalleryProvider.request(index);
        if (result instanceof Float) {
            bindPercent(view, (Float) result);
        } else if (result == GalleryProvider.RESULT_WAIT) {
            bindWait(view);
        } else if (result == GalleryProvider.RESULT_NONE) {
            bindNone(view);
        } else if (result == GalleryProvider.RESULT_FAILED) {
            bindFailed(view);
        } else {
            bindUnknown(view);
        }
    }

    @Override
    public void onShowGalleryPanel() {
        mSystemUiHelper.show();
    }

    @Override
    public void onHideGalleryPanel() {
        mSystemUiHelper.hide();
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

    static class CloseGalleryProviderTask implements Runnable {

        private GalleryProvider mGalleryProvider;
        private int mHashCode;

        public CloseGalleryProviderTask(GalleryProvider galleryProvider, int hashCode) {
            mGalleryProvider = galleryProvider;
            mHashCode = hashCode;
        }

        @Override
        public void run() {
            closeGalleryProvider(mGalleryProvider);
            mGalleryProvider = null;
            sGalleryProviderMap.remove(mHashCode);
            sCloseGalleryProviderTaskMap.remove(mHashCode);
        }
    }
}
