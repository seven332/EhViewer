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
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.ehviewer.R;

public class MaterialAlertDialog extends AlertDialog implements
        View.OnClickListener, AdapterView.OnItemClickListener {

    public final static int ACTION = 0x1;
    public final static int POSITIVE = 0x2;
    public final static int NEGATIVE = 0x4;
    public final static int NEUTRAL = 0x8;

    private final Context mContext;

    private final Builder mBuilder;

    private View mRootView;
    private TextView mTitle;
    private Button mActionButton;
    private Button mPositiveButton;
    private Button mNegativeButton;
    private Button mNeutralButton;

    protected MaterialAlertDialog(Builder builder) {
        // super(builder.mContext);
        super(new ContextThemeWrapper(builder.mContext, R.style.AppTheme_Main));
        mContext = builder.mContext;
        mBuilder = builder;

        init();
    }

    @SuppressLint("InflateParams")
    private void init() {
        mRootView = LayoutInflater.from(mContext).inflate(
                R.layout.alert_dialog_material, null);

        // Check in scroll view or not
        boolean inScrollView;
        if (mBuilder.mCustomView != null)
            inScrollView = mBuilder.mInScrollView;
        else if (mBuilder.mAdapter != null)
            inScrollView = false;
        else
            inScrollView = true;
        // Get widget
        LinearLayout topPanel;
        TextView message = null;
        ListView list = null;
        if (inScrollView) {
            mRootView.findViewById(R.id.top_panel_noscroll).setVisibility(
                    View.GONE);
            mTitle = (TextView) mRootView.findViewById(R.id.title);
            topPanel = (LinearLayout) mRootView.findViewById(R.id.top_panel);
            message = (TextView) mRootView.findViewById(R.id.message);
        } else {
            mRootView.findViewById(R.id.scroll_view).setVisibility(View.GONE);
            mTitle = (TextView) mRootView.findViewById(R.id.title2);
            topPanel = (LinearLayout) mRootView
                    .findViewById(R.id.top_panel_noscroll);
            list = (ListView) mRootView.findViewById(R.id.list);
        }
        // Title
        if (mBuilder.mTitle == null)
            mTitle.setVisibility(View.GONE);
        else
            mTitle.setText(mBuilder.mTitle);
        // Content
        if (mBuilder.mCustomView != null) {
            // CustomView
            if (message != null)
                message.setVisibility(View.GONE);
            if (list != null)
                list.setVisibility(View.GONE);
            LinearLayout.LayoutParams lp = mBuilder.mCustomLp;
            if (lp == null)
                lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
            mBuilder.mCustomView.setId(R.id.custom);
            topPanel.addView(mBuilder.mCustomView, lp);
        } else if (mBuilder.mAdapter != null) {
            // List
            if (message != null)
                message.setVisibility(View.GONE);
            list.setAdapter(mBuilder.mAdapter);
            list.setOnItemClickListener(this);
            if (mBuilder.mIsSingleChoice) {
                list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                if (mBuilder.mCheckedItem > -1) {
                    list.setItemChecked(mBuilder.mCheckedItem, true);
                    list.setSelection(mBuilder.mCheckedItem);
                }
            }
        } else {
            // Message
            if (list != null)
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
        if (mBuilder.mOnCancelListener != null)
            setOnCancelListener(mBuilder.mOnCancelListener);
        if (mBuilder.mOnDismissListener != null)
            setOnDismissListener(mBuilder.mOnDismissListener);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle.setText(title);
    }

    @Override
    public void setTitle(int resId) {
        setTitle(getContext().getString(resId));
    }

    public void setView(View view, boolean inScrollView) {
        setView(view, inScrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    public void setView(View view, boolean inScrollView,
            LinearLayout.LayoutParams lp) {
        mRootView.findViewById(inScrollView ? R.id.top_panel_noscroll : R.id.scroll_view).setVisibility(View.GONE);
        mRootView.findViewById(inScrollView ? R.id.scroll_view : R.id.top_panel_noscroll).setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.message).setVisibility(View.GONE);
        mRootView.findViewById(R.id.list).setVisibility(View.GONE);
        LinearLayout topPanel = (LinearLayout) mRootView.findViewById(inScrollView ? R.id.top_panel : R.id.top_panel_noscroll);
        topPanel.addView(view, lp);
        // Title
        TextView oldTitle = mTitle;
        mTitle = (TextView) mRootView.findViewById(inScrollView ? R.id.title : R.id.title2);
        if (mTitle != oldTitle && oldTitle != null)
            mTitle.setText(oldTitle.getText());
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

        protected final Context mContext;

        protected CharSequence mTitle;
        protected CharSequence mMessage;
        protected CharSequence mActionButtonText;
        protected CharSequence mPositiveButtonText;
        protected CharSequence mNegativeButtonText;
        protected CharSequence mNeutralButtonText;
        protected OnClickListener mButtonListener;
        protected View mCustomView;
        protected LinearLayout.LayoutParams mCustomLp;
        protected boolean mInScrollView = true;
        protected boolean mCancelable = true;
        protected int mAutoLinkMask = -1;

        protected OnCancelListener mOnCancelListener;

        protected ListAdapter mAdapter;
        protected OnClickListener mOnClickListener;
        protected int mCheckedItem;
        protected boolean mIsSingleChoice;

        protected DialogInterface.OnDismissListener mOnDismissListener;

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

        public Builder setDefaultButton(int b) {
            if ((b & POSITIVE) == POSITIVE)
                mPositiveButtonText = mContext.getString(android.R.string.ok);
            if ((b & NEGATIVE) == NEGATIVE)
                mNegativeButtonText = mContext.getString(android.R.string.cancel);
            return this;
        }

        public Builder setButtonListener(OnClickListener l) {
            mButtonListener = l;
            return this;
        }

        /**
         * Custom view id will be R.id.custom
         *
         * @param view
         * @param inScrollView
         * @return
         */
        public Builder setView(View view, boolean inScrollView) {
            mCustomView = view;
            mInScrollView = inScrollView;
            return this;
        }

        /**
         * Custom view id will be R.id.custom
         *
         * @param view
         * @param inScrollView
         * @param lp
         * @return
         */
        public Builder setView(View view, boolean inScrollView,
                LinearLayout.LayoutParams lp) {
            mCustomView = view;
            mInScrollView = inScrollView;
            mCustomLp = lp;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }

        public Builder setOnCancelListener(OnCancelListener onCancelListener) {
            mOnCancelListener = onCancelListener;
            return this;
        }

        public Builder setMessageAutoLink(int mask) {
            mAutoLinkMask = mask;
            return this;
        }

        public Builder setItems(int itemsId, final OnClickListener listener) {
            mAdapter = new ArrayAdapter<CharSequence>(mContext,
                    R.layout.select_dialog_item, android.R.id.text1, mContext
                            .getResources().getTextArray(itemsId));
            mOnClickListener = listener;
            mIsSingleChoice = false;
            return this;
        }

        public Builder setItems(CharSequence[] items,
                final OnClickListener listener) {
            mAdapter = new ArrayAdapter<CharSequence>(mContext,
                    R.layout.select_dialog_item, android.R.id.text1, items);
            mOnClickListener = listener;
            mIsSingleChoice = false;
            return this;
        }

        public Builder setAdapter(final ListAdapter adapter,
                final OnClickListener listener) {
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

        public Builder setSingleChoiceItems(CharSequence[] items,
                int checkedItem, final OnClickListener listener) {
            mAdapter = new ArrayAdapter<CharSequence>(mContext,
                    R.layout.select_dialog_singlechoice, android.R.id.text1,
                    items);
            mOnClickListener = listener;
            mCheckedItem = checkedItem;
            mIsSingleChoice = true;
            return this;
        }

        public Builder setSingleChoiceItems(ListAdapter adapter,
                int checkedItem, final OnClickListener listener) {
            mAdapter = adapter;
            mOnClickListener = listener;
            mCheckedItem = checkedItem;
            mIsSingleChoice = true;
            return this;
        }

        public Builder setOnDismissListener(DialogInterface.OnDismissListener l) {
            mOnDismissListener = l;
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
