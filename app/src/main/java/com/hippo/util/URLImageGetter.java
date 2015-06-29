package com.hippo.util;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;

import com.hippo.conaco.Conaco;
import com.hippo.drawable.UnikeryDrawable;

public class URLImageGetter implements Html.ImageGetter {

    private TextView mTextView;
    private Conaco mConaco;

    public URLImageGetter(TextView view, Conaco conaco) {
        mTextView = view;
        mConaco = conaco;
    }

    @Override
    public Drawable getDrawable(String source) {
        UnikeryDrawable drawable = new UnikeryDrawable(mTextView);
        mConaco.load(drawable, source, source);
        return drawable;
    }
}
