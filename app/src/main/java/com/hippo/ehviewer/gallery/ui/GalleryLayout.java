/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.gallery.ui;

import android.content.Context;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.LayoutUtils;

public class GalleryLayout extends FrameLayout implements GalleryView.ActionListener, GalleryPanel.ActionListener {

    private GalleryView mGalleryView;
    private GalleryPanel mGalleryPanel;

    private ActionListener mActionListener;

    public GalleryLayout(Context context, ActionListener listener) {
        mActionListener = listener;
        setBackgroundColor(context.getResources().getColor(R.color.gallery_background));

        mGalleryView = new GalleryView(context, 0, this);
        mGalleryView.setId(R.id.gallery_view);
        mGalleryView.setProgressSize(LayoutUtils.dp2pix(context, 56));
        mGalleryView.setInterval(LayoutUtils.dp2pix(context, 48));

        mGalleryPanel = new GalleryPanel(context, this);
        mGalleryPanel.setId(R.id.gallery_panel);

        addComponent(mGalleryView, new GravityLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addComponent(mGalleryPanel, new GravityLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public GalleryView getGalleryView() {
        return mGalleryView;
    }

    public GalleryPanel getGalleryPanel() {
        return mGalleryPanel;
    }

    @Override
    public void onTapCenter() {
        mGalleryPanel.setShown(true, true);
        mActionListener.onShowGalleryPanel();
    }

    @Override
    public void onSetMode(GalleryView.Mode mode) {
        mGalleryPanel.setRange(1, mGalleryView.getPages());
    }

    @Override
    public void onScrollToPage(int page, boolean internal) {
        if (internal) {
            mGalleryPanel.setProgress(page + 1);
        }
    }

    @Override
    public void onSetProgress(int newProgress, int oldProgress, boolean byUser) {
        if (byUser) {
            mGalleryView.scrollToPage(newProgress - 1);
        }
    }

    @Override
    public void onHide() {
        mActionListener.onHideGalleryPanel();
    }

    public interface ActionListener {

        void onShowGalleryPanel();

        void onHideGalleryPanel();
    }
}
