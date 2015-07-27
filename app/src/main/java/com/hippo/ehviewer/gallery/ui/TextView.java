package com.hippo.ehviewer.gallery.ui;

import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.TextTexture;

public class TextView extends GLView {

    TextTexture mTextTexture;

    private String mText = "";

    public TextView(TextTexture textTexture) {
        mTextTexture = textTexture;
    }

    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        if (!text.equals(mText)) {
            mText = text;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int textWidth = 0;
        int textHight = 0;

        if (mTextTexture != null) {
            textWidth = (int) mTextTexture.getTextWidth(mText);
            textHight = (int) mTextTexture.getTextHeight(mText);
        }

        setMeasuredSize(getDefaultSize(Math.max(getSuggestedMinimumWidth(), textWidth), widthSpec),
                getDefaultSize(Math.max(getSuggestedMinimumHeight(), textHight), heightSpec));
    }

    @Override
    protected void onRender(GLCanvas canvas) {
        mTextTexture.drawText(canvas, mText, 0, 0);
    }
}
