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

package com.hippo.ehviewer.ui.scene.preference;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.util.Settings;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.AppbarScene;
import com.hippo.widget.SensitiveCheckBox;
import com.hippo.widget.recyclerview.EasyRecyclerView;

public class ExcludedLanguagesScene extends AppbarScene implements View.OnClickListener{

    private static final int ROW_COUNT = 17;
    private static final int[] LANGUAGE_STR_IDS = {
            R.string.language_japanese,
            R.string.language_english,
            R.string.language_chinese,
            R.string.language_dutch,
            R.string.language_french,
            R.string.language_german,
            R.string.language_hungarian,
            R.string.language_italian,
            R.string.language_korean,
            R.string.language_polish,
            R.string.language_portuguese,
            R.string.language_russian,
            R.string.language_spanish,
            R.string.language_thai,
            R.string.language_vietnamese,
            R.string.language_na,
            R.string.language_other
    };

    private static final String[] LANGUAGES = {
            EhConfig.JAPANESE_TRANSLATED,
            EhConfig.JAPANESE_REWRITE,
            EhConfig.ENGLISH_ORIGINAL,
            EhConfig.ENGLISH_TRANSLATED,
            EhConfig.ENGLISH_REWRITE,
            EhConfig.CHINESE_ORIGINAL,
            EhConfig.CHINESE_TRANSLATED,
            EhConfig.CHINESE_REWRITE,
            EhConfig.DUTCH_ORIGINAL,
            EhConfig.DUTCH_TRANSLATED,
            EhConfig.DUTCH_REWRITE,
            EhConfig.FRENCH_ORIGINAL,
            EhConfig.FRENCH_TRANSLATED,
            EhConfig.FRENCH_REWRITE,
            EhConfig.GERMAN_ORIGINAL,
            EhConfig.GERMAN_TRANSLATED,
            EhConfig.GERMAN_REWRITE,
            EhConfig.HUNGARIAN_ORIGINAL,
            EhConfig.HUNGARIAN_TRANSLATED,
            EhConfig.HUNGARIAN_REWRITE,
            EhConfig.ITALIAN_ORIGINAL,
            EhConfig.ITALIAN_TRANSLATED,
            EhConfig.ITALIAN_REWRITE,
            EhConfig.KOREAN_ORIGINAL,
            EhConfig.KOREAN_TRANSLATED,
            EhConfig.KOREAN_REWRITE,
            EhConfig.POLISH_ORIGINAL,
            EhConfig.POLISH_TRANSLATED,
            EhConfig.POLISH_REWRITE,
            EhConfig.PORTUGUESE_ORIGINAL,
            EhConfig.PORTUGUESE_TRANSLATED,
            EhConfig.PORTUGUESE_REWRITE,
            EhConfig.RUSSIAN_ORIGINAL,
            EhConfig.RUSSIAN_TRANSLATED,
            EhConfig.RUSSIAN_REWRITE,
            EhConfig.SPANISH_ORIGINAL,
            EhConfig.SPANISH_TRANSLATED,
            EhConfig.SPANISH_REWRITE,
            EhConfig.THAI_ORIGINAL,
            EhConfig.THAI_TRANSLATED,
            EhConfig.THAI_REWRITE,
            EhConfig.VIETNAMESE_ORIGINAL,
            EhConfig.VIETNAMESE_TRANSLATED,
            EhConfig.VIETNAMESE_REWRITE,
            EhConfig.NA_ORIGINAL,
            EhConfig.NA_TRANSLATED,
            EhConfig.NA_REWRITE,
            EhConfig.OTHER_ORIGINAL,
            EhConfig.OTHER_TRANSLATED,
            EhConfig.OTHER_REWRITE};

    private boolean[][] mSeletions = new boolean[ROW_COUNT][3];

    private View mCancel;
    private View mOk;
    private View mSelectAll;
    private View mDeselectAll;
    private View mInvertSelection;
    private EasyRecyclerView mRecyclerView;

    private LanguageAdapter mAdapter;

    private int mOriginalRecyclerViewPaddingBottom;

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setContentView(R.layout.scene_excluded_languages);
        setTitle(R.string.excluded_languages);
        setIcon(R.drawable.ic_arrow_left_dark);


