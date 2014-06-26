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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

public class LinkifyTextView extends TextView {
    
    private String mUrl;
    
    public LinkifyTextView(Context context) {
        super(context);
        init();
    }
    
    public LinkifyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public LinkifyTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    public void init() {
        // Just make urls striking
        // LinkifyTextView itself should not handle touch url event
        setAutoLinkMask(Linkify.WEB_URLS);
        setLinksClickable(false);
    }
    
    public String getTouchedUrl() {
        return mUrl;
    }
    
    /**
     * Call it when you do not need the url any more
     */
    public void clearTouchedUrl() {
        mUrl = null;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getText() instanceof Spannable) {
            // Get this code from android.text.method.LinkMovementMethod.
            // Work fine !
            int x = (int) event.getX();
            int y = (int) event.getY();
            boolean isGetUrl = false;
            
            x -= getTotalPaddingLeft();
            y -= getTotalPaddingTop();
            
            x += getScrollX();
            y += getScrollY();
            
            Layout layout = getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);
            
            ClickableSpan[] links = ((Spannable)getText()).getSpans(off, off, ClickableSpan.class);
            
            for (ClickableSpan link : links) {
                if (link instanceof URLSpan) {
                    mUrl = ((URLSpan)link).getURL();
                    isGetUrl = true;
                    break;
                }
            }
            if (!isGetUrl)
                mUrl = null;
        } else {
            mUrl = null;
        }
        
        return super.onTouchEvent(event);
    }
}
