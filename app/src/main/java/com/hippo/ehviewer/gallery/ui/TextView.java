package com.hippo.ehviewer.gallery.ui;

import android.graphics.Rect;

import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.TextTexture;
import com.hippo.yorozuya.ArrayUtils;

public class TextView extends GLView {

    TextTexture mTextTexture;

    private String mText = "";
    private int[] mIndexes = ArrayUtils.EMPTY_INT_ARRAY;

    private int mGravity = Gravity.NO_GRAVITY;

    public TextView(TextTexture textTexture) {
        mTextTexture = textTexture;
    }

    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        if (!text.equals(mText)) {
            int oldMinimumWidth = getSuggestedMinimumWidth();

            mText = text;
            mIndexes = mTextTexture.getTextIndexes(text);

            if (getSuggestedMinimumWidth() != oldMinimumWidth) {
                requestLayout();
            } else {
                invalidate();
            }
        }
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            invalidate();
        }
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        if (mTextTexture == null) {
            return super.getSuggestedMinimumWidth();
        } else {
            return Math.max((int) mTextTexture.getTextWidth(mIndexes) + mPaddings.left + mPaddings.right,
                    super.getSuggestedMinimumWidth());
        }
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        if (mTextTexture == null) {
            return super.getSuggestedMinimumHeight();
        } else {
            return Math.max((int) mTextTexture.getTextHeight() + mPaddings.top + mPaddings.bottom,
                    super.getSuggestedMinimumHeight());
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredSize(getDefaultSize(getSuggestedMinimumWidth(), widthSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightSpec));
    }

    @Override
    protected void onRender(GLCanvas canvas) {
        Rect paddings = getPaddings();
        int x = getDefaultBegin(getWidth(), (int) mTextTexture.getTextWidth(mIndexes),
                paddings.left, paddings.right, Gravity.getPosition(mGravity, Gravity.HORIZONTAL));
        int y = getDefaultBegin(getHeight(), (int) mTextTexture.getTextHeight(),
                paddings.top, paddings.bottom, Gravity.getPosition(mGravity, Gravity.VERTICAL));
        mTextTexture.drawText(canvas, mIndexes, x, y);
    }
}
