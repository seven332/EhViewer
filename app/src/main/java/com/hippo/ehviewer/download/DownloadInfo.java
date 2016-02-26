/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.download;

import com.hippo.ehviewer.client.data.GalleryInfo;

public class DownloadInfo {

    public static final int STATE_NONE = 0;
    public static final int STATE_WAIT = 1;
    public static final int STATE_DOWNLOAD = 2;
    public static final int STATE_FINISH = 3;
    public static final int STATE_FAILED = 4;

    public String label;
    public GalleryInfo galleryInfo;
    public int state = STATE_NONE;
    // byte/second, -1 for can't get speed now
    public long speed;
    // -1 for unknown
    public int download = 0;
    // -1 for unknown
    public int total = -1;
    // -1 for unknown
    public int legacy = -1;
    // For order
    public long date;
}
