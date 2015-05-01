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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.drawable.AddDeleteDrawable;
import com.hippo.drawable.DrawerArrowDrawable;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.util.MathUtils;
import com.hippo.util.Messenger;
import com.hippo.util.UiUtils;
import com.hippo.util.ViewUtils;
import com.hippo.widget.SimpleImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchBar extends CardView implements View.OnClickListener,
        TextView.OnEditorActionListener, TextWatcher,
        SearchEditText.SearchEditTextListener, Messenger.Receiver {

    private static final long ANIMATION_TIME = 300;

    private Path mPath = new Path();
    private int mWidth;
    private int mHeight;
    private int mBaseHeight;
    private float mProgress;

    private SimpleImageView mMenuButton;
    private TextView mTitleTextView;
    private SimpleImageView mActionButton;
    private SearchEditText mEditText;
    private ListView mList;

    private DrawerArrowDrawable mDrawerArrowDrawable;
    private AddDeleteDrawable mAddDeleteDrawable;

    private SearchDatabase mSearchDatabase;
    private List<String> mSuggestionList;
    private ArrayAdapter mSuggestionAdapter;

    private Helper mHelper;

    private boolean mInEditMode = false;
    private boolean mInAnimation = false;
    private boolean mFirstLayout = true;

    private int mSource;

    public SearchBar(Context context) {
        super(context);
        init(context);
    }

    public SearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mSearchDatabase = SearchDatabase.getInstance(getContext());

        setRadius(UiUtils.dp2pix(context, 2));
        setCardElevation(UiUtils.dp2pix(context, 2));
        setCardBackgroundColor(Color.WHITE);

        LayoutInflater.from(context).inflate(R.layout.widget_search_bar, this);
        mMenuButton = (SimpleImageView) findViewById(R.id.search_menu);
        mTitleTextView = (TextView) findViewById(R.id.search_title);
        mActionButton = (SimpleImageView) findViewById(R.id.search_action);
        mEditText = (SearchEditText) findViewById(R.id.search_edit_text);
        mList = (ListView) findViewById(R.id.search_bar_list);

        mDrawerArrowDrawable = new DrawerArrowDrawable(getContext());
        mAddDeleteDrawable = new AddDeleteDrawable(getContext());

        mTitleTextView.setOnClickListener(this);
        mMenuButton.setDrawable(mDrawerArrowDrawable);
        mMenuButton.setOnClickListener(this);
        mActionButton.setDrawable(mAddDeleteDrawable);
        mActionButton.setOnClickListener(this);
        mEditText.setSearchEditTextListener(this);
        mEditText.setOnEditorActionListener(this);
        mEditText.addTextChangedListener(this);

        mSuggestionList = new ArrayList<>();
        // TODO
        mSuggestionAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, mSuggestionList);
        mList.setAdapter(mSuggestionAdapter);

        // TODO get source from config
        setSource(EhClient.SOURCE_EX);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Messenger.getInstance().register(Constants.MESSENGER_ID_SOURCE, this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Messenger.getInstance().unregister(Constants.MESSENGER_ID_SOURCE, this);
    }

    private void updateSuggestions() {
        String prefix = mEditText.getText().toString();
        String[] suggestions = mSearchDatabase.getSuggestions(prefix);
        mSuggestionList.clear();
        Collections.addAll(mSuggestionList, suggestions);
        mSuggestionAdapter.notifyDataSetChanged();
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public void setSource(int source) {
        if (mSource != source) {
            Resources resources = getContext().getResources();
            Drawable searchImage = resources.getDrawable(R.drawable.ic_search);
            SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
            ssb.append(String.format(resources.getString(R.string.search_bar_hint),
                    EhClient.getReadableHost(source)));
            int textSize = (int) (mEditText.getTextSize() * 1.25);
            searchImage.setBounds(0, 0, textSize, textSize);
            ssb.setSpan(new ImageSpan(searchImage), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mEditText.setHint(ssb);

            mSource = source;
        }
    }

    public void setHelper(Helper helper) {
        mHelper = helper;
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    @Override
    public void onClick(View v) {
        if (v == mTitleTextView) {
            mHelper.onClickTitle();
        } else if (v == mMenuButton) {
            if (mInEditMode) {
                mHelper.onClickArrow();
            } else {
                mHelper.onClickMenu();
            }
        } else if (v == mActionButton) {
            if (mInEditMode) {
                mEditText.setText("");
            } else {
                mHelper.onClickAdvanceSearch();
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mEditText) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL) {
                String query = mEditText.getText().toString();
                mHelper.onApplySearch(query);
                return true;
            }
        }
        return false;
    }

    public void setInEditMode(boolean showList) {
        if (!mInEditMode) {
            mInEditMode = true;
            setClickable(false);
            ViewUtils.setVisibility(mTitleTextView, View.GONE);
            ViewUtils.setVisibility(mEditText, View.VISIBLE);
            mEditText.requestFocus();
            // start animator
            if (mDrawerArrowDrawable.getProgress() != 1f) {
                ObjectAnimator oa1 = ObjectAnimator.ofFloat(mDrawerArrowDrawable, "progress", 0f, 1f);
                oa1.setDuration(ANIMATION_TIME);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    oa1.setAutoCancel(true);
                }
                oa1.start();
            }
            if (mAddDeleteDrawable.getProgress() != 1f) {
                ObjectAnimator oa2 = ObjectAnimator.ofFloat(mAddDeleteDrawable, "progress", 0f, 1f);
                oa2.setDuration(ANIMATION_TIME);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    oa2.setAutoCancel(true);
                }
                oa2.start();
            }
            // Show list if needed
            if (showList) {
                showImeAndSuggestionsList();
            }
        }
    }

    public void setInNormalMode() {
        if (mInEditMode) {
            mInEditMode = false;
            setOnClickListener(this);
            ViewUtils.setVisibility(mTitleTextView, View.VISIBLE);
            ViewUtils.setVisibility(mEditText, View.GONE);
            // start animator
            if (mDrawerArrowDrawable.getProgress() != 0f) {
                ObjectAnimator oa1 = ObjectAnimator.ofFloat(mDrawerArrowDrawable, "progress", 1f, 0f);
                oa1.setDuration(ANIMATION_TIME);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    oa1.setAutoCancel(true);
                }
                oa1.start();
            }
            if (mAddDeleteDrawable.getProgress() != 0f) {
                ObjectAnimator oa2 = ObjectAnimator.ofFloat(mAddDeleteDrawable, "progress", 1f, 0f);
                oa2.setDuration(ANIMATION_TIME);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    oa2.setAutoCancel(true);
                }
                oa2.start();
            }
            // Hide ime and suggestions list
            hideImeAndSuggestionsList();
        }
    }

    public void showImeAndSuggestionsList() {
        // Show ime
        mEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditText, 0);
        // update suggestion for show suggestions list
        updateSuggestions();
        // Show suggestions list
        if (mProgress != 1f) {
            ObjectAnimator oa = ObjectAnimator.ofFloat(this, "progress", 0f, 1f);
            oa.setDuration(ANIMATION_TIME);
            oa.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ViewUtils.setVisibility(mList, View.VISIBLE);
                    mInAnimation = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mInAnimation = false;
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                oa.setAutoCancel(true);
            }
            oa.start();
        }
    }

    public void hideImeAndSuggestionsList() {
        // Hide ime
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        // Hide suggestions list
        if (mProgress != 0f) {
            ObjectAnimator oa = ObjectAnimator.ofFloat(this, "progress", 1f, 0f);
            oa.setDuration(ANIMATION_TIME);
            oa.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mInAnimation = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ViewUtils.setVisibility(mList, View.GONE);
                    mInAnimation = false;
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                oa.setAutoCancel(true);
            }
            oa.start();
        }
    }

    public void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mInEditMode) {
            mWidth = right - left;
            mHeight = bottom - top;
        }
        if (mFirstLayout) {
            mFirstLayout = false;
            mBaseHeight = bottom - top;
        }
    }

    @SuppressWarnings("unused")
    public void setProgress(float progress) {
        mProgress = progress;
        invalidate();
    }

    @SuppressWarnings("unused")
    public float getProgress() {
        return mProgress;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mInAnimation) {
            final int state = canvas.save();
            float bottom = MathUtils.lerp(mBaseHeight, mHeight, mProgress);
            mPath.rewind();
            mPath.addRect(0f, 0f, mWidth, bottom, Path.Direction.CW);
            canvas.clipPath(mPath);
            super.draw(canvas);
            canvas.restoreToCount(state);
        } else {
            super.draw(canvas);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Empty
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Empty
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateSuggestions();
    }

    @Override
    public void onClick() {
        mHelper.onSearchEditTextClick();
    }

    @Override
    public void onBackPressed() {
        mHelper.onBackPressed();
    }

    @Override
    public void onReceive(int id, Object obj) {
        if (id == Constants.MESSENGER_ID_SOURCE) {
            if (obj instanceof Integer) {
                int source = (Integer) obj;
                setSource(source);
            }
        }
    }

    public interface Helper {
        void onClickTitle();
        void onClickMenu();
        void onClickArrow();
        void onClickAdvanceSearch();
        void onSearchEditTextClick();
        void onApplySearch(String query);
        void onBackPressed();
    }
}
