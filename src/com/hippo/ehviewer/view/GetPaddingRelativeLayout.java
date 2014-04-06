package com.hippo.ehviewer.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import com.hippo.ehviewer.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

public class GetPaddingRelativeLayout extends RelativeLayout {
    private static final String TAG = "GetPaddingRelativeLayout";
    private View mView1;
    private View mView2;
    
    public GetPaddingRelativeLayout(Context context) {
        super(context);
    }
    public GetPaddingRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public GetPaddingRelativeLayout(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setView(View view1, View view2) {
        mView1 = view1;
        mView2 = view2;
    }
    
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        boolean re = super.fitSystemWindows(insets);
        int pt = this.getPaddingTop();
        int pb = this.getPaddingBottom();
        if (mView1 != null)
            mView1.setPadding(mView1.getPaddingLeft(), pt, mView1.getPaddingRight(), pb);
        if (mView2 != null)
            mView2.setPadding(mView2.getPaddingLeft(), pt, mView2.getPaddingRight(), pb);
        return re;
    }




}
