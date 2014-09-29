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
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.widget.ColorPickerView;

public class ColorPreference extends DialogPreference implements ColorPickerView.OnColorChangedListener, View.OnClickListener {

    private View mColorBrick;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setColor(int color) {
        if (mColorBrick != null)
            mColorBrick.setBackgroundColor(color);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        View ll = view.findViewById(android.R.id.widget_frame);
        if (ll != null && ll instanceof LinearLayout) {
            LinearLayout widgetFrame = (LinearLayout)ll;
            widgetFrame.setVisibility(View.VISIBLE);

            mColorBrick = new View(getContext());
            if (isEnabled())
                setColor(Config.getThemeColor());
            else
                setColor(getContext().getResources().getColor(R.color.disabled_mask));
            widgetFrame.addView(mColorBrick, new LinearLayout.LayoutParams(Ui.dp2pix(24), Ui.dp2pix(24)));
        }
    }

    @Override
    @SuppressLint("InflateParams")
    protected View onCreateDialogView() {
        LayoutInflater mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return mInflater.inflate(R.layout.color_scheme_dialog, null);
    }

    private void setUpPresets(View view, final int which) {
        final Button preset = (Button) view.findViewById(which);
        if (preset != null)
            preset.setOnClickListener(this);
    }

    @Override
    protected void onBindDialogView(View view) {
        int curColor = Config.getThemeColor();
        final ColorPickerView colorPicker = (ColorPickerView) view.findViewById(R.id.color_picker_view);
        ViewUtils.removeHardwareAccelerationSupport(colorPicker);
        Button oldColor = (Button) view.findViewById(R.id.color_scheme_dialog_old_color);
        oldColor.setOnClickListener(this);
        final Button newColor = (Button) view.findViewById(R.id.color_scheme_dialog_new_color);
        setUpPresets(view, R.id.color_scheme_dialog_preset_one);
        setUpPresets(view, R.id.color_scheme_dialog_preset_two);
        setUpPresets(view, R.id.color_scheme_dialog_preset_three);
        setUpPresets(view, R.id.color_scheme_dialog_preset_four);
        setUpPresets(view, R.id.color_scheme_dialog_preset_five);
        setUpPresets(view, R.id.color_scheme_dialog_preset_six);
        setUpPresets(view, R.id.color_scheme_dialog_preset_seven);
        setUpPresets(view, R.id.color_scheme_dialog_preset_eight);
        final EditText hexValue = (EditText) view.findViewById(R.id.color_scheme_dialog_hex_value);
        hexValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before,
                    final int count) {
                try {
                    int color = Color.parseColor("#"
                            + hexValue.getText().toString().toUpperCase(Locale.getDefault()));
                    colorPicker.setColor(color);
                    newColor.setBackgroundColor(color);
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

        colorPicker.setOnColorChangedListener(this);
        oldColor.setBackgroundColor(curColor);
        colorPicker.setColor(curColor, true);
    }

    @Override
    protected void onPrepareDialogBuilder(MaterialAlertDialog.Builder builder) {
        // Empty
    }

    @Override
    public boolean onClick(MaterialAlertDialog dialog, int which) {
        super.onClick(dialog, which);
        if (which == MaterialAlertDialog.POSITIVE) {
            ColorPickerView colorPicker = (ColorPickerView) dialog.findViewById(R.id.color_picker_view);
            if (colorPicker == null)
                return true;
            int color = colorPicker.getColor() | 0xff000000;
            if (callChangeListener(color)) {
                Config.setThemeColor(color);
                setColor(color);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
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

    @Override
    public void onColorChanged(View view, int color) {
        try {
            View p = (View) view.getParent();
            EditText hexValue = (EditText) p.findViewById(R.id.color_scheme_dialog_hex_value);
            Button newColor = (Button) view.findViewById(R.id.color_scheme_dialog_new_color);

            if (hexValue != null)
                hexValue.setText(padLeft(Integer.toHexString(color).toUpperCase(Locale.getDefault()),
                        '0', 8));
            if (newColor != null)
                newColor.setBackgroundColor(color);
        } catch (Throwable e) {
        }
    }

    private int getColor(final int color) {
        return getContext().getResources().getColor(color);
    }

    @Override
    public void onClick(View v) {

        View view = ViewUtils.getAncestor(v, R.id.main);
        if (view == null)
            return;
        ColorPickerView colorPicker = (ColorPickerView) view.findViewById(R.id.color_picker_view);
        if (colorPicker == null)
            return;

        switch (v.getId()) {
        case R.id.color_scheme_dialog_preset_one:
            colorPicker.setColor(getColor(R.color.theme_red));
            break;
        case R.id.color_scheme_dialog_preset_two:
            colorPicker.setColor(getColor(R.color.theme_orange));
            break;
        case R.id.color_scheme_dialog_preset_three:
            colorPicker.setColor(getColor(R.color.theme_lime));
            break;
        case R.id.color_scheme_dialog_preset_four:
            colorPicker.setColor(getColor(R.color.theme_green));
            break;
        case R.id.color_scheme_dialog_preset_five:
            colorPicker.setColor(getColor(R.color.theme_teal));
            break;
        case R.id.color_scheme_dialog_preset_six:
            colorPicker.setColor(getColor(R.color.theme_blue));
            break;
        case R.id.color_scheme_dialog_preset_seven:
            colorPicker.setColor(getColor(R.color.theme_purple));
            break;
        case R.id.color_scheme_dialog_preset_eight:
            colorPicker.setColor(getColor(R.color.theme_brown));
            break;
        case R.id.color_scheme_dialog_old_color:
            colorPicker.setColor(Config.getThemeColor());
            break;
        default:
            break;
        }

        onColorChanged(colorPicker, colorPicker.getColor());
    }

    @Override
    protected void onDialogCreate(Dialog dialog) {
        dialog.getWindow().setFormat(PixelFormat.RGBA_8888);
    }
}
