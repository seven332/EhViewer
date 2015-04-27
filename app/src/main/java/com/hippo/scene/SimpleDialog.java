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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Space;
import android.widget.TextView;

import com.hippo.effect.ripple.RippleSalon;
import com.hippo.ehviewer.R;
import com.hippo.util.ViewUtils;
import com.hippo.widget.IndicatingScrollView;

public class SimpleDialog extends SceneDialog implements View.OnClickListener,
        SimpleDialogView.OnClickOutOfDialogListener {

    private static final int BACKGROUND_COLOR = 0x61000000;

    private Builder mBuilder;

    private StageActivity mActivity;

    private SimpleDialogView mSimpleDialogView;
    private SimpleDialogFrame mFrame;
    private View mBody;
    private TextView mTitle;
    private Space mSpaceTitleContent;
    private IndicatingScrollView mContent;
    private TextView mMessage;
    private View mButtonsSingleLine;
    private TextView mNegativeButton;
    private View mSpacePositiveNegative;
    private TextView mPositiveButton;

    private int mFitPaddingBottom;

    private SimpleDialog(@NonNull Builder builder) {
        mBuilder = builder;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO
        setBackgroundColor(BACKGROUND_COLOR);

        mActivity = getStageActivity();

        setContentView(R.layout.simple_dialog_frame);
        mFrame = (SimpleDialogFrame) findViewById(R.id.simple_dialog_frame);

        mActivity.getLayoutInflater().inflate(R.layout.simple_dialog, mFrame);

        mSimpleDialogView = (SimpleDialogView) getSceneView();
        mBody = findViewById(R.id.body);
        mTitle = (TextView) mBody.findViewById(R.id.title);
        mSpaceTitleContent = (Space) mBody.findViewById(R.id.space_title_content);
        mContent = (IndicatingScrollView) findViewById(R.id.content);
        mMessage = (TextView) mBody.findViewById(R.id.message);
        mButtonsSingleLine = findViewById(R.id.buttons_single_line);
        mNegativeButton = (TextView) mButtonsSingleLine.findViewById(R.id.negative);
        mSpacePositiveNegative = mButtonsSingleLine.findViewById(R.id.space_positive_negative);
        mPositiveButton = (TextView) mButtonsSingleLine.findViewById(R.id.positive);

        bindDialog();
    }

    private void bindDialog() {
        if (mBuilder.mTitle == null && mBuilder.mMessage == null) {
            ViewUtils.setVisibility(mBody, View.GONE);
        } else {
            if (mBuilder.mTitle != null) {
                mTitle.setText(mBuilder.mTitle);
            } else {
                ViewUtils.setVisibility(mTitle, View.GONE);
                ViewUtils.setVisibility(mSpaceTitleContent, View.GONE);
            }
            if (mBuilder.mMessage != null) {
                mMessage.setText(mBuilder.mMessage);
            } else {
                ViewUtils.setVisibility(mContent, View.GONE);
                ViewUtils.setVisibility(mSpaceTitleContent, View.GONE);
            }
        }

        if (mBuilder.mPositiveButtonText == null && mBuilder.mNegativeButtonText == null) {
            ViewUtils.setVisibility(mButtonsSingleLine, View.GONE);
        } else {
            if (mBuilder.mPositiveButtonText != null) {
                mPositiveButton.setText(mBuilder.mPositiveButtonText);
                mPositiveButton.setOnClickListener(this);
                RippleSalon.addRipple(mPositiveButton, false);
            } else {
                ViewUtils.setVisibility(mPositiveButton, View.GONE);
                ViewUtils.setVisibility(mSpacePositiveNegative, View.GONE);
            }
            if (mBuilder.mNegativeButtonText != null) {
                mNegativeButton.setText(mBuilder.mNegativeButtonText);
                mNegativeButton.setOnClickListener(this);
                RippleSalon.addRipple(mNegativeButton, false);
            } else {
                ViewUtils.setVisibility(mNegativeButton, View.GONE);
                ViewUtils.setVisibility(mSpacePositiveNegative, View.GONE);
            }
        }

        mSimpleDialogView.setOnClickOutOfDialogListener(this);
    }

    @Override
    protected SceneView createSceneView(Context context) {
        return new SimpleDialogView(context);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mFitPaddingBottom = b;
        mFrame.setFitPaddingBottom(b);
    }

    public SimpleDialogFrame getFrame() {
        return mFrame;
    }

    public void getCenterLocation(int[] location) {
        if (location == null || location.length < 2) {
            throw new IllegalArgumentException("location must be an array of two integers");
        }

        if (mSimpleDialogView == null) {
            location[0] = 0;
            location[1] = 0;
        } else {
            location[0] = mSimpleDialogView.getWidth() / 2;
            location[1] = (mSimpleDialogView.getHeight() - mFitPaddingBottom) / 2;
        }
    }

    public int getWidth() {
        if (mFrame == null) {
            return 0;
        } else {
            return mFrame.getWidth();
        }
    }

    public int getHeight() {
        if (mFrame == null) {
            return 0;
        } else {
            return mFrame.getHeight();
        }
    }

    public int getStartX() {
        return mBuilder.mStartX;
    }

    public int getStartY() {
        return mBuilder.mStartY;
    }

    @Override
    public void onClick(View v) {
        if (v == mPositiveButton) {
            finish();
        } else if (v == mNegativeButton) {
            finish();
        }
    }

    @Override
    public void onClickOutOfDialog() {
        if (mBuilder.mCancelable) {
            finish();
        }
    }

    public static class Builder {

        private Context mContext;

        private String mTitle;
        private String mMessage;
        private String mPositiveButtonText;
        private String mNegativeButtonText;

        private boolean mCancelable = true;

        private int mStartX;
        private int mStartY;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setTitle(@StringRes int resId) {
            return setTitle(mContext.getString(resId));
        }

        public Builder setMessage(String message) {
            mMessage = message;
            return this;
        }

        public Builder setMessage(@StringRes int resId) {
            return setMessage(mContext.getString(resId));
        }

        public Builder setPositiveButton(String string) {
            mPositiveButtonText = string;
            return this;
        }

        public Builder setPositiveButton(int resId) {
            return setPositiveButton(mContext.getString(resId));
        }

        public Builder setNegativeButton(String string) {
            mNegativeButtonText = string;
            return this;
        }

        public Builder setNegativeButton(int resId) {
            return setNegativeButton(mContext.getString(resId));
        }

        public Builder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }

        public Builder setStartPoint(int startX, int startY) {
            mStartX = startX;
            mStartY = startY;
            return this;
        }

        public @NonNull SceneDialog build() {
            return new SimpleDialog(this);
        }

        public void show() {
            show(null);
        }

        public void show(Curtain curtain) {
            if (curtain == null) {
                curtain = new SimpleDialogCurtain(mStartX, mStartY);
            }
            new SimpleDialog(this).show(curtain);
        }
    }
}
