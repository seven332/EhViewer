package com.hippo.ehviewer.gallery.gifdecoder;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.hippo.conaco.BitmapPool;

public class GifBitmapProvider implements GifDecoder.BitmapProvider {

    private BitmapPool mBitmapPool;
    private ByteArrayPool mByteArrayPool;

    public GifBitmapProvider(BitmapPool bitmapPool, ByteArrayPool byteArrayPool) {
        mBitmapPool = bitmapPool;
        mByteArrayPool = byteArrayPool;
    }

    @NonNull
    @Override
    public Bitmap obtain(int width, int height) {
        Bitmap bitmap = mBitmapPool.getBitmap(width, height);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setHasAlpha(true);
        }
        return bitmap;
    }

    @Override
    public void release(Bitmap bitmap) {
        mBitmapPool.addReusableBitmap(bitmap);
    }

    @Override
    public byte[] obtainByteArray(int size) {
        byte[] bytes = mByteArrayPool.getAtLeast(size);
        if (bytes == null) {
            bytes = new byte[size];
        }
        return bytes;
    }

    @Override
    public void release(byte[] bytes) {
        mByteArrayPool.add(bytes);
    }
}
