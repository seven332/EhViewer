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

public interface PreviewImpl {

    public int getGid();

    public String getToken();

    public String getTitle();

    /**
     * If it is unkonwn, return Integer.MAX_VALUE
     *
     * @return
     */
    public int getPreviewPageNum();

    public int getPreviewPerPage();

    /**
     * If page is error, return null
     *
     * @param page
     * @return
     */
    public PreviewList getPreview(int page);

    /**
     *
     * @param previewList
     */
    public void setPreview(int page, PreviewList previewList);

    public GalleryInfo toGalleryInfo();

}
