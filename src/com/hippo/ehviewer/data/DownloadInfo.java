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

package com.hippo.ehviewer.data;

import com.hippo.ehviewer.widget.ExpandableListItem;


public class DownloadInfo implements ExpandableListItem {
    @SuppressWarnings("unused")
    private static final String TAG = DownloadInfo.class.getSimpleName();

    public static final int STATE_NONE = 0x0;
    public static final int STATE_WAIT = 0x1;
    public static final int STATE_DOWNLOAD = 0x2;
    public static final int STATE_FINISH = 0x3;

    public final GalleryInfo galleryInfo;
    public final int mode;
    public volatile int state = STATE_NONE;
    /** byte/second **/
    public volatile int speed;
    public volatile int download = -1;
    public volatile int total = -1;
    public volatile int legacy;

    private boolean mExpanded = false;

    public DownloadInfo(GalleryInfo gi, int mode) {
        this.galleryInfo = gi;
        this.mode = mode;
    }

    public DownloadInfo(GalleryInfo gi, int mode, int state, int legacy) {
        this.galleryInfo = gi;
        this.mode = mode;
        this.state = state;
        this.legacy = legacy;
    }

    @Override
    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
    }

    @Override
    public boolean getExpanded() {
        return mExpanded;
    }
}
