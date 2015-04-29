package com.hippo.ehviewer.ui;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.LockPatternUtils;
import com.hippo.ehviewer.widget.LockPatternView;

public class PatternProtectionActivity extends AbsActivity implements View.OnClickListener{

    private TextView mTextView;
    private LockPatternView mPatternView;
    private View mCancel;
    private View mOK;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern_protection);

        // Set random color
        int themeColor = Config.getCustomThemeColor() ? Config.getThemeColor() : Ui.THEME_COLOR;
        getActionBar().setBackgroundDrawable(new ColorDrawable(themeColor));
        Ui.colorStatusBarL(this, themeColor);

        // Menu
        MaterialIndicatorDrawable materialIndicator = new MaterialIndicatorDrawable(this, Color.WHITE, MaterialIndicatorDrawable.Stroke.THIN);
        materialIndicator.setIconState(MaterialIndicatorDrawable.IconState.ARROW);
        Ui.setMaterialIndicator(getActionBar(), materialIndicator);

        mTextView = (TextView) findViewById(R.id.text);
        mPatternView = (LockPatternView) findViewById(R.id.pattern_view);
        mCancel = findViewById(R.id.cancel);
        mOK = findViewById(R.id.ok);

        mOK.setOnClickListener(this);
        mCancel.setOnClickListener(this);

        String pattern = Config.getPatternProtection();
        boolean setPattern = LockPatternUtils.isPatternVaild(pattern);
        Resources resources = getResources();
        int patternProtectionStateId = setPattern ?
                R.string.pattern_protection_set : R.string.pattern_protection_not_set;
        mTextView.setText(resources.getString(patternProtectionStateId) + "\n\n" +
                getString(R.string.pattern_protection_warning));

        if (setPattern) {
            mPatternView.setPattern(LockPatternView.DisplayMode.Correct, LockPatternUtils.stringToPattern(pattern));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == mOK) {
            Config.setPatternProtection(mPatternView.getPatternString());
            finish();
        } else if (v == mCancel) {
            finish();
        }
    }
}
