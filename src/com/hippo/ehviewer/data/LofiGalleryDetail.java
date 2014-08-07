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

public class LofiGalleryDetail extends LofiGalleryInfo
        implements PreviewImpl {
    public int previewSum;
    public int previewPerPage;
    public PreviewList[] previewLists;

    public LofiGalleryDetail(LofiGalleryInfo lofiGalleryInfo) {
        gid = lofiGalleryInfo.gid;
        token = lofiGalleryInfo.token;
        title = lofiGalleryInfo.title;
        posted = lofiGalleryInfo.posted;
        category = lofiGalleryInfo.category;
        thumb = lofiGalleryInfo.thumb;
        uploader = lofiGalleryInfo.uploader;
        rating = lofiGalleryInfo.rating;
        simpleLanguage = lofiGalleryInfo.simpleLanguage;
        lofiTags = lofiGalleryInfo.lofiTags;

        previewSum = Integer.MAX_VALUE;
    }

    @Override
    public int getPreviewSum() {
        return previewSum;
    }

    @Override
    public int getPreviewPerPage() {
        return previewPerPage;
    }

    @Override
    public PreviewList[] getPreview() {
        return previewLists;
    }
}
