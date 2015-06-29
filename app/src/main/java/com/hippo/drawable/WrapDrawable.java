package com.hippo.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class WrapDrawable extends Drawable {

    private Drawable mDrawable;

    public void setDrawable(Drawable drawable) {
        mDrawable = drawable;
    }

    public void updateBounds() {
        setBounds(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        if (mDrawable != null) {
            mDrawable.draw(canvas);
        }
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);

        if (mDrawable != null) {
            mDrawable.setBounds(left, top, right, bottom);
        }
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);

        if (mDrawable != null) {
            mDrawable.setBounds(bounds);
        }
    }

    @Override
    public void setChangingConfigurations(int configs) {
        super.setChangingConfigurations(configs);

        if (mDrawable != null) {
            mDrawable.setChangingConfigurations(configs);
        }
    }

    @Override
    public int getChangingConfigurations() {
        if (mDrawable != null) {
            return mDrawable.getChangingConfigurations();
        } else {
            return super.getChangingConfigurations();
        }
    }

    @Override
    public void setDither(boolean dither) {
        super.setDither(dither);

        if (mDrawable != null) {
            mDrawable.setDither(dither);
        }
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        super.setFilterBitmap(filter);

        if (mDrawable != null) {
            mDrawable.setFilterBitmap(filter);
        }
    }


    @Override
    public void setAlpha(int alpha) {
        if (mDrawable != null) {
            mDrawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (mDrawable != null) {
            mDrawable.setColorFilter(cf);
        }
    }

    @Override
    public int getOpacity() {
        if (mDrawable != null) {
            return mDrawable.getOpacity();
        } else {
            return PixelFormat.UNKNOWN;
        }
    }

    @Override
    public int getIntrinsicWidth() {
        if (mDrawable != null) {
            return mDrawable.getIntrinsicWidth();
        } else {
            return super.getIntrinsicWidth();
        }
    }

    @Override
    public int getIntrinsicHeight() {
        if (mDrawable != null) {
            return mDrawable.getIntrinsicHeight();
        } else {
            return super.getIntrinsicHeight();
        }
    }
}
