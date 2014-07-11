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

package com.hippo.ehviewer.data;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

import com.hippo.ehviewer.ui.DetailSectionFragment;
import com.hippo.ehviewer.widget.AutoWrapLayout;

/**
 * This interface describe the previews in one page
 * 
 * @author Hippo
 *
 */
public abstract class PreviewList {
    
    protected PreviewHolder mHolder;
    protected Activity mActivity;
    protected GalleryDetail mGi;
    protected int mTargetPage;
    
    /**
     * Add preview to view group
     * @param viewGroup
     * @return
     */
    public abstract void addPreview(AutoWrapLayout viewGroup);
    
    public void setData(PreviewHolder holder, Activity activity, GalleryDetail gi, int targetPage) {
        mHolder = holder;
        mActivity = activity;
        mGi = gi;
        mTargetPage = targetPage;
    }
    
    public void setTargetPage(int targetPage) {
        mTargetPage = targetPage;
    }
    
    /**
     * Return page url at target index, null for invalid index
     * @param index
     * @return
     */
    public abstract String getPageUrl(int index);
    
    protected class TextViewWithUrl extends TextView {
        public String url;
        
        public TextViewWithUrl(Context context) {
            super(context);
        }
    }
    
    public interface PreviewHolder {
        public int getCurPreviewPage();
        public void onGetPreviewImageFailure();
    }
}
