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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.gallery.GalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProviderListener;
import com.hippo.ehviewer.gallery.GallerySpider;
import com.hippo.ehviewer.gallery.ImageHandler;
import com.hippo.ehviewer.gallery.widget.GalleryView;
import com.hippo.util.SystemUiHelper;
import com.hippo.widget.ProgressView;
import com.hippo.yorozuya.Say;

import java.io.IOException;

// TODO Adjust RecyclerView recycle logic or Create a bitmap map here
public class GalleryActivity extends AppCompatActivity implements GalleryProviderListener {

    public static final String KEY_GALLERY_BASE = "gallery_base";

    private SystemUiHelper mSystemUiHelper;

    private GalleryView mGalleryView;

    private GalleryAdapter mAdapter;

    private GallerySpider mGallerySpider;

    private GalleryBase mGalleryBase;

    private int mSize;

    private boolean mReady = false;

    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        mGalleryBase = intent.getParcelableExtra(KEY_GALLERY_BASE);
        if (mGalleryBase == null) {
            finish();
            return;
        }

        try {
            mGallerySpider = GallerySpider.obtain(mGalleryBase, ImageHandler.Mode.READ);
        } catch (IOException e) {
            throw new IllegalStateException(e); // TODO
        }
        mGallerySpider.addGalleryProviderListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSystemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE,
                SystemUiHelper.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES | SystemUiHelper.FLAG_IMMERSIVE_STICKY);
        mSystemUiHelper.hide();

        setContentView(R.layout.activity_gallery);

        mGalleryView = (GalleryView) findViewById(R.id.gallery_view);

        mAdapter = new GalleryAdapter();
        mGalleryView.setAdapter(mAdapter);

        handleIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mGallerySpider.removeGalleryProviderListener(this);
        if (mGallerySpider != null) {
            GallerySpider.release(mGallerySpider);
            mGallerySpider = null;
        }
        // TODO free all bitmap in ImageView
    }

    @Override
    public void onTotallyFailed(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onPartlyFailed(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onGetSize(int size) {

        Say.d("TAG", "size = " + size);

        mReady = true;
        mSize = size;

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onGetBitmap(int index, @Nullable Bitmap bitmap) {
        GalleryHolder holder = (GalleryHolder) mGalleryView.findViewHolderForAdapterPosition(index);
        if (holder == null) {
            mGallerySpider.releaseBitmap(bitmap);
        } else {
            if (bitmap == null) {
                clearDrawable(holder.imageView, getResources().getDrawable(R.drawable.ic_sad));
                holder.imageView.setVisibility(View.VISIBLE);
                holder.progressView.setVisibility(View.GONE);
            } else {
                clearDrawable(holder.imageView, new BitmapDrawable(getResources(), bitmap));
                holder.imageView.setVisibility(View.VISIBLE);
                holder.progressView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPagePercent(int index, float percent) {
        GalleryHolder holder = (GalleryHolder) mGalleryView.findViewHolderForAdapterPosition(index);
        if (holder != null) {
            clearDrawable(holder.imageView, null);
            holder.imageView.setVisibility(View.GONE);
            ProgressView progressView = holder.progressView;
            progressView.setVisibility(View.VISIBLE);
            progressView.setIndeterminate(false);
            progressView.setProgress(percent);
        }
    }

    @Override
    public void onPageSucceed(int index) {
        GalleryHolder holder = (GalleryHolder) mGalleryView.findViewHolderForAdapterPosition(index);
        if (holder != null) {
            bind(holder, index);
        }
    }

    @Override
    public void onPageFailed(int index, Exception e) {
        e.printStackTrace();

        GalleryHolder holder = (GalleryHolder) mGalleryView.findViewHolderForAdapterPosition(index);
        if (holder != null) {
            clearDrawable(holder.imageView, null);
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_sad)); // TODO
            holder.progressView.setVisibility(View.GONE);
        }
    }

    private void bind(GalleryHolder holder, int index) {
        clearDrawable(holder.imageView, null);

        if (mReady) {
            Object result = mGallerySpider.request(index);
            if (result instanceof Float) {
                holder.imageView.setVisibility(View.GONE);
                ProgressView progressView = holder.progressView;
                progressView.setVisibility(View.VISIBLE);
                progressView.setIndeterminate(false);
                progressView.setProgress((Float) result);

            } else if (result == GalleryProvider.RESULT_NONE) {
                holder.imageView.setVisibility(View.GONE);
                ProgressView progressView = holder.progressView;
                progressView.setVisibility(View.VISIBLE);
                progressView.setIndeterminate(true);

            } else if (result == GalleryProvider.RESULT_WAIT) {
                holder.imageView.setVisibility(View.GONE);
                holder.progressView.setVisibility(View.GONE);

            } else if (result == GalleryProvider.RESULT_FAILED) {
                holder.imageView.setVisibility(View.VISIBLE);
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_sad)); // TODO
                holder.progressView.setVisibility(View.GONE);

            } else {
                holder.imageView.setVisibility(View.VISIBLE);
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_heart)); // TODO
                holder.progressView.setVisibility(View.GONE);
            }

        } else {
            holder.imageView.setVisibility(View.GONE);
            holder.progressView.setVisibility(View.VISIBLE);
            holder.progressView.setIndeterminate(true);
        }
    }

    private void clearDrawable(ImageView imageView, Drawable drawable) {
        Drawable oldDrawable = imageView.getDrawable();
        if (oldDrawable instanceof BitmapDrawable) {
            mGallerySpider.releaseBitmap(((BitmapDrawable) oldDrawable).getBitmap());
        }
        imageView.setImageDrawable(drawable);
    }

    class GalleryHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public ProgressView progressView;

        public GalleryHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.image);
            progressView = (ProgressView) itemView.findViewById(R.id.progress);
        }
    }

    class GalleryAdapter extends RecyclerView.Adapter<GalleryHolder> {

        @Override
        public GalleryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new GalleryHolder(getLayoutInflater().inflate(
                    R.layout.item_gallery, parent, false));
        }

        @Override
        public void onBindViewHolder(GalleryHolder holder, int position) {
            bind(holder, position);
        }

        @Override
        public int getItemCount() {
            return mReady ? mSize : 1;
        }
    }
}
