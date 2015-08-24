/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.gallery;

import android.support.annotation.Nullable;

import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Settings;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.FileUtils;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class GalleryDir {

    private static final String[] POSSIBLE_IMAGE_EXTENSIONS = new String[] {
            "jpg", "jpeg", "png", "gif"
    };

    private String mDirname;

    private UniFile mUniFile;

    private AtomicReferenceArray<String> mFilenames;

    public static String generateImageFilename(int index, String extension) {
        return String.format("%08d.%s", index + 1, extension);
    }

    private static String getDirname(GalleryBase galleryBase) {
        String dirname = DBUtils.getDirname(galleryBase.gid);
        if (dirname == null) {
            dirname = FileUtils.ensureFilename(galleryBase.gid + "-" + EhUtils.getSuitableTitle(galleryBase));
            DBUtils.addDirname(galleryBase, dirname);
        }
        return dirname;
    }

    public GalleryDir(GalleryBase galleryBase) {
        mDirname = getDirname(galleryBase);
    }

    public void ensureUniFile() {
        if (mUniFile == null) {
            mUniFile = Settings.getImageDownloadLocation().createDirectory(mDirname);
        }
    }

    public void ensureFilenames(int pages) {
        if (mFilenames == null) {
            mFilenames = new AtomicReferenceArray<>(pages);
        }
    }

    public UniFile getDir() {
        return mUniFile;
    }

    private String guessImageFilename(int index) {
        if (mUniFile == null) {
            return null;
        }

        for (String extension : POSSIBLE_IMAGE_EXTENSIONS) {
            String filename = generateImageFilename(index, extension);
            if (mUniFile.findFile(filename) != null) {
                return filename;
            }
        }
        return null;
    }

    public @Nullable String getImageFilename(int index) {
        if (mFilenames == null) {
            return null;
        }

        String filename = mFilenames.get(index);
        if (filename == null) {
            filename = guessImageFilename(index);
            if (filename != null) {
                // Put filename into array
                mFilenames.lazySet(index, filename);
            }
        }
        return filename;
    }

    public @Nullable UniFile getImageFile(int index) {
        if (mUniFile == null) {
            return null;
        }

        String filename = getImageFilename(index);
        if (filename != null) {
            return mUniFile.createFile(filename);
        } else {
            return null;
        }
    }

    public @Nullable UniFile findImageFile(int index) {
        if (mUniFile == null) {
            return null;
        }

        String filename = getImageFilename(index);
        if (filename != null) {
            return mUniFile.findFile(filename);
        } else {
            return null;
        }
    }

    public void putImageFilename(String filename, int index) {
        if (mFilenames != null) {
            mFilenames.lazySet(index, filename);
        }
    }

    public @Nullable UniFile createUniFile(String filename) {
        if (mUniFile == null) {
            return null;
        } else {
            return mUniFile.createFile(filename);
        }
    }

    public @Nullable UniFile findUniFile(String filename) {
        if (mUniFile == null) {
            return null;
        } else {
            return mUniFile.findFile(filename);
        }
    }
}
