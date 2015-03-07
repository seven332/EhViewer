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

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.hippo.ehviewer.util.ViewUtils;

public class SuggestionTextView extends PrefixEditText {

    private static final String TAG = SuggestionTextView.class.getSimpleName();

    private List<String> mAllSuggestions;
    private ArrayAdapter<String> mAdapte;

    private boolean mKeyboardIsShowing = true;

    public SuggestionTextView(Context context) {
        super(context);
    }

    public SuggestionTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SuggestionTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * We override this method to avoid an extra onItemClick being called on
     * the drop-down's OnItemClickListener by
     * {@link AutoCompleteTextView#onKeyUp(int, KeyEvent)} when an item is
     * clicked with the trackball.
     */
    @Override
    public void performCompletion() {
    }

    /**
     * We override this method so that we can allow a threshold of zero,
     * which ACTV does not.
     */
    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPopupShowing())
            showDropDown();

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // special case for the back key, we do not even try to send it
            // to the drop down list but instead, consume it immediately
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                KeyEvent.DispatcherState state = getKeyDispatcherState();
                if (state != null) {
                    state.startTracking(event, this);
                }
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                KeyEvent.DispatcherState state = getKeyDispatcherState();
                if (state != null) {
                    state.handleUpEvent(event);
                }
                if (event.isTracking() && !event.isCanceled()) {
                    InputMethodManager imm = (InputMethodManager)
                            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && mKeyboardIsShowing) {
                        imm.hideSoftInputFromWindow(getWindowToken(), 0);
                        mKeyboardIsShowing = false;
                    } else {
                        clearFocus();
                        mKeyboardIsShowing = true;
                    }
                    return true;
                }
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setSuggestionHelper(SuggestionHelper helper) {
        if (helper != null) {
            mAllSuggestions = helper.getQueries();
            mAdapte = new ArrayAdapter<String>(ViewUtils.sContextThemeWrapper,
                    android.R.layout.simple_dropdown_item_1line, mAllSuggestions);
            setAdapter(mAdapte);
        } else {
            mAllSuggestions = null;
            mAdapte = null;
            setAdapter(null);
        }
    }
}
