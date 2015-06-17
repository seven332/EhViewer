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

package com.hippo.scene;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.rippleold.RippleSalon;
import com.hippo.util.ViewUtils;

public class SimpleDialog extends SceneDialog implements View.OnClickListener,
        SceneDialogView.OnClickOutOfDialogListener, AdapterView.OnItemClickListener {

    private static final int BACKGROUND_COLOR = 0x8a000000;

    public final static int POSITIVE = 0;
    public final static int NEGATIVE = 1;

    private Builder mBuilder;

    private SceneDialogView mSceneDialogView;
    private SimpleDialogLayout mLayout;
    private CardView mCushion;
    private SimpleDialogFrame mFrame;
    private View mBody;
    private TextView mTitle;
    private Space mSpaceTitleContent;
    private ScrollView mContent;
    private TextView mMessage;
    private FrameLayout mCustom;
    private ListView mListView;
    private View mButtonsSingleLine;
    private TextView mNegativeButton;
    private View mSpacePositiveNegative;
    private TextView mPositiveButton;

    private boolean mCancel = true;

    private int mFitPaddingBottom;

    private void setBuilder(@NonNull Builder builder) {
        mBuilder = builder;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO
        setBackgroundColor(BACKGROUND_COLOR);

        setContentView(R.layout.simple_dialog_frame);

        mLayout = (SimpleDialogLayout) findViewById(R.id.simple_dialog_layout);
        mCushion = (CardView) mLayout.findViewById(R.id.simple_dialog_cushion);
        mFrame = (SimpleDialogFrame) mLayout.findViewById(R.id.simple_dialog_frame);

        getStageActivity().getLayoutInflater().inflate(R.layout.simple_dialog, mFrame);

        mSceneDialogView = (SceneDialogView) getSceneView();
        mBody = findViewById(R.id.body);
        mTitle = (TextView) mBody.findViewById(R.id.title);
        mSpaceTitleContent = (Space) mBody.findViewById(R.id.space_title_content);
        mContent = (ScrollView) findViewById(R.id.content);
        mMessage = (TextView) mBody.findViewById(R.id.message);
        mCustom = (FrameLayout) mBody.findViewById(R.id.custom);
        mListView = (ListView) mBody.findViewById(R.id.list);
        mButtonsSingleLine = findViewById(R.id.buttons_single_line);
        mNegativeButton = (TextView) mButtonsSingleLine.findViewById(R.id.negative);
        mSpacePositiveNegative = mButtonsSingleLine.findViewById(R.id.space_positive_negative);
        mPositiveButton = (TextView) mButtonsSingleLine.findViewById(R.id.positive);

        bindDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        OnCloseListener listener = mBuilder.mOnCloseListener;
        if (listener != null) {
            listener.onClose(this, mCancel);
        }
    }

    private void bindDialog() {
        ListView listView = mListView;

        if (mBuilder.mTitle == null && mBuilder.mMessage == null && mBuilder.mCustomViewResId == 0) {
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
                ViewUtils.setVisibility(mListView, View.GONE);
                ViewUtils.setVisibility(mCustom, View.GONE);
            } else if (mBuilder.mItemLayoutResId != 0) {
                listView.setAdapter(new ArrayAdapter<>(getStageActivity(),
                        mBuilder.mItemLayoutResId, android.R.id.text1, mBuilder.mItems));
                listView.setOnItemClickListener(this);
                if (mBuilder.mIsSingleChoice) {
                    listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    if (mBuilder.mCheckedItem > -1) {
                        listView.setItemChecked(mBuilder.mCheckedItem, true);
                        listView.setSelection(mBuilder.mCheckedItem);
                    }
                }
                ViewUtils.setVisibility(mContent, View.GONE);
                ViewUtils.setVisibility(mCustom, View.GONE);
            } else if (mBuilder.mCustomViewResId != 0) {
                getStageActivity().getLayoutInflater().inflate(mBuilder.mCustomViewResId, mCustom);
                if (mBuilder.mOnCreateCustomViewListener != null) {
                    mBuilder.mOnCreateCustomViewListener.onCreateCustomView(this,
                            mCustom.getChildAt(0));
                }
                ViewUtils.setVisibility(mContent, View.GONE);
                ViewUtils.setVisibility(mListView, View.GONE);
            } else {
                ViewUtils.setVisibility(mContent, View.GONE);
                ViewUtils.setVisibility(mListView, View.GONE);
                ViewUtils.setVisibility(mCustom, View.GONE);
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

        mSceneDialogView.setOnClickOutOfDialogListener(this);
    }

    @Override
    protected SceneView createSceneView(Context context) {
        return new SceneDialogView(context);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mFitPaddingBottom = b;
        mLayout.setFitPaddingBottom(b);
    }

    public CardView getCushion() {
        return mCushion;
    }

    public ViewGroup getFrame() {
        return mFrame;
    }

    public void getCenterLocation(int[] location) {
        if (location == null || location.length < 2) {
            throw new IllegalArgumentException("location must be an array of two integers");
        }

        if (mSceneDialogView == null) {
            location[0] = 0;
            location[1] = 0;
        } else {
            location[0] = mSceneDialogView.getWidth() / 2;
            location[1] = (mSceneDialogView.getHeight() - mFitPaddingBottom) / 2;
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

    public void pressPositiveButton() {
        if (mPositiveButton.isShown()) {
            onClick(mPositiveButton);
        }
    }

    public int getCheckedItemPosition() {
        // TODO check mListView isShown
        return mListView.getCheckedItemPosition();
    }

    @Override
    public void onClick(View v) {
        if (v == mPositiveButton) {
            if (mBuilder.mOnButtonClickListener == null ||
                    mBuilder.mOnButtonClickListener.onClick(this, POSITIVE)) {
                mCancel = false;
                finish();
            }
        } else if (v == mNegativeButton) {
            if (mBuilder.mOnButtonClickListener == null ||
                    mBuilder.mOnButtonClickListener.onClick(this, NEGATIVE)) {
                finish();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mListView) {
            OnClickListener listener = mBuilder.mOnListItemClickListener;
            if (listener == null || listener.onClick(this, position)) {
                mCancel = false;
                finish();
            }
        }
    }

    @Override
    public void onClickOutOfDialog(int x, int y) {
        if (mBuilder.mCancelable) {
            Curtain curtain = getCurtain();
            if (curtain instanceof SimpleDialogCurtain) {
                ((SimpleDialogCurtain) curtain).setStartPosition(x, y);
            }
            finish();
        }
    }

    public static class Builder {

        private Context mContext;

        private String mTitle;
        private String mMessage;

        private CharSequence[] mItems;
        private int mItemLayoutResId;
        private OnClickListener mOnListItemClickListener;
        protected int mCheckedItem;
        protected boolean mIsSingleChoice;

        private int mCustomViewResId = 0;
        private OnCreateCustomViewListener mOnCreateCustomViewListener;

        private String mPositiveButtonText;
        private String mNegativeButtonText;
        private OnClickListener mOnButtonClickListener;

        private boolean mCancelable = true;
        private OnCloseListener mOnCloseListener;

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

        public Builder setItems(int itemsId, final OnClickListener listener) {
            mItems = mContext.getResources().getTextArray(itemsId);
            mItemLayoutResId = R.layout.select_dialog_item;
            mOnListItemClickListener = listener;
            mIsSingleChoice = false;
            return this;
        }

        public Builder setItems(CharSequence[] items,
                final OnClickListener listener) {
            mItems = items;
            mItemLayoutResId = R.layout.select_dialog_item;
            mOnListItemClickListener = listener;
            mIsSingleChoice = false;
            return this;
        }

        public Builder setSingleChoiceItems(int itemsId, int checkedItem,
                final OnClickListener listener) {
            mItems = mContext.getResources().getTextArray(itemsId);
            mItemLayoutResId = R.layout.select_dialog_singlechoice;
            mOnListItemClickListener = listener;
            mCheckedItem = checkedItem;
            mIsSingleChoice = true;
            return this;
        }

        public Builder setSingleChoiceItems(CharSequence[] items,
                int checkedItem, final OnClickListener listener) {
            mItems = items;
            mItemLayoutResId = R.layout.select_dialog_singlechoice;
            mOnListItemClickListener = listener;
            mCheckedItem = checkedItem;
            mIsSingleChoice = true;
            return this;
        }

        public Builder setCustomView(@LayoutRes int resId,
                @Nullable OnCreateCustomViewListener listener) {
            mCustomViewResId = resId;
            mOnCreateCustomViewListener = listener;
            return this;
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

        public Builder setOnButtonClickListener(OnClickListener listener) {
            mOnButtonClickListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }

        public Builder setOnCloseListener(OnCloseListener listener) {
            mOnCloseListener = listener;
            return this;
        }

        public Builder setStartPoint(int startX, int startY) {
            mStartX = startX;
            mStartY = startY;
            return this;
        }

        public @NonNull SimpleDialog build() {
            // Avoid memory leak
            mContext = null;

            SimpleDialog dialog = new SimpleDialog();
            dialog.setBuilder(this);
            return dialog;
        }

        public void show() {
            show(null);
        }

        public void show(Curtain curtain) {
            if (curtain == null) {
                curtain = new SimpleDialogCurtain(mStartX, mStartY);
            }
            build().show(curtain);
        }
    }

    public interface OnCreateCustomViewListener {
        void onCreateCustomView(SimpleDialog dialog, View view);
    }

    public interface OnClickListener {
        boolean onClick(SimpleDialog dialog, int which);
    }

    public interface OnCloseListener {
        void onClose(SimpleDialog dialog, boolean cancel);
    }
}
