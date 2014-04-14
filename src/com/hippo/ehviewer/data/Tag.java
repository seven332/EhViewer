package com.hippo.ehviewer.data;

import com.hippo.ehviewer.ListUrls;

public class Tag extends ListUrls {
    private String mName;
    
    public Tag(String name, int category, String search) {
        super(category, search);
        mName = name;
    }
    
    public Tag(String name, ListUrls lus) {
        this(name, lus.getType(), lus.getSearch());
        this.setAdvance(lus.getAdvanceType(), lus.getMinRating());
    }
    
    public void setName(String name) {
        mName = name;
    }
    
    public String getName() {
        return mName;
    }
}
