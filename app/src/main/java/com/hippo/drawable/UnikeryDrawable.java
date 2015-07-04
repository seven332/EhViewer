package com.hippo.drawable;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.hippo.conaco.BitmapHolder;
import com.hippo.conaco.Conaco;
import com.hippo.conaco.Unikery;
import com.hippo.ehviewer.R;

public class UnikeryDrawable extends WrapDrawable implements Unikery {

    private int mTaskId = Unikery.INVAILD_ID;

    private View mView;

    public UnikeryDrawable(View view) {
        mView = view;
    }

    @Override
    public void setBitmap(BitmapHolder bitmapHolder, Conaco.Source source) {
        // TODO release sometime ?
        bitmapHolder.obtain();
        setDrawable(new BitmapDrawable(mView.getResources(), bitmapHolder.getBitmap()));
    }

    @Override
    public void setDrawable(Drawable drawable) {
        super.setDrawable(drawable);
        updateBounds();
        mView.requestLayout();
    }

    @Override
    public void setTaskId(int id) {
        mTaskId = id;
    }

    @Override
    public int getTaskId() {
        return mTaskId;
    }

    @Override
    public void onFailure() {
        setDrawable(mView.getResources().getDrawable(R.drawable.ic_sad));
    }

    @Override
    public void onCancel() {
        // Empty
    }
}
