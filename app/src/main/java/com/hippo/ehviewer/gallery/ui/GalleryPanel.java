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

package com.hippo.ehviewer.gallery.ui;

import android.content.Context;
import android.view.MotionEvent;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.anim.FloatAnimation;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.TextTexture;
import com.hippo.ehviewer.gallery.glrenderer.XmlResourceTexture;
import com.hippo.util.AnimationUtils;
import com.hippo.widget.FitPaddingImpl;
import com.hippo.yorozuya.LayoutUtils;

public class GalleryPanel extends GLView implements FitPaddingImpl, Slider.OnSetProgressListener {

    private Context mContext;

    private LinearLayout mRightView;
    private XmlResourceView mScreenRotation;
    private XmlResourceView mRefresh;
    private XmlResourceView mShare;

    private LinearLayout mBottomView;
    private TextView mProgressTextView;
    private TextView mPagesTextView;
    private Slider mSlider;

    private float mShowPercent = 0.0f;

    private int mEnd = 0;

    private ActionListener mListener;

    private FloatAnimation mAnimation;
    private boolean mShown = false;

    private static TextTexture sTextTexture;

    public static void setTextTexture(TextTexture textTexture) {
        sTextTexture = textTexture;
    }

    public GalleryPanel(Context context, ActionListener listener) {
        mContext = context;
        mListener = listener;

        int background = context.getResources().getColor(R.color.gallery_panel_background);
        LinearLayout.LayoutParams lp;

        int dp8 = LayoutUtils.dp2pix(context, 8);
        mRightView = new LinearLayout();
        mRightView.setOrientation(LinearLayout.VERTICAL);
        mRightView.setBackgroundColor(background);
        mRightView.setInterval(dp8);
        mRightView.setPaddings(dp8, dp8, dp8, dp8);
        mScreenRotation = new XmlResourceView();
        XmlResourceTexture screenRotationTexture = new XmlResourceTexture(context, R.drawable.ic_screen_rotation_dark_x48);
        mScreenRotation.setTexture(screenRotationTexture);
        mRefresh = new XmlResourceView();
        XmlResourceTexture refreshTexture = new XmlResourceTexture(context, R.drawable.ic_refresh_dark_x48);
        mRefresh.setTexture(refreshTexture);
        mShare = new XmlResourceView();
        XmlResourceTexture shareTexture = new XmlResourceTexture(context, R.drawable.ic_share_dark_x48);
        mShare.setTexture(shareTexture);

        int actionSize = LayoutUtils.dp2pix(context, 48);
        lp = new LinearLayout.LayoutParams(
                actionSize, actionSize);
        mRightView.addComponent(mScreenRotation, lp);
        lp = new LinearLayout.LayoutParams(
                actionSize, actionSize);
        mRightView.addComponent(mRefresh, lp);
        lp = new LinearLayout.LayoutParams(
                actionSize, actionSize);
        mRightView.addComponent(mShare, lp);

        mBottomView = new LinearLayout();
        mBottomView.setOrientation(LinearLayout.HORIZONTAL);
        mBottomView.setBackgroundColor(background);
        mBottomView.setPaddingLeft(LayoutUtils.dp2pix(context, 8));
        mBottomView.setPaddingRight(LayoutUtils.dp2pix(context, 8));
        mProgressTextView = new TextView(sTextTexture);
        mProgressTextView.setGravity(Gravity.CENTER);
        mPagesTextView = new TextView(sTextTexture);
        mPagesTextView.setGravity(Gravity.CENTER);
        mSlider = new Slider(context);
        mSlider.setPaddings(LayoutUtils.dp2pix(context, 8), LayoutUtils.dp2pix(context, 18),
                LayoutUtils.dp2pix(context, 8), LayoutUtils.dp2pix(context, 18));
        mSlider.setColor(context.getResources().getColor(R.color.theme_primary));
        mSlider.setOnSetProgressListener(this);

        lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_VERTICAL;
        mBottomView.addComponent(mProgressTextView, lp);
        lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_VERTICAL;
        lp.weight = 1;
        mBottomView.addComponent(mSlider, lp);
        lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_VERTICAL;
        mBottomView.addComponent(mPagesTextView, lp);

        addComponent(mRightView, new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addComponent(mBottomView, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mAnimation = new FloatAnimation() {
            @Override
            protected void onCalculate(float progress) {
                super.onCalculate(progress);
                mShowPercent = get();
                mRightView.offsetLeftAndRight(getWidth() - (int) (mRightView.getWidth() * mShowPercent) - mRightView.bounds().left);
                mBottomView.offsetTopAndBottom(getHeight() - (int) (mBottomView.getMeasuredHeight() * mShowPercent) - mBottomView.bounds().top);
            }
        };
        mAnimation.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
    }

    protected void setShown(boolean shown) {
        if (mShown != shown) {
            mShown = shown;

            mAnimation.setRange(mShowPercent, shown ? 1.0f : 0.0f);
            mAnimation.setDuration(300); // TODO
            mAnimation.forceReset();
            invalidate();

            if (!shown) {
                mListener.onHide();
            }
        }
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        if (mShown && event.getAction() == MotionEvent.ACTION_UP) {
            setShown(false);
        }
        return mShown;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        measureComponent(mRightView, widthSpec, heightSpec);
        measureComponent(mBottomView, widthSpec, heightSpec);
        super.onMeasure(widthSpec, heightSpec);
    }

    @Override
    protected void onLayout(
            boolean changeSize, int left, int top, int right, int bottom) {
        if (changeSize) {
            int width = getWidth();
            int height = getHeight();

            int l = width - (int) (mRightView.getMeasuredWidth() * mShowPercent);
            mRightView.layout(l, height / 2 - mRightView.getMeasuredHeight() / 2,
                    l + mRightView.getMeasuredWidth(), height / 2 + mRightView.getMeasuredHeight() / 2);

            int t = height - (int) (mBottomView.getMeasuredHeight() * mShowPercent);
            mBottomView.layout(0, t, width, t + mBottomView.getMeasuredHeight());
        }
    }

    public void setRange(int start, int end) {
        mSlider.setRange(start, end);
        mEnd = end;

        String pages = Integer.toString(end);
        mProgressTextView.setMinimumWidth((int) (sTextTexture.getMaxWidth() * pages.length()));
        mPagesTextView.setMinimumWidth((int) (sTextTexture.getMaxWidth() * pages.length()));
        mPagesTextView.setText(pages);
    }

    public void setProgress(int progress) {
        mSlider.setProgress(progress);
        mProgressTextView.setText(Integer.toString(progress));
    }

    @Override
    public void onFitPadding(int left, int top, int right, int bottom) {
        mRightView.setPaddingRight(LayoutUtils.dp2pix(mContext, 8) + right);
        mBottomView.setPaddingBottom(bottom);
        mBottomView.setPaddingRight(LayoutUtils.dp2pix(mContext, 8) + right);
    }

    @Override
    public void onSetProgress(int newProgress, int oldProgress, boolean byUser) {
        mListener.onSetProgress(newProgress, oldProgress, byUser);
        mProgressTextView.setText(Integer.toString(newProgress));
    }

    @Override
    protected void onRender(GLCanvas canvas) {
        if (mAnimation.calculate(AnimationTime.get())) {
            invalidate();
        }

        super.onRender(canvas);
    }

    public interface ActionListener {
        void onSetProgress(int newProgress, int oldProgress, boolean byUser);

        void onHide();
    }
}
