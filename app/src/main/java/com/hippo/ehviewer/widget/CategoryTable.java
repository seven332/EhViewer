/*
 * Copyright (C) 2014-2015 Hippo Seven
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

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TableLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.util.Utils;
import com.hippo.widget.CheckTextView;

public class CategoryTable extends TableLayout {

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_CATEGORY = "category";

    private CheckTextView mDoujinshi;
    private CheckTextView mManga;
    private CheckTextView mArtistCG;
    private CheckTextView mGameCG;
    private CheckTextView mWestern;
    private CheckTextView mNonH;
    private CheckTextView mImageSets;
    private CheckTextView mCosplay;
    private CheckTextView mAsianPorn;
    private CheckTextView mMisc;

    public CategoryTable(Context context) {
        super(context);
        init();
    }

    public CategoryTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.widget_category_table, this);

        ViewGroup row0 = (ViewGroup) getChildAt(0);
        mDoujinshi = (CheckTextView) row0.getChildAt(0);
        mManga = (CheckTextView) row0.getChildAt(1);

        ViewGroup row1 = (ViewGroup) getChildAt(1);
        mArtistCG = (CheckTextView) row1.getChildAt(0);
        mGameCG = (CheckTextView) row1.getChildAt(1);

        ViewGroup row2 = (ViewGroup) getChildAt(2);
        mWestern = (CheckTextView) row2.getChildAt(0);
        mNonH = (CheckTextView) row2.getChildAt(1);

        ViewGroup row3 = (ViewGroup) getChildAt(3);
        mImageSets = (CheckTextView) row3.getChildAt(0);
        mCosplay = (CheckTextView) row3.getChildAt(1);

        ViewGroup row4 = (ViewGroup) getChildAt(4);
        mAsianPorn = (CheckTextView) row4.getChildAt(0);
        mMisc = (CheckTextView) row4.getChildAt(1);
    }

    /**
     * Set each button checked or not according to category.
     *
     * @param category target category
     */
    public void setCategory(int category) {
        mDoujinshi.setChecked(!Utils.int2boolean(category & EhUtils.DOUJINSHI), false);
        mManga.setChecked(!Utils.int2boolean(category & EhUtils.MANGA), false);
        mArtistCG.setChecked(!Utils.int2boolean(category & EhUtils.ARTIST_CG), false);
        mGameCG.setChecked(!Utils.int2boolean(category & EhUtils.GAME_CG), false);
        mWestern.setChecked(!Utils.int2boolean(category & EhUtils.WESTERN), false);
        mNonH.setChecked(!Utils.int2boolean(category & EhUtils.NON_H), false);
        mImageSets.setChecked(!Utils.int2boolean(category & EhUtils.IMAGE_SET), false);
        mCosplay.setChecked(!Utils.int2boolean(category & EhUtils.COSPLAY), false);
        mAsianPorn.setChecked(!Utils.int2boolean(category & EhUtils.ASIAN_PORN), false);
        mMisc.setChecked(!Utils.int2boolean(category & EhUtils.MISC), false);
    }

    /**
     * Get category according to button.
     * @return the category of this view
     */
    public int getCategory() {
        int category = 0;
        if (!mDoujinshi.isChecked()) category |= EhUtils.DOUJINSHI;
        if (!mManga.isChecked()) category |= EhUtils.MANGA;
        if (!mArtistCG.isChecked()) category |= EhUtils.ARTIST_CG;
        if (!mGameCG.isChecked()) category |= EhUtils.GAME_CG;
        if (!mWestern.isChecked()) category |= EhUtils.WESTERN;
        if (!mNonH.isChecked()) category |= EhUtils.NON_H;
        if (!mImageSets.isChecked()) category |= EhUtils.IMAGE_SET;
        if (!mCosplay.isChecked()) category |= EhUtils.COSPLAY;
        if (!mAsianPorn.isChecked()) category |= EhUtils.ASIAN_PORN;
        if (!mMisc.isChecked()) category |= EhUtils.MISC;
        return category;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putInt(STATE_KEY_CATEGORY, getCategory());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            setCategory(savedState.getInt(STATE_KEY_CATEGORY));
        }
    }
}
