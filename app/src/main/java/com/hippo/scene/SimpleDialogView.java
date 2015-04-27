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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.util.ViewUtils;
import com.hippo.widget.HotspotTouchHelper;
import com.hippo.widget.Hotspotable;

public class SimpleDialogView extends SceneView implements View.OnClickListener, Hotspotable {

    private float mHostpotX;
    private float mHostpotY;

    private OnClickOutOfDialogListener mOnClickOutOfDialogListener;

    public SimpleDialogView(Context context) {
        super(context);
        init();
    }

    public SimpleDialogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleDialogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSoundEffectsEnabled(false);
        setOnClickListener(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setOnTouchListener(new HotspotTouchHelper(this));
        }
    }

    public void setOnClickOutOfDialogListener(OnClickOutOfDialogListener listener) {
        mOnClickOutOfDialogListener = listener;
    }

    @Override
    public void setHotspot(float x, float y) {
        mHostpotX = x;
        mHostpotY = y;
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        mHostpotX = x;
        mHostpotY = y;
    }

    @Override
    public void onClick(View v) {
        if (v == this && !ViewUtils.isViewUnder(getChildAt(0), (int) mHostpotX,
                (int) mHostpotY) && mOnClickOutOfDialogListener != null) {
            mOnClickOutOfDialogListener.onClickOutOfDialog();
        }
    }

    public interface OnClickOutOfDialogListener {
        void onClickOutOfDialog();
    }
}
