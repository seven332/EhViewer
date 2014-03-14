package com.hippo.ehviewer.view;

import com.hippo.ehviewer.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class SearchView extends LinearLayout {
    public SearchView(Context context) {
        super(context, null);
    }
    
    public SearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.search_view, this, true);
    }
    
    
}
