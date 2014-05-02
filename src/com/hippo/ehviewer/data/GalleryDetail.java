package com.hippo.ehviewer.data;

import android.os.Parcel;
import android.os.Parcelable;

public class GalleryDetail extends GalleryInfo{
    
    public int pages;
    public String size;
    public String resized;
    public String parent;
    public String visible;
    public String language;
    public int people;
    public String firstPage;
    public String[][] tags;
    
    // For Preview
    public int previewSum;
    public int previewPerPage;
    public PreviewList[] previewLists;
    
    /*
    public static final Parcelable.Creator<GalleryDetail> CREATOR =
            new Parcelable.Creator<GalleryDetail>() {
                @Override
                public GalleryDetail createFromParcel(Parcel source) {
                    GalleryDetail p =
                            new GalleryDetail(GalleryInfo.CREATOR.createFromParcel(source));
                    p.pages = source.readString();
                    p.size = source.readString();
                    p.resized = source.readString();
                    p.parent = source.readString();
                    p.visible = source.readString();
                    p.language = source.readString();
                    p.people = source.readString();
                    p.firstPage = source.readString();
                    int tagsNum = source.readInt();
                    p.tags = new String[tagsNum][];
                    for (int i = 0; i < tagsNum; i++) {
                        int num = source.readInt();
                        String[] tag = new String[num];
                        p.tags[i] = tag;
                        for (int j = 0; j < num; j++) {
                            tag[j] = source.readString();
                        }
                    }
                    p.previewSum = source.readInt();
                    p.previewPerPage = source.readInt();
                    source.readParcelable(loader);
                    return p;
                }
                
                @Override
                public GalleryDetail[] newArray(int size) {
                    return new GalleryDetail[size];
                }
    };*/
    
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
