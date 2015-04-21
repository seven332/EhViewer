package com.hippo.ehviewer.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

public final class PackageUtils {

    private static final String TAG = PackageUtils.class.getSimpleName();

    public static String getSignature(Context context, String packageName) {
        try {
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            Signature[] ss = pi.signatures;
            if (ss != null && ss.length >= 1) {
                return Utils.computeSHA1(ss[0].toByteArray());
            } else {
                Log.e(TAG, "Can't find signature in package " + packageName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't find package " + packageName, e);
        }
        return null;
    }
}
