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
            mText = text;
            mIndexes = mTextTexture.getTextIndexes(text);
            requestLayout();
        }
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int textWidth = 0;
        int textHight = 0;

        if (mTextTexture != null) {
            textWidth = (int) mTextTexture.getTextWidth(mIndexes);
            textHight = (int) mTextTexture.getTextHeight();
        }

        setMeasuredSize(getDefaultSize(Math.max(getSuggestedMinimumWidth(), textWidth), widthSpec),
                getDefaultSize(Math.max(getSuggestedMinimumHeight(), textHight), heightSpec));
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
