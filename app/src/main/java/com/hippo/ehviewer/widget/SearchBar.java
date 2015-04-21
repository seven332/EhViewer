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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
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
import com.hippo.ehviewer.R;
import com.hippo.util.MathUtils;
import com.hippo.util.UiUtils;
import com.hippo.util.ViewUtils;
import com.hippo.widget.SimpleImageView;

import java.util.ArrayList;
import java.util.List;

public class SearchBar extends CardView implements View.OnClickListener,
        TextView.OnEditorActionListener {

    private static final long ANIMATION_TIME = 300;

    private Path mPath = new Path();
    private int mWidth;
    private int mHeight;
    private int mBaseHeight;
    private float mProgress;

    private SimpleImageView mMenuButton;
    private TextView mLogoTextView;
    private SimpleImageView mActionButton;
    private SearchEditText mEditText;
    private ListView mList;

    private DrawerArrowDrawable mDrawerArrowDrawable;
    private AddDeleteDrawable mAddDeleteDrawable;

    private List<String> mSuggestionList;
    private ArrayAdapter mSuggestionAdapter;

    private Helper mHelper;

    private boolean mInEditMode = false;
    private boolean mInAnimation = false;
    private boolean mFirstLayout = true;

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
        setRadius(UiUtils.dp2pix(context, 2));
        setCardElevation(UiUtils.dp2pix(context, 2));
        setCardBackgroundColor(Color.WHITE);
        setOnClickListener(this);

        LayoutInflater.from(context).inflate(R.layout.widget_search_bar, this);
        mMenuButton = (SimpleImageView) findViewById(R.id.menu);
        mLogoTextView = (TextView) findViewById(R.id.logo);
        mActionButton = (SimpleImageView) findViewById(R.id.action);
        mEditText = (SearchEditText) findViewById(R.id.search_edit_text);
        mList = (ListView) findViewById(R.id.search_bar_list);

        mDrawerArrowDrawable = new DrawerArrowDrawable(getContext());
        mAddDeleteDrawable = new AddDeleteDrawable(getContext());

        mMenuButton.setDrawable(mDrawerArrowDrawable);
        mMenuButton.setOnClickListener(this);
        mLogoTextView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Slabo.ttf"));
        mActionButton.setDrawable(mAddDeleteDrawable);
        mActionButton.setOnClickListener(this);
        mEditText.setSearchBar(this);
        mEditText.setOnEditorActionListener(this);

        mSuggestionList = new ArrayList<>();
        mSuggestionList.add("aaaaaaaaaaaaaaaaaaaaa");
        mSuggestionList.add("dfsgfewrewfes");
        mSuggestionList.add("dehyklrthrhbdsrf");
        mSuggestionList.add("粉色热污染份额为别人的");
        mSuggestionAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, mSuggestionList);
        mList.setAdapter(mSuggestionAdapter);
    }

    public void setHelper(Helper helper) {
        mHelper = helper;
    }

    @Override
    public void onClick(View v) {
        if (v == this) {
            setInEditMode();
        } else if (v == mMenuButton) {
            if (mInEditMode) {
                setInNormalMode();
            } else {
                mHelper.onClickMenu();
            }
        } else if (v == mActionButton) {
            if (mInEditMode) {
                // TODO when set prefix
                mEditText.setText("");
            } else {
                mHelper.onClickAction();
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mEditText) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL) {
                String query = mEditText.getText().toString();
                if (!TextUtils.isEmpty(query)) {
                    mHelper.onApplySearch(query);
                }
                return true;
            }
        }
        return false;
    }

    public void setInEditMode() {
        if (!mInEditMode) {
            mInEditMode = true;
            setClickable(false);
            ViewUtils.setVisibility(mLogoTextView, View.GONE);
            ViewUtils.setVisibility(mEditText, View.VISIBLE);
            mEditText.requestFocus();
            // show ime
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            // start animator
            ObjectAnimator oa1 = ObjectAnimator.ofFloat(mDrawerArrowDrawable, "progress", 0f, 1f);
            oa1.setDuration(ANIMATION_TIME);
            ObjectAnimator oa2 = ObjectAnimator.ofFloat(mAddDeleteDrawable, "progress", 0f, 1f);
            oa2.setDuration(ANIMATION_TIME);
            ObjectAnimator oa3 = ObjectAnimator.ofFloat(this, "progress", 0f, 1f);
            oa3.setDuration(ANIMATION_TIME);
            oa3.addListener(new SimpleAnimatorListener() {
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
                oa1.setAutoCancel(true);
                oa2.setAutoCancel(true);
                oa3.setAutoCancel(true);
            }
            oa1.start();
            oa2.start();
            oa3.start();
        }
    }

    public void setInNormalMode() {
        if (mInEditMode) {
            mInEditMode = false;
            setOnClickListener(this);
            ViewUtils.setVisibility(mLogoTextView, View.VISIBLE);
            ViewUtils.setVisibility(mEditText, View.GONE);
            // hide ime
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
            // start animator
            ObjectAnimator oa1 = ObjectAnimator.ofFloat(mDrawerArrowDrawable, "progress", 1f, 0f);
            oa1.setDuration(ANIMATION_TIME);
            ObjectAnimator oa2 = ObjectAnimator.ofFloat(mAddDeleteDrawable, "progress", 1f, 0f);
            oa2.setDuration(ANIMATION_TIME);
            ObjectAnimator oa3 = ObjectAnimator.ofFloat(this, "progress", 1f, 0f);
            oa3.setDuration(ANIMATION_TIME);
            oa3.addListener(new SimpleAnimatorListener() {
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
                oa1.setAutoCancel(true);
                oa2.setAutoCancel(true);
                oa3.setAutoCancel(true);
            }
            oa1.start();
            oa2.start();
            oa3.start();
        }
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

    public interface Helper {
        void onClickMenu();
        void onClickAction();
        void onApplySearch(String query);
    }
}
