package com.hippo.ehviewer.data;

import android.os.Parcel;
import android.os.Parcelable;

public class GalleryInfo implements Parcelable{
    public int gid;
    public String token;
    public String title;
    public String posted;
    public int category;
    public String thumb;
    public String uploader;
    public float rating;
    
    public static final Parcelable.Creator<GalleryInfo> CREATOR =
            new Parcelable.Creator<GalleryInfo>() {
                @Override
                public GalleryInfo createFromParcel(Parcel source) {
                    GalleryInfo p = new GalleryInfo();
                    p.gid = source.readInt();
                    p.token = source.readString();
                    p.title = source.readString();
                    p.posted = source.readString();
                    p.category = source.readInt();
                    p.thumb = source.readString();
                    p.uploader = source.readString();
                    p.rating = source.readFloat();
                    return p;
                }

                @Override
                public GalleryInfo[] newArray(int size) {
                    return new GalleryInfo[size];
                }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(gid);
        dest.writeString(token);
        dest.writeString(title);
        dest.writeString(posted);
        dest.writeInt(category);
        dest.writeString(thumb);
        dest.writeString(uploader);
        dest.writeFloat(rating);
    }
}
