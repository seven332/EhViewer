package com.hippo.ehviewer.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class AlertButton extends Button {
    public AlertDialog dialog;
    public AlertButton(Context context) {
        super(context);
    }
    public AlertButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public AlertButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