        mCancel = findViewById(R.id.cancel);
        mOk = findViewById(R.id.ok);
        mSelectAll = findViewById(R.id.select_all);
        mDeselectAll = findViewById(R.id.deselect_all);
        mInvertSelection = findViewById(R.id.invert_selection);
        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setClipToPadding(false);
        mAdapter = new LanguageAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getStageActivity()));

        mOriginalRecyclerViewPaddingBottom = mRecyclerView.getPaddingBottom();

        mCancel.setOnClickListener(this);
        mOk.setOnClickListener(this);
        mSelectAll.setOnClickListener(this);
        mDeselectAll.setOnClickListener(this);
        mInvertSelection.setOnClickListener(this);

        RippleSalon.addRipple(mCancel, false);
        RippleSalon.addRipple(mOk, false);
        RippleSalon.addRipple(mSelectAll, false);
        RippleSalon.addRipple(mDeselectAll, false);
        RippleSalon.addRipple(mInvertSelection, false);
    }

    @Override
    protected void onBind() {
        super.onBind();

        String excludedLanguages = Settings.getExcludedLanguages();
        String[] languages = excludedLanguages.split("x");

        int iLength = languages.length;
        int jLength = LANGUAGES.length;
        for (int i = 0, j = 0; i < iLength; i++) {
            String language = languages[i];
            if (!isDecimal(language)) {
                continue;
            }

            for (; j < jLength; j++) {
                String pattern = LANGUAGES[j];
                if (pattern.equals(language)) {
                    // Get it
                    int row = (j + 1) / 3;
                    int column = (j + 1) % 3;
                    mSeletions[row][column] = true;
                    break;
                }
            }
        }
    }

    private boolean isDecimal(String str) {
        int length = str.length();

        // "" is not decimal
        if (length <= 0) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onIconClick() {
        finish();
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        EasyRecyclerView recyclerView = mRecyclerView;
        recyclerView.setPadding(recyclerView.getPaddingLeft(),
                recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                mOriginalRecyclerViewPaddingBottom + b);
    }

    @Override
    public void onClick(View v) {
        if (v == mCancel) {
            finish();
        } else if (v == mOk) {
            StringBuilder sb = new StringBuilder();
            int i = -1;
            boolean first = true;
            for (boolean[] selections : mSeletions) {
                for (boolean b : selections) {
                    i++;
                    if (i == 0) {
                        continue;
                    }
                    if (b) {
                        if (!first) {
                            sb.append("x");
                        } else {
                            first = false;
                        }
                        sb.append(LANGUAGES[i - 1]);
                    }
                }
            }

            String excludedLanguages = sb.toString();
            Settings.putExcludedLanguages(excludedLanguages);
            EhApplication.getEhHttpClient(getStageActivity()).setExcludedLanguages(excludedLanguages);
            finish();
        } else if (v == mSelectAll) {
            for (boolean[] selections : mSeletions) {
                int length = selections.length;
                for (int i = 0; i < length; i++) {
                    selections[i] = true;
                }
            }
            mAdapter.notifyDataSetChanged();
        } else if (v == mDeselectAll) {
            for (boolean[] selections : mSeletions) {
                int length = selections.length;
                for (int i = 0; i < length; i++) {
                    selections[i] = false;
                }
            }
            mAdapter.notifyDataSetChanged();
        } else if (v == mInvertSelection) {
            for (boolean[] selections : mSeletions) {
                int length = selections.length;
                for (int i = 0; i < length; i++) {
                    selections[i] = !selections[i];
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    class LanguageHolder extends RecyclerView.ViewHolder implements SensitiveCheckBox.OnCheckedChangeListener {

        public TextView language;
        public SensitiveCheckBox original;
        public SensitiveCheckBox translated;
        public SensitiveCheckBox rewrite;

        public LanguageHolder(View itemView) {
            super(itemView);

            ViewGroup viewGroup = (ViewGroup) itemView;
            language = (TextView) viewGroup.getChildAt(0);
            original = (SensitiveCheckBox) viewGroup.getChildAt(1);
            translated = (SensitiveCheckBox) viewGroup.getChildAt(2);
            rewrite = (SensitiveCheckBox) viewGroup.getChildAt(3);

            original.setOnCheckedChangeListener(this);
            translated.setOnCheckedChangeListener(this);
            rewrite.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(SensitiveCheckBox view, boolean isChecked, boolean fromUser) {
            if (fromUser) {
                int row = getAdapterPosition();
                int column;
                if (view == original) {
                    column = 0;
                } else if (view == translated) {
                    column = 1;
                } else {
                    column = 2;
                }
                mSeletions[row][column] = !mSeletions[row][column];
            }
        }
    }

    class LanguageAdapter extends RecyclerView.Adapter<LanguageHolder> {

        @Override
        public LanguageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new LanguageHolder(getStageActivity().getLayoutInflater()
                    .inflate(R.layout.item_language, parent, false));
        }

        @Override
        public void onBindViewHolder(LanguageHolder holder, int position) {
            holder.language.setText(LANGUAGE_STR_IDS[position]);
            if (position == 0) {
                holder.original.setVisibility(View.INVISIBLE);
            } else {
                holder.original.setVisibility(View.VISIBLE);
            }
            boolean[] selections = mSeletions[position];
            holder.original.setChecked(selections[0]);
            holder.translated.setChecked(selections[1]);
            holder.rewrite.setChecked(selections[2]);
        }

        @Override
        public int getItemCount() {
            return ROW_COUNT;
        }
    }
}
