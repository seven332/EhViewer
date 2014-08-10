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

import java.util.ArrayList;
import java.util.List;

public class LofiGalleryDetail extends LofiGalleryInfo
        implements LofiDetailImpl {
    public int previewPageNum;
    public int previewPerPage;
    private final List<PreviewList> previewLists = new ArrayList<PreviewList>();

    public LofiGalleryDetail(GalleryInfo galleryInfo) {
        this(new LofiGalleryInfo(galleryInfo));
    }

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

        previewPageNum = Integer.MAX_VALUE;
    }

    @Override
    public int getGid() {
        return gid;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getPreviewPageNum() {
        return previewPageNum;
    }

    @Override
    public void setPreviewPageNum(int pageNum) {
        previewPageNum = pageNum;
    }

    @Override
    public int getPreviewPerPage() {
        return previewPerPage;
    }

    @Override
    public PreviewList getPreview(int page) {
        if (page >= 0 && page < previewLists.size())
            return previewLists.get(page);
        else
            return null;
    }

    @Override
    public void setPreview(int page, PreviewList previewList) {
        if (page >= 0 && page < previewLists.size())
            previewLists.set(page, previewList);
        else if (page == previewLists.size())
            previewLists.add(previewList);
        else
            ; // Do nothing
    }

    @Override
    public String[] getTags() {
        return lofiTags;
    }
}
