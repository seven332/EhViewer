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

package com.hippo.ehviewer.ui;

public class DownloadInfo {
    public static final int STOP = 0x0;
    public static final int DOWNLOADING = 0x1;
    public static final int WAITING = 0x2;
    public static final int COMPLETED = 0x3;
    public static final int FAILED = 0x4;
    
    public static final boolean DETAIL_URL = false;
    public static final boolean PAGE_URL = true;
    
    public String gid;
    public String thumb;
    public String title;
    public int status = STOP;
    public boolean type;
    
    public String detailUrlStr;
    
    public int pageSum = 0;
    public int lastStartIndex = 1;
    public String pageUrlStr;
    
    public float totalSize;
    public float downloadSize;
}
