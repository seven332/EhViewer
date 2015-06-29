package com.hippo.miscellaneous;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.hippo.ehviewer.R;

public class StartActivityHelper {

    public static boolean sendEmail(@NonNull Activity from, @NonNull String address,
            @Nullable String subject, @Nullable String text) {
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:" + address));
        if (subject != null) {
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        if (text != null) {
            i.putExtra(Intent.EXTRA_TEXT, text);
        }

        try {
            from.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(from, R.string.em_cant_find_activity, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static boolean share(@NonNull Activity from, String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");

        try {
            from.startActivity(sendIntent);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(from, R.string.em_cant_find_activity, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
