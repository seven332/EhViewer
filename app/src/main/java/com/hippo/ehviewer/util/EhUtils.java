/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.util;

import java.io.File;

import com.hippo.ehviewer.data.ListUrls;

public final class EhUtils {

    public static final String EH_DOWNLOAD_FILENAME = ".ehviewer";

    private static final int[] CATEGORY_VALUES = { ListUrls.MISC, ListUrls.DOUJINSHI, ListUrls.MANGA,
            ListUrls.ARTIST_CG, ListUrls.GAME_CG, ListUrls.IMAGE_SET, ListUrls.COSPLAY, ListUrls.ASIAN_PORN,
            ListUrls.NON_H, ListUrls.WESTERN, ListUrls.UNKNOWN };

    // TODO How about "Private"
    private static final String[][] CATEGORY_STRINGS = { new String[] { "misc" }, new String[] { "doujinshi" },
            new String[] { "manga" }, new String[] { "artistcg", "Artist CG Sets" },
            new String[] { "gamecg", "Game CG Sets" }, new String[] { "imageset", "Image Sets" },
            new String[] { "cosplay" }, new String[] { "asianporn", "Asian Porn" }, new String[] { "non-h" },
            new String[] { "western" }, new String[] { "unknown" } };

    public static int getCategory(String type) {
        int i;
        for (i = 0; i < CATEGORY_STRINGS.length - 1; i++) {
            for (String str : CATEGORY_STRINGS[i])
                if (str.equalsIgnoreCase(type))
                    return CATEGORY_VALUES[i];
        }

        return CATEGORY_VALUES[i];
    }

    public static String getCategory(int type) {
        int i;
        for (i = 0; i < CATEGORY_VALUES.length - 1; i++) {
            if (CATEGORY_VALUES[i] == type)
                break;
        }
        return CATEGORY_STRINGS[i][0];
    }

    /**
     * Get gallery for read and download Gid is constant, but title may be
     * changed
     *
     * @param gid
     * @param title
     * @return
     */
    public static File getGalleryDir(int gid, String title) {

        File downloadDir = new File(Config.getDownloadPath());
        File gDir = new File(downloadDir, Utils.standardizeFilename(Integer.toString(gid)));
        File defaultDir = generateGalleryDir(gid, title);

        if (gDir.exists()) {
            if (gDir.renameTo(defaultDir))
                return defaultDir;
            else
                return gDir;
        } else {
            // TODO I need a better way or only call it in non-UI thread
            String[] fileList = downloadDir.list();
            if (fileList != null) {
                for (String str : fileList) {
                    // For title changed
                    if (str.startsWith(gid + "-") && (gDir = new File(downloadDir, str)) != null && gDir.isDirectory())
                        return gDir;
                }
            }

            return defaultDir;
        }
    }

    public static File generateGalleryDir(int gid, String title) {
        return new File(Config.getDownloadPath(), Utils.standardizeFilename(gid + "-" + title));
    }

    /**
     * Index start from 0
     *
     * @param index
     * @param extension
     * @return
     */
    public static String getImageFilename(int index, String extension) {
        return String.format("%08d.%s", index + 1, extension);
    }

    public static String[] POSSIBLE_IMAGE_EXTENSION_ARRAY = new String[] {
        "jpg", "jpeg", "png", "gif"
    };

    public static String DEFAULT_IMAGE_EXTENSION = POSSIBLE_IMAGE_EXTENSION_ARRAY[0];

    public static String[] getPossibleImageFilenames(int index) {
        String prefix = String.format("%08d.", index + 1);

        int length = POSSIBLE_IMAGE_EXTENSION_ARRAY.length;
        String[] strs = new String[length];
        for (int i = 0; i < length; i++)
            strs[i] = prefix + POSSIBLE_IMAGE_EXTENSION_ARRAY[i];

        return strs;
    }

}
