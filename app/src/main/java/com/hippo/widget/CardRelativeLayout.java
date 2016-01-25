/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.hippo.cardsalon.CardHelper;
import com.hippo.cardsalon.CardViewDelegate;

public class CardRelativeLayout extends RelativeLayout implements CardViewDelegate {

    private CardHelper mCardHelper;

    public CardRelativeLayout(Context context) {
        super(context);
        init(context, null);
    }

    public CardRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CardRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mCardHelper = new CardHelper(this);
        mCardHelper.initialize(context, attrs);
    }

    @Override
    public void setBackgroundResource(int resid) {
        // Empty
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        // Empty
    }

    @Override
    public void setCardPadding(int left, int top, int right, int bottom) {
        if (mCardHelper != null) {
            mCardHelper.setPadding(left, top, right, bottom);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setRawBackgroundDrawable(Drawable drawable) {
        super.setBackgroundDrawable(drawable);
    }

    @Override
    public void setCardRadius(float radius) {
        mCardHelper.setCardRadius(radius);
    }

    @Override
    public float getCardRadius() {
        return mCardHelper.getCardRadius();
    }

    @Override
    public void setCardBackgroundColor(ColorStateList color) {
        mCardHelper.setCardBackgroundColor(color);
    }

    @Override
    public ColorStateList getCardBackgroundColor() {
        return mCardHelper.getCardBackgroundColor();
    }

    @Override
    public void setCardBoundSize(float size) {
        mCardHelper.setCardBoundSize(size);
    }

    @Override
    public float getCardBoundSize() {
        return mCardHelper.getCardBoundSize();
    }

    @Override
    public void setCardBoundColor(ColorStateList color) {
        mCardHelper.setCardBoundColor(color);
    }

    @Override
    public ColorStateList getCardBoundColor() {
        return mCardHelper.getCardBoundColor();
    }

    @Override
    public void setCardElevation(float elevation) {
        mCardHelper.setCardElevation(elevation);
    }

    @Override
    public float getCardElevation() {
        return mCardHelper.getCardElevation();
    }
}
