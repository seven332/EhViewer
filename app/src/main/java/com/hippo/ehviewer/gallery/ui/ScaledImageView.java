package com.hippo.ehviewer.gallery.ui;

import android.graphics.Rect;
import android.graphics.RectF;

import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.TiledTexture;
import com.hippo.yorozuya.MathUtils;

public class ScaledImageView extends GLView {

    // TODO adjust scale max and min accroding to image size and screen size
    private static final float SCALE_MIN = 1 / 10.0f;
    private static final float SCALE_MAX = 10.0f;

    private TiledTexture mTiledTexture;

    // Corresponding source is [0, 0, mTiledTexture.getWidth(), mTiledTexture.getHeight()]
    private RectF mTarget = new RectF();
    private RectF mSeen = new RectF();
    private RectF mSourceAfterSeen = new RectF();
    private RectF mTargetAfterSeen = new RectF();
    private boolean mScaleOffsetDirty = true;
    private boolean mSeenDirty = true;

    private GalleryView.Scale mScaleMode;
    private GalleryView.StartPosition mStartPosition;
    private float mLastScale;
    private float mScale = 1f;

    public void setTiledTexture(TiledTexture tiledTexture) {
        mTiledTexture = tiledTexture;
        setScaleOffset(mScaleMode, mStartPosition, mLastScale);
        invalidate();
    }

    public TiledTexture getTiledTexture() {
        return mTiledTexture;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return Math.max(super.getSuggestedMinimumWidth(),
                mTiledTexture == null ? 0 : mTiledTexture.getWidth());
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return Math.max(super.getSuggestedMinimumHeight(),
                mTiledTexture == null ? 0 : mTiledTexture.getHeight());
    }

    /**
     *
     * If target is shorter then screen, make it in screen center. If target is
     * longer then parent, make sure target fill parent over
     */
    private void adjustPosition() {
        RectF target = mTarget;
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        float targetWidth = target.width();
        float targetHeight = target.height();

        if (targetWidth > screenWidth) {
            float fixXOffset = target.left;
            if (fixXOffset > 0) {
                target.left -= fixXOffset;
                target.right -= fixXOffset;
            } else if ((fixXOffset = screenWidth - target.right) > 0) {
                target.left += fixXOffset;
                target.right += fixXOffset;
            }
        } else {
            float left = (screenWidth - targetWidth) / 2;
            target.offsetTo(left, target.top);
        }
        if (targetHeight > screenHeight) {
            float fixYOffset = target.top;
            if (fixYOffset > 0) {
                target.top -= fixYOffset;
                target.bottom -= fixYOffset;
            } else if ((fixYOffset = screenHeight - target.bottom) > 0) {
                target.top += fixYOffset;
                target.bottom += fixYOffset;
            }
        } else {
            float top = (screenHeight - targetHeight) / 2;
            target.offsetTo(target.left, top);
        }
    }

    public void setScaleOffset(GalleryView.Scale scale,
            GalleryView.StartPosition startPosition, float lastScale) {
        mScaleMode = scale;
        mStartPosition = startPosition;
        mLastScale = lastScale;

        int screenWidth = getWidth();
        int screenHeight = getHeight();

        if (mTiledTexture == null || screenWidth == 0 || screenHeight == 0) {
            mScaleOffsetDirty = true;
            return;
        }

        int imageWidth = mTiledTexture.getWidth();
        int imageHeight = mTiledTexture.getHeight();

        // Set scale
        float targetWidth;
        float targetHeight;
        switch (scale) {
            case ORIGIN:
                mScale = 1.0f;
                targetWidth = imageWidth;
                targetHeight = imageHeight;
                break;
            case FIT_WIDTH:
                mScale = (float) screenWidth / imageWidth;
                targetWidth = screenWidth;
                targetHeight = imageHeight * mScale;
                break;
            case FIT_HEIGHT:
                mScale = (float) screenHeight / imageHeight;
                targetWidth = imageWidth * mScale;
                targetHeight = screenHeight;
                break;
            case FIT:
                float scaleX = (float) screenWidth / imageWidth;
                float scaleY = (float) screenHeight / imageHeight;
                if (scaleX < scaleY) {
                    mScale = scaleX;
                    targetWidth = screenWidth;
                    targetHeight = imageHeight * scaleX;
                } else {
                    mScale = scaleY;
                    targetWidth = imageWidth * scaleY;
                    targetHeight = screenHeight;
                    break;
                }
                break;
            case FIXED:
            default:
                mScale = lastScale;
                targetWidth = imageWidth * lastScale;
                targetHeight = imageHeight * lastScale;
                break;
        }

        // adjust scale, not too big, not too small
        if (mScale < SCALE_MIN) {
            mScale = SCALE_MIN;
            targetWidth = imageWidth * SCALE_MIN;
            targetHeight = imageHeight * SCALE_MIN;
        } else if (mScale > SCALE_MAX) {
            mScale = SCALE_MAX;
            targetWidth = imageWidth * SCALE_MAX;
            targetHeight = imageHeight * SCALE_MAX;
        }

        RectF target = mTarget;
        // set start position
        switch (startPosition) {
            case TOP_LEFT:
                target.left = 0;
                target.top = 0;
                break;
            case TOP_RIGHT:
                target.left = screenWidth - targetWidth;
                target.top = 0;
                break;
            case BOTTOM_LEFT:
                target.left = 0;
                target.top = screenHeight - targetHeight;
                break;
            case BOTTOM_RIGHT:
                target.left = screenWidth - targetWidth;
                target.top = screenHeight - targetHeight;
                break;
            case CENTER:
            default:
                target.left = (screenWidth - targetWidth) / 2;
                target.top = (screenHeight - targetHeight) / 2;
                break;
        }

        target.right = target.left + targetWidth;
        target.bottom = target.top + targetHeight;

        // adjust position
        adjustPosition();

        mScaleOffsetDirty = false;
        mSeenDirty = true;
    }

