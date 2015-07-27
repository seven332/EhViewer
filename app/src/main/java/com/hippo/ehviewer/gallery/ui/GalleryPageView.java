package com.hippo.ehviewer.gallery.ui;

import android.content.Context;
import android.graphics.Rect;

import com.hippo.ehviewer.gallery.glrenderer.TextTexture;
import com.hippo.yorozuya.LayoutUtils;

public class GalleryPageView extends FrameView {

    public ScaledImageView mImageView;
    public LinearView mLinearView;
    public TextView mIndexView;
    public ProgressView mProgressView;

    private Rect mSeen = new Rect();

    private static TextTexture sTextTexture;

    public static void setTextTexture(TextTexture textTexture) {
        sTextTexture = textTexture;
    }

    public GalleryPageView(Context context) {
        mImageView = new ScaledImageView();
        mLinearView = new LinearView();
        mLinearView.setInterval(LayoutUtils.dp2pix(context, 24)); // 12dp
        mIndexView = new TextView(sTextTexture);
        mProgressView = new ProgressView();
        mProgressView.setMinimumWidth(LayoutUtils.dp2pix(context, 56)); // 56dp
        mProgressView.setMinimumHeight(LayoutUtils.dp2pix(context, 56)); // 56dp

        GravityLayoutParams lp = new GravityLayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        mLinearView.addComponent(mIndexView, lp);
        lp = new GravityLayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        mLinearView.addComponent(mProgressView, lp);

        lp = new GravityLayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        addComponent(mImageView, lp);

        lp = new GravityLayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        addComponent(mLinearView, lp);
    }

    @SuppressWarnings("ResourceType")
    private void updateSeen() {
        Rect bounds = bounds();
        Rect parentBounds = mParent.bounds();
        mSeen.set(bounds);
        if (!mSeen.intersect(0, 0, parentBounds.width(), parentBounds.height())) {
            mSeen.set(0, 0, 0, 0);
        } else {
            mSeen.offset(-bounds.left, -bounds.top);
        }
        mImageView.setSeen(mSeen);
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        super.onLayout(changeSize, left, top, right, bottom);
        updateSeen();
    }

    @Override
    public void offsetLeftAndRight(int offset) {
        super.offsetLeftAndRight(offset);
        updateSeen();
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        super.offsetTopAndBottom(offset);
        updateSeen();
    }

    public void setScaleOffset(GalleryView.Scale scale,
            GalleryView.StartPosition startPosition, float lastScale) {
        mImageView.setScaleOffset(scale, startPosition, lastScale);
    }

    public void scroll(int dx, int dy, int[] remain) {
        mImageView.scroll(dx, dy, remain);
    }

    public void scale(float focusX, float focusY, float scale) {
        mImageView.scale(focusX, focusY, scale);
    }
}
