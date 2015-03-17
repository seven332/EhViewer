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

package com.hippo.ehviewer.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hippo.effect.ripple.RippleSalon;
import com.hippo.ehviewer.R;
import com.hippo.util.ViewUtils;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGBuilder;

public class SelectSearchImageLayout extends LinearLayout implements View.OnClickListener{

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_PATH = "path";

    private TextView mButtonSelect;
    private TextView mTextPath;
    private ImageView mImagePreview;

    private String mSelectedPath;
    private Bitmap mSelectedImage;

    private SelectSearchImageLayoutHelper mHelper;

    public SelectSearchImageLayout(Context context) {
        super(context);
        init(context);
    }

    public SelectSearchImageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SelectSearchImageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SelectSearchImageLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_select_search_image_layout, this);

        // TODO It is a bad idea.
        View firstChild = getChildAt(0);
        if (firstChild instanceof TextView) {
            setOrientation(LinearLayout.VERTICAL);
        } else if (firstChild instanceof LinearLayout) {
            setOrientation(LinearLayout.HORIZONTAL);
        } else {
            throw new IllegalStateException("Can't guess orientation, did you changed the layout res ?");
        }

        mButtonSelect = (TextView) findViewById(R.id.search_select_image_action);
        mTextPath = (TextView) findViewById(R.id.search_select_image_path);
        mImagePreview = (ImageView) findViewById(R.id.search_select_image_image);

        RippleSalon.addRipple(mButtonSelect, false);
        mButtonSelect.setOnClickListener(this);

        ViewUtils.removeHardwareAccelerationSupport(mImagePreview);
        SVG svg = new SVGBuilder().readFromResource(getResources(), R.raw.file_image).build();
        mImagePreview.setImageDrawable(svg.getDrawable());
    }

    public void setHelper(SelectSearchImageLayoutHelper helper) {
        mHelper = helper;
    }

    private void setImagePreview(String imagePath) {
        if (mSelectedImage != null && !mSelectedImage.isRecycled()) {
            mSelectedImage.isRecycled();
        }
        mSelectedImage = BitmapFactory.decodeFile(imagePath);
        if (mSelectedImage != null) {
            mImagePreview.setImageBitmap(mSelectedImage);
        } else {
            SVG svg = new SVGBuilder().readFromResource(getResources(), R.raw.file_image_off).build();
            mImagePreview.setImageDrawable(svg.getDrawable());
        }
    }

    public void onSelectImage(@NonNull String selectPath) {
        mSelectedPath = selectPath;

        mTextPath.setText(selectPath);
        setImagePreview(selectPath);

        ViewUtils.setVisibility(mTextPath, View.VISIBLE);
        ViewUtils.setVisibility(mImagePreview, View.VISIBLE);

        super.onDetachedFromWindow();
        super.onAttachedToWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        if (mSelectedPath != null) {
            setImagePreview(mSelectedPath);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mSelectedImage != null && !mSelectedImage.isRecycled()) {
            mSelectedImage.isRecycled();
        }
        mSelectedImage = null;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putString(STATE_KEY_PATH, mSelectedPath);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            String path = savedState.getString(STATE_KEY_PATH);
            if (path != null) {
                onSelectImage(path);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (mHelper != null) {
            mHelper.onRequstSelectImage();
        }
    }

    public interface SelectSearchImageLayoutHelper {
        public void onRequstSelectImage();
    }

}
