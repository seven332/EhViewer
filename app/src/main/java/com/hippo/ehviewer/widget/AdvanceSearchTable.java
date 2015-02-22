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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TableLayout;

import com.hippo.ehviewer.R;

public class AdvanceSearchTable extends TableLayout {

    public static final int SNAME = 0x1;
    public static final int STAGS = 0x2;
    public static final int SDESC = 0x4;
    public static final int STORR = 0x8;
    public static final int STO = 0x10;
    public static final int SDT1 = 0x20;
    public static final int SDT2 = 0x40;
    public static final int SH = 0x80;

    private CheckBox mSname;
    private CheckBox mStags;
    private CheckBox mSdesc;
    private CheckBox mStorr;
    private CheckBox mSto;
    private CheckBox mSdt1;
    private CheckBox mSdt2;
    private CheckBox mSh;
    private CheckBox mSr;
    private Spinner mMinRating;

    public AdvanceSearchTable(Context context) {
        super(context);
        init();
    }

    public AdvanceSearchTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.widget_advance_search_table, this);

        mSname = (CheckBox) findViewById(R.id.search_sname);
        mStags = (CheckBox) findViewById(R.id.search_stags);
        mSdesc = (CheckBox) findViewById(R.id.search_sdesc);
        mStorr = (CheckBox) findViewById(R.id.search_storr);
        mSto = (CheckBox) findViewById(R.id.search_sto);
        mSdt1 = (CheckBox) findViewById(R.id.search_sdt1);
        mSdt2 = (CheckBox) findViewById(R.id.search_sdt2);
        mSh = (CheckBox) findViewById(R.id.search_sh);
        mSr = (CheckBox) findViewById(R.id.search_sr);
        mMinRating = (Spinner) findViewById(R.id.search_min_rating);
    }

    public int getAdvanceSearch() {
        int advanceSearch = 0;
        if (mSname.isChecked()) advanceSearch |= SNAME;
        if (mStags.isChecked()) advanceSearch |= STAGS;
        if (mSdesc.isChecked()) advanceSearch |= SDESC;
        if (mStorr.isChecked()) advanceSearch |= STORR;
        if (mSto.isChecked()) advanceSearch |= STO;
        if (mSdt1.isChecked()) advanceSearch |= SDT1;
        if (mSdt2.isChecked()) advanceSearch |= SDT2;
        if (mSh.isChecked()) advanceSearch |= SH;
        return advanceSearch;
    }

    public int getMinRating() {
        if (mSr.isChecked()) {
            return mMinRating.getSelectedItemPosition() + 2;
        } else {
            return -1;
        }
    }

}
