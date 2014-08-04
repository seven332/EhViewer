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

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class GalleryDetail extends GalleryInfo {

    public String title_jpn;
    public int pages;
    public String size;
    public String resized;
    public String parent;
    public String visible;
    public String language;
    public int people;
    public String firstPage;
    public LinkedHashMap<String, LinkedList<SimpleEntry<String, Integer>>> tags;

    // For Preview
    public int previewSum;
    public int previewPerPage;
    public PreviewList[] previewLists;
    public List<Comment> comments;

    public GalleryDetail() {}

    public GalleryDetail(GalleryInfo galleryInfo) {
        gid = galleryInfo.gid;
        token = galleryInfo.token;
        title = galleryInfo.title;
        posted = galleryInfo.posted;
        category = galleryInfo.category;
        thumb = galleryInfo.thumb;
        uploader = galleryInfo.uploader;
        rating = galleryInfo.rating;
        simpleLanguage = galleryInfo.simpleLanguage;
    }
}
