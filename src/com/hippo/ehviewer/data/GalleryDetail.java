package com.hippo.ehviewer.data;

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class GalleryDetail extends GalleryInfo{
    
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
    
    public GalleryDetail(GalleryInfo galleryInfo) {
        gid = galleryInfo.gid;
        token = galleryInfo.token;
        title = galleryInfo.title;
        posted = galleryInfo.posted;
        category = galleryInfo.category;
        thumb = galleryInfo.thumb;
        uploader = galleryInfo.uploader;
        rating = galleryInfo.rating;
    }
}
