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

package com.hippo.ehviewer;

import com.hippo.ehviewer.data.GalleryInfo;

public class GalleryDownloadInfo {
    
    public static final int NONE = 0x0;
    public static final int DOWNLOADING = 0x1;
    public static final int FINISHED = 0x2;
    
    public GalleryInfo galleryInfo;
    
    // For ui set progressbar
    public int picNum;
    public int downloadNum;
    
    public int state;
    
    public GalleryDownloadInfo(GalleryInfo galleryInfo) {
        this(galleryInfo, NONE);
    }
    
    public GalleryDownloadInfo(GalleryInfo galleryInfo, int state) {
        this.galleryInfo = galleryInfo;
        this.state = state == DOWNLOADING ? NONE : FINISHED;
    }
}
