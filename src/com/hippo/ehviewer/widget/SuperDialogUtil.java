package com.hippo.ehviewer.widget;

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
    
    
    public static AlertDialog createUpdateDialog(Context context, String version,
            String size, String info, View.OnClickListener positiveListener) {
        
        return new DialogBuilder(context).setTitle(R.string.update).setCancelable(false)
                .setMessage(String.format(context.getString(R.string.update_message), version, size, info))
                .setPositiveButton(android.R.string.ok, positiveListener)
                .setSimpleNegativeButton()
                .create();
    }
}
