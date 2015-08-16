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

package com.hippo.ehviewer.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.cardsalon.CardView;
import com.hippo.drawable.AddDeleteDrawable;
import com.hippo.drawable.DrawerArrowDrawable;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.widget.SimpleImageView;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.ViewUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchBar extends CardView implements View.OnClickListener,
        TextView.OnEditorActionListener, TextWatcher,
        SearchEditText.SearchEditTextListener {

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_STATE = "state";

    private static final long ANIMATE_TIME = 300l;

    public static final int STATE_NORMAL = 0;
    public static final int STATE_SEARCH = 1;
    public static final int STATE_SEARCH_LIST = 2;

    private int mState = STATE_NORMAL;

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

    private ViewTransition mViewTransition;

    private DrawerArrowDrawable mDrawerArrowDrawable;
    private AddDeleteDrawable mAddDeleteDrawable;

    private SearchDatabase mSearchDatabase;
    private List<String> mSuggestionList;
    private ArrayAdapter mSuggestionAdapter;

    private Helper mHelper;

    private boolean mInAnimation = false;

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

        LayoutInflater.from(context).inflate(R.layout.widget_search_bar, this);
        mMenuButton = (SimpleImageView) findViewById(R.id.search_menu);
        mTitleTextView = (TextView) findViewById(R.id.search_title);
        mActionButton = (SimpleImageView) findViewById(R.id.search_action);
        mEditText = (SearchEditText) findViewById(R.id.search_edit_text);
        mList = (ListView) findViewById(R.id.search_bar_list);

        mViewTransition = new ViewTransition(mTitleTextView, mEditText);

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

        // Get base height
        ViewUtils.measureView(this, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mBaseHeight = getMeasuredHeight();

        mSuggestionList = new ArrayList<>();
        // TODO Use custom view
        mSuggestionAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, mSuggestionList);
        mList.setAdapter(mSuggestionAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String suggestion = mSuggestionList.get(position);
                mEditText.setText(suggestion);
                mEditText.setSelection(suggestion.length());
            }
        });
    }

    private void updateSuggestions() {
        String prefix = mEditText.getText().toString();
        String[] suggestions = mSearchDatabase.getSuggestions(prefix);
        mSuggestionList.clear();
        Collections.addAll(mSuggestionList, suggestions);
        mSuggestionAdapter.notifyDataSetChanged();
    }

    public float getEditTextTextSize() {
        return mEditText.getTextSize();
    }

    public void setEditTextHint(CharSequence hint) {
        mEditText.setHint(hint);
    }

    public void setHelper(Helper helper) {
        mHelper = helper;
    }

    public void setText(String text) {
        mEditText.setText(text);
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    @Override
    public void onClick(View v) {
        if (v == mTitleTextView) {
            mHelper.onClickTitle();
        } else if (v == mMenuButton) {
            if (mState == STATE_NORMAL) {
                mHelper.onClickMenu();
            } else {
                mHelper.onClickArrow();
            }
        } else if (v == mActionButton) {
            if (mState == STATE_NORMAL) {
                mHelper.onClickAdvanceSearch();
            } else {
                mEditText.setText("");
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

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        setState(state, true);
    }

    public void setState(int state, boolean animation) {
        if (mState != state) {
            int oldState = mState;
            mState = state;

            switch (oldState) {
                default:
                case STATE_NORMAL:
                    mViewTransition.showView(1, animation);
                    mEditText.requestFocus();
                    mDrawerArrowDrawable.setArrow(animation ? ANIMATE_TIME : 0);
                    mAddDeleteDrawable.setDelete(animation ? ANIMATE_TIME : 0);
                    if (state == STATE_SEARCH_LIST) {
                        showImeAndSuggestionsList(animation);
                    }
                    break;
                case STATE_SEARCH:
                    if (state == STATE_NORMAL) {
                        mViewTransition.showView(0, animation);
                        mDrawerArrowDrawable.setMenu(animation ? ANIMATE_TIME : 0);
                        mAddDeleteDrawable.setAdd(animation ? ANIMATE_TIME : 0);
                    } else if (state == STATE_SEARCH_LIST) {
                        showImeAndSuggestionsList(animation);
                    }
                    break;
                case STATE_SEARCH_LIST:
                    hideImeAndSuggestionsList(animation);
                    if (state == STATE_NORMAL) {
                        mViewTransition.showView(0, animation);
                        mDrawerArrowDrawable.setMenu(animation ? ANIMATE_TIME : 0);
                        mAddDeleteDrawable.setAdd(animation ? ANIMATE_TIME : 0);
                    }
                    break;
            }
        }
    }

    private void showImeAndSuggestionsList() {
        showImeAndSuggestionsList(true);
    }

    private void showImeAndSuggestionsList(boolean animation) {
        // Show ime
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditText, 0);
        // update suggestion for show suggestions list
        updateSuggestions();
        // Show suggestions list
        if (animation) {
            ObjectAnimator oa = ObjectAnimator.ofFloat(this, "progress", 1f);
            oa.setDuration(ANIMATE_TIME);
            oa.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mList.setVisibility(View.VISIBLE);
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
        } else {
            mList.setVisibility(View.VISIBLE);
            setProgress(1f);
        }
    }

    private void hideImeAndSuggestionsList() {
        hideImeAndSuggestionsList(true);
    }

    private void hideImeAndSuggestionsList(boolean animation) {
        // Hide ime
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        // Hide suggestions list
        if (animation) {
            ObjectAnimator oa = ObjectAnimator.ofFloat(this, "progress", 0f);
            oa.setDuration(ANIMATE_TIME);
            oa.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mInAnimation = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mList.setVisibility(View.GONE);
                    mInAnimation = false;
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                oa.setAutoCancel(true);
            }
            oa.start();
        } else {
            setProgress(0f);
            mList.setVisibility(View.GONE);
        }
    }

    public void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mList.getVisibility() == View.VISIBLE) {
            mWidth = right - left;
            mHeight = bottom - top;
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
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putInt(STATE_KEY_STATE, mState);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            setState(savedState.getInt(STATE_KEY_STATE), false);
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
