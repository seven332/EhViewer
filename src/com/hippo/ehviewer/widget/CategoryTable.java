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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TableLayout;

import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.R;

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
        inflater.inflate(R.layout.category_table, this);

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

    private final boolean int2boolean(int integer) {
        return integer == 0 ? false : true;
    }

    /**
     * Set each button checked or not according to category.
     *
     * @param category
     */
    public void setCategory(int category) {

        System.out.println("category = " + category);

        mDoujinshi.setChecked(!int2boolean(category & ListUrls.DOUJINSHI));
        mManga.setChecked(!int2boolean(category & ListUrls.MANGA));
        mArtistCG.setChecked(!int2boolean(category & ListUrls.ARTIST_CG));
        mGameCG.setChecked(!int2boolean(category & ListUrls.GAME_CG));
        mWestern.setChecked(!int2boolean(category & ListUrls.WESTERN));
        mNonH.setChecked(!int2boolean(category & ListUrls.NON_H));
        mImageSets.setChecked(!int2boolean(category & ListUrls.IMAGE_SET));
        mCosplay.setChecked(!int2boolean(category & ListUrls.COSPLAY));
        mAsianPorn.setChecked(!int2boolean(category & ListUrls.ASIAN_PORN));
        mMisc.setChecked(!int2boolean(category & ListUrls.MISC));
    }

    /**
     * Get category according to button.
     * @return
     */
    public int getCategory() {
        int category = 0;
        if (!mDoujinshi.isChecked()) category |= ListUrls.DOUJINSHI;
        if (!mManga.isChecked()) category |= ListUrls.MANGA;
        if (!mArtistCG.isChecked()) category |= ListUrls.ARTIST_CG;
        if (!mGameCG.isChecked()) category |= ListUrls.GAME_CG;
        if (!mWestern.isChecked()) category |= ListUrls.WESTERN;
        if (!mNonH.isChecked()) category |= ListUrls.NON_H;
        if (!mImageSets.isChecked()) category |= ListUrls.IMAGE_SET;
        if (!mCosplay.isChecked()) category |= ListUrls.COSPLAY;
        if (!mAsianPorn.isChecked()) category |= ListUrls.ASIAN_PORN;
        if (!mMisc.isChecked()) category |= ListUrls.MISC;
        return category;
    }
}
