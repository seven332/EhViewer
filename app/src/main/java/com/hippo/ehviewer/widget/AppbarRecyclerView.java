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

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.rippleold.RippleSalon;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.widget.recyclerview.LinearDividerItemDecoration;
import com.hippo.yorozuya.LayoutUtils;

public class AppbarRecyclerView extends LinearLayout implements View.OnClickListener {

    public TextView mTitle;
    public View mPlus;
    public View mSettings;
    public ViewGroup mTip;
    public TextView mTipTextView;
    public EasyRecyclerView mRecyclerView;

    public ViewTransition mViewTransition;

    public int mOriginalPaddingBottom;

    public Helper mHelper;

    public AppbarRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public AppbarRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AppbarRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        setOrientation(VERTICAL);

        LayoutInflater.from(context).inflate(R.layout.widget_appbar_recycler_view, this);
        mTitle = (TextView) findViewById(R.id.title);
        mPlus = findViewById(R.id.plus);
        mSettings = findViewById(R.id.settings);
        mTip = (ViewGroup) findViewById(R.id.tip);
        mTipTextView = (TextView) mTip.findViewById(R.id.text_view);
        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        mRecyclerView.setDrawSelectorOnTop(true);
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL,
                context.getResources().getColor(R.color.divider_light),
                LayoutUtils.dp2pix(context, 1));
        decoration.setOverlap(true);
        mRecyclerView.addItemDecoration(decoration);

        mPlus.setOnClickListener(this);
        mSettings.setOnClickListener(this);

        RippleSalon.addRipple(mPlus, true);
        RippleSalon.addRipple(mSettings, true);

        mOriginalPaddingBottom = mRecyclerView.getPaddingBottom();

        mViewTransition = new ViewTransition(mTip, mRecyclerView);
    }

    public void setHelper(Helper helper) {
        mHelper = helper;
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setTipText(String tipText) {
        mTipTextView.setText(tipText);
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        mRecyclerView.setAdapter(adapter);
    }

    public void addOnChildAttachStateChangeListener(RecyclerView.OnChildAttachStateChangeListener listener) {
        mRecyclerView.addOnChildAttachStateChangeListener(listener);
    }

    public void showTip(boolean animation) {
        mViewTransition.showView(0, animation);
    }

    public void showRecyclerView(boolean animation) {
        mViewTransition.showView(1, animation);
    }

    public void setOnItemClickListener(EasyRecyclerView.OnItemClickListener listener) {
        mRecyclerView.setOnItemClickListener(listener);
    }

    public int getChildAdapterPosition(View view) {
        return mRecyclerView.getChildAdapterPosition(view);
    }

    public EasyRecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void setFitPaddingBottom(int fitPaddingBottom) {
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight(), mOriginalPaddingBottom + fitPaddingBottom);
    }

    public void setPlusVisibility(int visibility) {
        mPlus.setVisibility(visibility);
    }

    public void setSettingsVisibility(int visibility) {
        mSettings.setVisibility(visibility);
    }

    @Override
    public void onClick(View v) {
        if (mPlus == v) {
            if (mHelper != null) {
                mHelper.onClickPlusListener();
            }
        } else if (mSettings == v) {
            if (mHelper != null) {
                mHelper.onClickSettingsListener();
            }
        }
    }

    public interface Helper {

        void onClickPlusListener();

        void onClickSettingsListener();
    }
}
