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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TableLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.util.Utils;
import com.hippo.widget.CheckTextView;

public class CategoryTable extends TableLayout {

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

        setStretchAllColumns(true);

        mDoujinshi = (CheckTextView)findViewById(R.id.button_doujinshi);
        mManga = (CheckTextView)findViewById(R.id.button_manga);
        mArtistCG = (CheckTextView)findViewById(R.id.button_artistcg);
        mGameCG = (CheckTextView)findViewById(R.id.button_gamecg);
        mWestern = (CheckTextView)findViewById(R.id.button_western);
        mNonH = (CheckTextView)findViewById(R.id.button_non_h);
        mImageSets = (CheckTextView)findViewById(R.id.button_imageset);
        mCosplay = (CheckTextView)findViewById(R.id.button_cosplay);
        mAsianPorn = (CheckTextView)findViewById(R.id.button_asianporn);
        mMisc = (CheckTextView)findViewById(R.id.button_misc);
    }

    /**
     * Set each button checked or not according to category.
     *
     * @param category
     */
    public void setCategory(int category) {
        mDoujinshi.setChecked(!Utils.int2boolean(category & EhUtils.DOUJINSHI));
        mManga.setChecked(!Utils.int2boolean(category & EhUtils.MANGA));
        mArtistCG.setChecked(!Utils.int2boolean(category & EhUtils.ARTIST_CG));
        mGameCG.setChecked(!Utils.int2boolean(category & EhUtils.GAME_CG));
        mWestern.setChecked(!Utils.int2boolean(category & EhUtils.WESTERN));
        mNonH.setChecked(!Utils.int2boolean(category & EhUtils.NON_H));
        mImageSets.setChecked(!Utils.int2boolean(category & EhUtils.IMAGE_SET));
        mCosplay.setChecked(!Utils.int2boolean(category & EhUtils.COSPLAY));
        mAsianPorn.setChecked(!Utils.int2boolean(category & EhUtils.ASIAN_PORN));
        mMisc.setChecked(!Utils.int2boolean(category & EhUtils.MISC));
    }

    /**
     * Get category according to button.
     * @return
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
}
