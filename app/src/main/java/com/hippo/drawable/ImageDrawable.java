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

package com.hippo.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.hippo.yorozuya.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class ImageDrawable extends Drawable implements ImageWrapper.Callback, Animatable {

    private static final int TILE_SIZE = 512;

    private ImageWrapper mImageWrapper;
    private Paint mPaint;

    private List<Tile> mTileList;

    private static class Tile {
        Bitmap bitmap;
        int w;
        int h;
        int x;
        int y;
    }

    private static final BitmapPool sBitmapPool = new BitmapPool();

    public ImageDrawable(@NonNull ImageWrapper imageWrapper) {
        mImageWrapper = imageWrapper;
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mTileList = createTileArray();

        // Render first frame
        render();

        // Add callback
        imageWrapper.addCallback(this);
    }

    private List<Tile> createTileArray() {
        int width = mImageWrapper.getWidth();
        int height = mImageWrapper.getHeight();
        int capacity = MathUtils.clamp(MathUtils.ceilDivide(width, TILE_SIZE) *
                MathUtils.ceilDivide(height, TILE_SIZE), 0, 100);
        List<Tile> tiles = new ArrayList<>(capacity);

        for (int x = 0; x < width; x += TILE_SIZE) {
            int w = Math.min(TILE_SIZE, width - x);
            for (int y = 0; y < height; y += TILE_SIZE) {
                int h = Math.min(TILE_SIZE, height - y);
                Tile tile = new Tile();
                tile.x = x;
                tile.y = y;
                tile.w = w;
                tile.h = h;
                tile.bitmap = sBitmapPool.get(w, h);
                tiles.add(tile);
            }
        }

        return tiles;
    }

    private void render() {
        ImageWrapper imageWrapper = mImageWrapper;
        if (imageWrapper.isRecycled()) {
            return;
        }

        List<Tile> tiles = mTileList;
        for (int i = 0, length = tiles.size(); i < length; i++) {
            Tile tile = tiles.get(i);
            if (tile.bitmap != null) {
                imageWrapper.render(tile.x, tile.y, tile.bitmap, 0, 0, tile.w, tile.h, false, 0);
            }
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return mImageWrapper.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mImageWrapper.getHeight();
    }

    @Override
    public void draw(Canvas canvas) {
        List<Tile> tiles = mTileList;
        for (int i = 0, length = tiles.size(); i < length; i++) {
            Tile tile = tiles.get(i);
            if (tile.bitmap != null) {
                canvas.drawBitmap(tile.bitmap, tile.x, tile.y, mPaint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void recycle() {
        mImageWrapper.removeCallback(this);

        // Free tile's bitmap
        List<Tile> tiles = mTileList;
        for (int i = 0, length = tiles.size(); i < length; i++) {
            Tile tile = tiles.get(i);
            sBitmapPool.put(tile.bitmap);
            tile.bitmap = null;
        }
    }

    @Override
    public void renderImage(ImageWrapper who) {
        render();
    }

    @Override
    public void invalidateImage(ImageWrapper who) {
        invalidateSelf();
    }

    @Override
    public void start() {
        mImageWrapper.start();
    }

    @Override
    public void stop() {
        mImageWrapper.stop();
    }

    @Override
    public boolean isRunning() {
        return mImageWrapper.isRunning();
    }
}
