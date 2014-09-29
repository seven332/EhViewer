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

package com.hippo.ehviewer.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hippo.ehviewer.R;

public class MaterialAlertDialog extends AlertDialog implements View.OnClickListener, AdapterView.OnItemClickListener {

    public final static int ACTION = 0;
    public final static int POSITIVE = 1;
    public final static int NEGATIVE = 2;
    public final static int NEUTRAL = 3;

    private final Context mContext;

    private final Builder mBuilder;

    private View mRootView;
    private Button mActionButton;
    private Button mPositiveButton;
    private Button mNegativeButton;
    private Button mNeutralButton;

    private MaterialAlertDialog(Builder builder) {
        super(builder.mContext);

        mContext = builder.mContext;
        mBuilder = builder;

        init();
    }

    @SuppressLint("InflateParams")
    private void init() {
        mRootView = LayoutInflater.from(mContext).inflate(
                R.layout.alert_dialog_material, null);

        // Title
        TextView title = (TextView) mRootView.findViewById(R.id.title);
        if (mBuilder.mTitle == null)
            title.setVisibility(View.GONE);
        else
            title.setText(mBuilder.mTitle);

        TextView message = (TextView) mRootView.findViewById(R.id.message);
        ListView list = (ListView) mRootView.findViewById(R.id.list);
        if (mBuilder.mCustomView != null) {
            // CustomView
            message.setVisibility(View.GONE);
            list.setVisibility(View.GONE);
            LinearLayout topPanel = (LinearLayout) mRootView.findViewById(R.id.top_panel);
            LinearLayout.LayoutParams lp = mBuilder.mCustomLp;
            if (lp == null)
                lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
            topPanel.addView(mBuilder.mCustomView, lp);
        } else if (mBuilder.mAdapter != null) {
            // List
            message.setVisibility(View.GONE);
            list.setAdapter(mBuilder.mAdapter);
            list.setOnItemClickListener(this);
            if (mBuilder.mIsSingleChoice) {
                list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                if (mBuilder.mCheckedItem > -1) {
                    list.setItemChecked(mBuilder.mCheckedItem, true);
                    //list.setSelection(mBuilder.mCheckedItem);
                }
            }
        } else {
            // Message
            list.setVisibility(View.GONE);
            if (mBuilder.mAutoLinkMask != -1)
                message.setAutoLinkMask(mBuilder.mAutoLinkMask);
            message.setText(mBuilder.mMessage);
        }

        // Buttons
        View buttonPanel = mRootView.findViewById(R.id.button_panel);
        View nonActionPanel = mRootView.findViewById(R.id.non_action_panel);
        mActionButton = (Button) mRootView.findViewById(R.id.button_action);
        mPositiveButton = (Button) mRootView.findViewById(R.id.button_positive);
        mNegativeButton = (Button) mRootView.findViewById(R.id.button_negative);
        mNeutralButton = (Button) mRootView.findViewById(R.id.button_neutral);
        boolean hasAction = mBuilder.mActionButtonText != null;
        boolean hasNonAction = mBuilder.mPositiveButtonText != null
                || mBuilder.mNegativeButtonText != null
                || mBuilder.mNeutralButtonText != null;
        boolean hasButton = hasAction || hasNonAction;
        if (hasButton) {
            if (hasAction) {
                mActionButton.setText(mBuilder.mActionButtonText);
                mActionButton.setOnClickListener(this);
            } else {
                mActionButton.setVisibility(View.GONE);
            }
            if (hasNonAction) {
                // Positive
                if (mBuilder.mPositiveButtonText != null) {
                    mPositiveButton.setText(mBuilder.mPositiveButtonText);
                    mPositiveButton.setOnClickListener(this);
                } else {
                    mPositiveButton.setVisibility(View.GONE);
                }
                // Negative
                if (mBuilder.mNegativeButtonText != null) {
                    mNegativeButton.setText(mBuilder.mNegativeButtonText);
                    mNegativeButton.setOnClickListener(this);
                } else {
                    mNegativeButton.setVisibility(View.GONE);
                }
                // Neutral
                if (mBuilder.mNeutralButtonText != null) {
                    mNeutralButton.setText(mBuilder.mNeutralButtonText);
                    mNeutralButton.setOnClickListener(this);
                } else {
                    mNeutralButton.setVisibility(View.GONE);
                }
            } else {
                nonActionPanel.setVisibility(View.GONE);
            }
        } else {
            buttonPanel.setVisibility(View.GONE);
        }

        setView(mRootView);

        setCancelable(mBuilder.mCancelable);

        // Try to keep title visible
        final ScrollView sv = ((ScrollView) mRootView.findViewById(R.id.scrollView));
        sv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right,
                    int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                sv.scrollTo(0, 0);
            }
        });
    }

    @Override
    public void onClick(View v) {
        boolean close = true;

        if (mBuilder.mButtonListener != null) {
            if (v == mActionButton)
                close = mBuilder.mButtonListener.onClick(this, ACTION);
            else if (v == mPositiveButton)
                close = mBuilder.mButtonListener.onClick(this, POSITIVE);
            else if (v == mNegativeButton)
                close = mBuilder.mButtonListener.onClick(this, NEGATIVE);
            else if (v == mNeutralButton)
                close = mBuilder.mButtonListener.onClick(this, NEUTRAL);
        }

        if (close)
            dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {

        boolean close = false;

        if (mBuilder.mOnClickListener != null)
            close = mBuilder.mOnClickListener.onClick(this, position);

        if (close)
            dismiss();
    }


    public static interface OnClickListener {
        public boolean onClick(MaterialAlertDialog dialog, int which);
    }

    public static class Builder {

        private final Context mContext;

        private CharSequence mTitle;
        private CharSequence mMessage;
        private CharSequence mActionButtonText;
        private CharSequence mPositiveButtonText;
        private CharSequence mNegativeButtonText;
        private CharSequence mNeutralButtonText;
        private OnClickListener mButtonListener;
        private View mCustomView;
        private LinearLayout.LayoutParams mCustomLp;
        private boolean mCancelable = true;
        private int mAutoLinkMask = -1;

        private ListAdapter mAdapter;
        private OnClickListener mOnClickListener;
        private int mCheckedItem;
        private boolean mIsSingleChoice;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder setTitle(int resId) {
            return setTitle(mContext.getString(resId));
        }

        public Builder setMessage(CharSequence message) {
            mMessage = message;
            return this;
        }

        public Builder setMessage(int resId) {
            return setMessage(mContext.getString(resId));
        }

        public Builder setActionButton(CharSequence text) {
            mActionButtonText = text;
            return this;
        }

        public Builder setActionButton(int resId) {
            return setActionButton(mContext.getString(resId));
        }

        public Builder setPositiveButton(CharSequence text) {
            mPositiveButtonText = text;
            return this;
        }

        public Builder setPositiveButton(int resId) {
            return setPositiveButton(mContext.getString(resId));
        }

        public Builder setNegativeButton(CharSequence text) {
            mNegativeButtonText = text;
            return this;
        }

        public Builder setNegativeButton(int resId) {
            return setNegativeButton(mContext.getString(resId));
        }

        public Builder setNeutralButton(CharSequence text) {
            mNeutralButtonText = text;
            return this;
        }

        public Builder setNeutralButton(int resId) {
            return setNeutralButton(mContext.getString(resId));
        }

        public Builder setButtonListener(OnClickListener l) {
            mButtonListener = l;
            return this;
        }

        public Builder setView(View view) {
            mCustomView = view;
            return this;
        }

        public Builder setView(View view, LinearLayout.LayoutParams lp) {
            mCustomView = view;
            mCustomLp = lp;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }

        public Builder setMessageAutoLink(int mask) {
            mAutoLinkMask = mask;
            return this;
        }

        public Builder setItems(int itemsId, final OnClickListener listener) {
            mAdapter = new ArrayAdapter<CharSequence>(mContext,
                    R.layout.select_dialog_item, android.R.id.text1,
                    mContext.getResources().getTextArray(itemsId));
            mOnClickListener = listener;
            mIsSingleChoice = false;
            return this;
        }

        public Builder setItems(CharSequence[] items, final OnClickListener listener) {
            mAdapter = new ArrayAdapter<CharSequence>(mContext,
                    R.layout.select_dialog_item, android.R.id.text1, items);
            mOnClickListener = listener;
            mIsSingleChoice = false;
            return this;
        }

        public Builder setAdapter(final ListAdapter adapter, final OnClickListener listener) {
            mAdapter = adapter;
            mOnClickListener = listener;
            mIsSingleChoice = false;
            return this;
        }

        public Builder setSingleChoiceItems(int itemsId, int checkedItem,
                final OnClickListener listener) {
            mAdapter = new ArrayAdapter<CharSequence>(mContext,
                    R.layout.select_dialog_singlechoice, android.R.id.text1,
                    mContext.getResources().getTextArray(itemsId));
            mOnClickListener = listener;
            mCheckedItem = checkedItem;
            mIsSingleChoice = true;
            return this;
        }

        public Builder setSingleChoiceItems(CharSequence[] items, int checkedItem, final OnClickListener listener) {
            mAdapter = new ArrayAdapter<CharSequence>(mContext,
                    R.layout.select_dialog_singlechoice, android.R.id.text1, items);
            mOnClickListener = listener;
            mCheckedItem = checkedItem;
            mIsSingleChoice = true;
            return this;
        }

        public Builder setSingleChoiceItems(ListAdapter adapter, int checkedItem, final OnClickListener listener) {
            mAdapter = adapter;
            mOnClickListener = listener;
            mCheckedItem = checkedItem;
            mIsSingleChoice = true;
            return this;
        }

        public MaterialAlertDialog create() {
            return new MaterialAlertDialog(this);
        }

        public MaterialAlertDialog show() {
            MaterialAlertDialog dialog = create();
            dialog.show();
            return dialog;
        }
    }
}