    public void scroll(int dx, int dy, int[] remain) {
        if (mTiledTexture == null) {
            remain[0] = dx;
            remain[1] = dy;
            return;
        }

        RectF target = mTarget;
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        float targetWidth = target.width();
        float targetHeight = target.height();

        if (targetWidth > screenWidth) {
            target.offset(-dx, 0);

            float fixXOffset = target.left;
            if (fixXOffset > 0) {
                target.left -= fixXOffset;
                target.right -= fixXOffset;
                remain[0] = -(int) fixXOffset;
            } else if ((fixXOffset = screenWidth - target.right) > 0) {
                target.left += fixXOffset;
                target.right += fixXOffset;
                remain[0] = (int) fixXOffset;
            } else {
                remain[0] = 0;
            }
        } else {
            remain[0] = dx;
        }
        if (targetHeight > screenHeight) {
            target.offset(0, -dy);

            float fixYOffset = target.top;
            if (fixYOffset > 0) {
                target.top -= fixYOffset;
                target.bottom -= fixYOffset;
                remain[1] = -(int) fixYOffset;
            } else if ((fixYOffset = screenHeight - target.bottom) > 0) {
                target.top += fixYOffset;
                target.bottom += fixYOffset;
                remain[1] = (int) fixYOffset;
            } else {
                remain[1] = 0;
            }
        } else {
            remain[1] = dy;
        }

        if (dx != remain[0] || dy != remain[1]) {
            mSeenDirty = true;
            invalidate();
        }
    }

    public void scale(float focusX, float focusY, float scale) {
        if (mTiledTexture == null) {
            return;
        }

        if ((mScale == SCALE_MAX && scale > 1f) || (mScale == SCALE_MIN && scale < 1f)) {
            return;
        }

        float newScale = mScale * scale;
        newScale = MathUtils.clamp(newScale, SCALE_MIN, SCALE_MAX);
        mScale = newScale;
        RectF target = mTarget;
        float left = (focusX - ((focusX - target.left) * scale));
        float top = (focusY - ((focusY - target.top) * scale));
        target.set(left, top,
                (left + (mTiledTexture.getWidth() * newScale)),
                (top + (mTiledTexture.getHeight() * newScale)));

        // adjust position
        adjustPosition();

        mSeenDirty = true;
        invalidate();
    }

    public float getScale() {
        return mScale;
    }

    public float getFitScale() {
        int screenWidth = getWidth();
        int screenHeight = getHeight();

        if (mTiledTexture == null || screenWidth == 0 || screenHeight == 0) {
            return 1f;
        }

        int imageWidth = mTiledTexture.getWidth();
        int imageHeight = mTiledTexture.getHeight();

        float scaleX = (float) screenWidth / imageWidth;
        float scaleY = (float) screenHeight / imageHeight;
        if (scaleX < scaleY) {
            return scaleX;
        } else {
            return scaleY;
        }
    }

    public boolean isLoaded() {
        return mTiledTexture != null;
    }

    public void setSeen(Rect seen) {
        mSeen.set(seen);
        mSeenDirty = true;
    }

    public void applySeen() {
        int width = mTiledTexture.getWidth();
        int height = mTiledTexture.getHeight();
        RectF target = mTarget;
        RectF targetAfterSeen = mTargetAfterSeen;
        RectF sourceAfterSeen = mSourceAfterSeen;

        targetAfterSeen.set(target);
        if (targetAfterSeen.intersect(mSeen)) {
            sourceAfterSeen.left = MathUtils.lerp(0, width,
                    MathUtils.delerp(target.left, target.right, targetAfterSeen.left));
            sourceAfterSeen.right = MathUtils.lerp(0, width,
                    MathUtils.delerp(target.left, target.right, targetAfterSeen.right));
            sourceAfterSeen.top = MathUtils.lerp(0, height,
                    MathUtils.delerp(target.top, target.bottom, targetAfterSeen.top));
            sourceAfterSeen.bottom = MathUtils.lerp(0, height,
                    MathUtils.delerp(target.top, target.bottom, targetAfterSeen.bottom));
        } else {
            // Can't be seen
            sourceAfterSeen.set(0f, 0f, 0f, 0f);
        }
    }

    @Override
    protected void onRender(GLCanvas canvas) {
        if (mTiledTexture != null) {

            if (mScaleOffsetDirty) {
                setScaleOffset(mScaleMode, mStartPosition, mLastScale);
            }
            if (mSeenDirty) {
                applySeen();
                mSeenDirty = false;
            }
            // Draw
            if (!mSourceAfterSeen.isEmpty()) {
                mTiledTexture.draw(canvas, mSourceAfterSeen, mTargetAfterSeen);
            }
        }
    }
}
