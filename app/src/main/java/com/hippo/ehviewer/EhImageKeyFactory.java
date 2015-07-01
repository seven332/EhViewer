package com.hippo.ehviewer;

public class EhImageKeyFactory {

    public static String getThumbKey(int gid) {
        return "thumb:" + gid;
    }

    /**
     * @param index start from 0
     */
    public static String getNormalPreviewKey(int gid, int index) {
        return "preview:normal:" + gid + ":" + index;
    }

    /**
     * @param index start from 0
     */
    public static String getLargePreviewKey(int gid, int index) {
        return "preview:large:" + gid + ":" + index;
    }
}
