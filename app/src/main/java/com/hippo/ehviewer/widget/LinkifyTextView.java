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
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkifyTextView extends TextView {

    private static final Pattern URL_PATTERN = Pattern.compile("(http|https)://[a-z0-9A-Z%-]+(\\.[a-z0-9A-Z%-]+)+(:\\d{1,5})?(/[a-zA-Z0-9-_~:#@!&',;=%/\\*\\.\\?\\+\\$\\[\\]\\(\\)]+)?/?");

    private String mUrl;

    public LinkifyTextView(Context context) {
        super(context);
    }

    public LinkifyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkifyTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setLinkifyText(CharSequence text) {
        Matcher m = URL_PATTERN.matcher(text);

        Spannable spannable;
        if (text instanceof Spannable) {
            spannable = (Spannable) text;
        } else {
            spannable = new SpannableString(text);
        }

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            URLSpan[] links = spannable.getSpans(start, end, URLSpan.class);
            if (links.length > 0) {
                // There has been URLSpan already, leave it alone
                continue;
            }

            URLSpan urlSpan = new URLSpan(m.group(0));
            spannable.setSpan(urlSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        setText(spannable);
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
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        // Let the parent or grandparent of TextView to handles click aciton.
        // Otherwise click effect like ripple will not work, and if touch area
        // do not contain a url, the TextView will still get MotionEvent.
        // onTouchEven must be called with MotionEvent.ACTION_DOWN for each touch
        // action on it, so we analyze touched url here.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getText() instanceof Spanned) {
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

                URLSpan[] links = ((Spanned)getText()).getSpans(off, off, URLSpan.class);

                for (URLSpan link : links) {
                    mUrl = link.getURL();
                    if (mUrl != null) {
                        isGetUrl = true;
                        break;
                    }
                }
                if (!isGetUrl) {
                    mUrl = null;
                }
            } else {
                mUrl = null;
            }
        }

        return super.onTouchEvent(event);
    }
}
