package com.hippo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ListView;

import com.hippo.ehviewer.R;

public class IndicatingListView extends ListView {

    private int mIndicatorHeight;

    private Paint mPaint = new Paint();
    private Rect mTemp = new Rect();

    public IndicatingListView(Context context) {
        super(context);
        init(context);
    }

    public IndicatingListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public IndicatingListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mIndicatorHeight = context.getResources().getDimensionPixelOffset(R.dimen.scroll_view_indicator_height);
        mPaint.setColor(context.getResources().getColor(R.color.scroll_view_indicator));
        mPaint.setStyle(Paint.Style.FILL);
    }

    private void fillTopIndicatorDrawRect() {
        mTemp.set(0, 0, getWidth(), mIndicatorHeight);
    }

    private void fillBottomIndicatorDrawRect() {
        mTemp.set(0, getHeight() - mIndicatorHeight, getWidth(), getHeight());
    }

    private boolean needShowTopIndicator() {
        return canScrollVertically(-1);
    }

    private boolean needShowBottomIndicator() {
        return canScrollVertically(1);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        final int restoreCount = canvas.save();
        canvas.translate(getScrollX(), getScrollY());

        // Draw top indicator
        if (needShowTopIndicator()) {
            fillTopIndicatorDrawRect();
            canvas.drawRect(mTemp, mPaint);
        }
        // Draw bottom indicator
        if (needShowBottomIndicator()) {
            fillBottomIndicatorDrawRect();
            canvas.drawRect(mTemp, mPaint);
        }

        canvas.restoreToCount(restoreCount);
    }
}
