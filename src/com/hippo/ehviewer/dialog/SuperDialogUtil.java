package com.hippo.ehviewer.dialog;

import com.hippo.ehviewer.R;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class SuperDialogUtil {
    
    public static void setTitle(AlertDialog dialog, String title) {
        TextView titleView = (TextView)dialog.findViewById(R.id.title);
        titleView.setVisibility(View.VISIBLE);
        titleView.setText(title);
    }
    
}
