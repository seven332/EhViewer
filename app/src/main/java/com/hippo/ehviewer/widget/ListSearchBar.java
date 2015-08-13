/*
 * Copyright (C) 2015 Hippo Seven
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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.util.Settings;
import com.hippo.yorozuya.Messenger;

public class ListSearchBar extends SearchBar implements Messenger.Receiver {

    private int mSource = -1;

    public ListSearchBar(Context context) {
        super(context);
        init();
    }

    public ListSearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ListSearchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSource(Settings.getEhSource());
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Messenger.getInstance().register(Constants.MESSENGER_ID_EH_SOURCE, this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Messenger.getInstance().unregister(Constants.MESSENGER_ID_EH_SOURCE, this);
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public void setSource(int source) {
        if (mSource != source) {
            Resources resources = getContext().getResources();
            Drawable searchImage = resources.getDrawable(R.drawable.ic_search);
            SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
            ssb.append(String.format(resources.getString(R.string.search_bar_hint),
                    EhUrl.getReadableHost(source)));
            int textSize = (int) (getEditTextTextSize() * 1.25);
            searchImage.setBounds(0, 0, textSize, textSize);
            ssb.setSpan(new ImageSpan(searchImage), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            setEditTextHint(ssb);

            mSource = source;
        }
    }

    @Override
    public void onReceive(int id, Object obj) {
        if (id == Constants.MESSENGER_ID_EH_SOURCE) {
            if (obj instanceof Integer) {
                int source = (Integer) obj;
                setSource(source);
            }
        }
    }
}
