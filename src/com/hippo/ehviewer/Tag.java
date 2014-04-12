package com.hippo.ehviewer;

public class Tag extends ListUrls {
    private String mName;
    
    public Tag(String name, int category, String search) {
        super(category, search);
        mName = name;
    }
    
    public void setName(String name) {
        mName = name;
    }
    
    public String getName() {
        return mName;
    }
}
