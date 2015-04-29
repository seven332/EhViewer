package com.hippo.ehviewer.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.drawable.MaterialIndicatorDrawable;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.LockPatternUtils;
import com.hippo.ehviewer.widget.LockPatternView;

import java.util.List;

public class EnterPatternActivity extends AbsActivity {

    private static final String KEY_LAST_BAD_PATTERN_RETRY = "last_bad_pattern_retry";

    private static final int MAX_RETRY_TIMES = 5;

    private int mRetryTimes = 0;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_pattern);

        // Set random color
        int themeColor = Config.getCustomThemeColor() ? Config.getThemeColor() : Ui.THEME_COLOR;
        getActionBar().setBackgroundDrawable(new ColorDrawable(themeColor));
        Ui.colorStatusBarL(this, themeColor);

        // Menu
        MaterialIndicatorDrawable materialIndicator = new MaterialIndicatorDrawable(this, Color.WHITE, MaterialIndicatorDrawable.Stroke.THIN);
        materialIndicator.setIconState(MaterialIndicatorDrawable.IconState.ARROW);
        Ui.setMaterialIndicator(getActionBar(), materialIndicator);

        // Can't try if bad retry happened in 10 mins
        long lastBadRetry = Config.getLong(KEY_LAST_BAD_PATTERN_RETRY, Long.MAX_VALUE);
        if (System.currentTimeMillis() - lastBadRetry < 1000 * 60 * 10) {
            mRetryTimes = 10;
        }

        final TextView textView = (TextView) findViewById(R.id.text);
        LockPatternView patternView = (LockPatternView) findViewById(R.id.pattern_view);
        patternView.setOnPatternListener(new LockPatternView.OnPatternListener() {
            @Override
            public void onPatternStart() {

            }

            @Override
            public void onPatternCleared() {

            }

            @Override
            public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {

            }

            @Override
            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (mRetryTimes > MAX_RETRY_TIMES) {
                    textView.setText(R.string.no_more_retries);
                    Config.setLong(KEY_LAST_BAD_PATTERN_RETRY, System.currentTimeMillis());
                } else {
                    String enterPatter = LockPatternUtils.patternToString(pattern);
                    if (enterPatter.equals(Config.getPatternProtection())) {
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        textView.setText(R.string.error_pattern);
                    }
                }
                mRetryTimes++;
            }
        });
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
}
