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

package com.hippo.ehviewer.client.data;

import com.hippo.ehviewer.dao.DownloadInfoObj;
import com.hippo.ehviewer.util.DBUtils;

public class DownloadInfo {

    public static final int STATE_NONE = 0x0;
    public static final int STATE_WAIT = 0x1;
    public static final int STATE_DOWNLOAD = 0x2;
    public static final int STATE_FINISH = 0x3;

    public String label;
    public GalleryBase galleryBase;
    public int state = STATE_NONE;
    // byte/second, -1 for can't get speed now
    public long speed;
    // -1 for unknown
    public int download = -1;
    // -1 for unknown
    public int total = -1;
    // Integer.MAX_VALUE for unknown
    public int legacy = Integer.MAX_VALUE;

    public long time;

    public static DownloadInfo fromDownloadInfoObj(DownloadInfoObj obj) {
        GalleryBase galleryBase = DBUtils.getGalleryBase((int) (long) obj.getGid());
        if (galleryBase != null) {
            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.galleryBase = galleryBase;
            downloadInfo.label = obj.getLabel();
            downloadInfo.state = obj.getState();
            downloadInfo.legacy = obj.getLegacy();
            downloadInfo.time = obj.getTime();
            return downloadInfo;
        } else {
            return null;
        }
    }
}
