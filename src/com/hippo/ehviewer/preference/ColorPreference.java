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

package com.hippo.ehviewer.preference;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.ColorPickerView;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.SuperToast;

public class ColorPreference extends Preference implements Preference.OnPreferenceClickListener {

    private final Context mContext;
    private View mColorBrick;

    public ColorPreference(Context context) {
        this(context, null);
    }

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnPreferenceClickListener(this);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setOnPreferenceClickListener(this);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        View ll = view.findViewById(android.R.id.widget_frame);
        if (ll != null && ll instanceof LinearLayout) {
            LinearLayout widgetFrame = (LinearLayout)ll;
            widgetFrame.setVisibility(View.VISIBLE);

            mColorBrick = new View(mContext);
            mColorBrick.setBackgroundColor(Config.getThemeColor());
            widgetFrame.addView(mColorBrick, new LinearLayout.LayoutParams(Ui.dp2pix(24), Ui.dp2pix(24)));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        new ColorSchemeDialogBuilder(mContext).show();
        return true;
    }

    public void setColor(int color) {
        if (mColorBrick != null)
            mColorBrick.setBackgroundColor(color);
    }

    public class ColorSchemeDialogBuilder extends DialogBuilder implements
            ColorPickerView.OnColorChangedListener {

        private final int mCurrentColor;

        private final ColorPickerView.OnColorChangedListener mListener = this;;

        private LayoutInflater mInflater;

        private ColorPickerView mColorPicker;

        private Button mOldColor;

        private Button mNewColor;

        private View mRootView;

        private EditText mHexValue;

        /**
         * Constructor of <code>ColorSchemeDialog</code>
         *
         * @param context The {@link Contxt} to use.
         */
        public ColorSchemeDialogBuilder(final Context context) {
            super(context);
            mCurrentColor = Config.getThemeColor();
            setUp(context, mCurrentColor);
        }

        /*
         * (non-Javadoc)
         * @see com.andrew.apollo.widgets.ColorPickerView.OnColorChangedListener#
         * onColorChanged(int)
         */
        @Override
        public void onColorChanged(final int color) {
            if (mHexValue != null) {
                mHexValue.setText(padLeft(Integer.toHexString(color).toUpperCase(Locale.getDefault()),
                        '0', 8));
            }
            mNewColor.setBackgroundColor(color);
        }

        private String padLeft(final String string, final char padChar, final int size) {
            if (string.length() >= size) {
                return string;
            }
            final StringBuilder result = new StringBuilder();
            for (int i = string.length(); i < size; i++) {
                result.append(padChar);
            }
            result.append(string);
            return result.toString();
        }

        /**
         * Initialzes the presets and color picker
         *
         * @param color The color to use.
         */
        @SuppressLint("InflateParams")
        private void setUp(final Context context, final int color) {
            mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mRootView = mInflater.inflate(R.layout.color_scheme_dialog, null);

            mColorPicker = (ColorPickerView)mRootView.findViewById(R.id.color_picker_view);
            mOldColor = (Button)mRootView.findViewById(R.id.color_scheme_dialog_old_color);
            mOldColor.setOnClickListener(mPresetListener);
            mNewColor = (Button)mRootView.findViewById(R.id.color_scheme_dialog_new_color);
            setUpPresets(R.id.color_scheme_dialog_preset_one);
            setUpPresets(R.id.color_scheme_dialog_preset_two);
            setUpPresets(R.id.color_scheme_dialog_preset_three);
            setUpPresets(R.id.color_scheme_dialog_preset_four);
            setUpPresets(R.id.color_scheme_dialog_preset_five);
            setUpPresets(R.id.color_scheme_dialog_preset_six);
            setUpPresets(R.id.color_scheme_dialog_preset_seven);
            setUpPresets(R.id.color_scheme_dialog_preset_eight);
            mHexValue = (EditText)mRootView.findViewById(R.id.color_scheme_dialog_hex_value);
            mHexValue.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(final CharSequence s, final int start, final int before,
                        final int count) {
                    try {
                        mColorPicker.setColor(Color.parseColor("#"
                                + mHexValue.getText().toString().toUpperCase(Locale.getDefault())));
                        mNewColor.setBackgroundColor(Color.parseColor("#"
                                + mHexValue.getText().toString().toUpperCase(Locale.getDefault())));
                    } catch (final Exception ignored) {
                    }
                }

                @Override
                public void beforeTextChanged(final CharSequence s, final int start, final int count,
                        final int after) {
                    /* Nothing to do */
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    /* Nothing to do */
                }
            });

            mColorPicker.setOnColorChangedListener(this);
            mOldColor.setBackgroundColor(color);
            mColorPicker.setColor(color, true);

            setTitle(R.string.color_picker_title);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            setView(mRootView, lp, false);
            setSimpleNegativeButton();
            setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((AlertButton)v).dialog.dismiss();
                    new SuperToast(context, R.string.restart_to_take_effect).show();

                    int color = getColor() | 0xff000000;
                    Config.setThemeColor(color);
                    setColor(color);
                }
            });
        }

        /**
         * @param color The color resource.
         * @return A new color from Apollo's resources.
         */
        private int getColor(final int color) {
            return getContext().getResources().getColor(color);
        }

        /**
         * @return {@link ColorPickerView}'s current color
         */
        public int getColor() {
            return mColorPicker.getColor();
        }

        /**
         * @param which The Id of the preset color
         */
        private void setUpPresets(final int which) {
            final Button preset = (Button)mRootView.findViewById(which);
            if (preset != null) {
                preset.setOnClickListener(mPresetListener);
            }
        }

        /**
         * Sets up the preset buttons
         */
        private final View.OnClickListener mPresetListener = new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                switch (v.getId()) {
                    case R.id.color_scheme_dialog_preset_one:
                        mColorPicker.setColor(getColor(android.R.color.holo_blue_light));
                        break;
                    case R.id.color_scheme_dialog_preset_two:
                        mColorPicker.setColor(getColor(android.R.color.holo_green_light));
                        break;
                    case R.id.color_scheme_dialog_preset_three:
                        mColorPicker.setColor(getColor(android.R.color.holo_orange_dark));
                        break;
                    case R.id.color_scheme_dialog_preset_four:
                        mColorPicker.setColor(getColor(android.R.color.holo_orange_light));
                        break;
                    case R.id.color_scheme_dialog_preset_five:
                        mColorPicker.setColor(getColor(android.R.color.holo_purple));
                        break;
                    case R.id.color_scheme_dialog_preset_six:
                        mColorPicker.setColor(getColor(android.R.color.holo_red_light));
                        break;
                    case R.id.color_scheme_dialog_preset_seven:
                        mColorPicker.setColor(getColor(android.R.color.darker_gray));
                        break;
                    case R.id.color_scheme_dialog_preset_eight:
                        mColorPicker.setColor(getColor(android.R.color.black));
                        break;
                    case R.id.color_scheme_dialog_old_color:
                        mColorPicker.setColor(mCurrentColor);
                        break;
                    default:
                        break;
                }
                if (mListener != null) {
                    mListener.onColorChanged(getColor());
                }
            }
        };

        @Override
        public AlertDialog create() {
            AlertDialog dialog = super.create();
            dialog.getWindow().setFormat(PixelFormat.RGBA_8888);
            Util.removeHardwareAccelerationSupport(mColorPicker);
            return dialog;
        }
    }
}
