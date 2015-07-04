package com.hippo.ehviewer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.conaco.BitmapHolder;
import com.hippo.conaco.Conaco;
import com.hippo.conaco.Unikery;
import com.hippo.ehviewer.R;
import com.hippo.widget.FixedAspectImageView;

public class LoadImageView extends FixedAspectImageView implements Unikery,
        View.OnClickListener, View.OnLongClickListener {

    private static final int STATE_NONE = 0;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADED = 2;
    private static final int STATE_FAILURE = 3;

    private static final int RETRY_DRAWABLE = R.drawable.retry_load_image;

    private int mTaskId = Unikery.INVAILD_ID;

    private int mState = STATE_NONE;

    private BitmapHolder mBitmapHolder;

    private Conaco mConaco;
    private String mKey;
    private String mUrl;

    private RetryType mRetryType = RetryType.NONE;

    private static final RetryType[] sRetryTypeArray = {
            RetryType.NONE,
            RetryType.CLICK,
            RetryType.LONG_CLICK
    };

    public enum RetryType {
        NONE      (0),
        CLICK      (1),
        LONG_CLICK   (2);

        RetryType(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;
    }

    public LoadImageView(Context context) {
        super(context);
    }

    public LoadImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LoadImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoadImageView);
        final int index = a.getInt(R.styleable.LoadImageView_retryType, -1);
        if (index >= 0) {
            setRetryType(sRetryTypeArray[index]);
        }
        a.recycle();
    }

    public void setRetryType(RetryType retryType) {
        if (mRetryType != retryType) {
            RetryType oldRetryType = mRetryType;
            mRetryType = retryType;

            if (mState == STATE_FAILURE) {
                if (oldRetryType == RetryType.CLICK) {
                    setOnClickListener(null);
                    setClickable(false);
                } else if (oldRetryType == RetryType.LONG_CLICK) {
                    setOnLongClickListener(null);
                    setLongClickable(false);
                }

                if (retryType == RetryType.CLICK) {
                    setOnClickListener(this);
                } else if (retryType == RetryType.LONG_CLICK) {
                    setOnLongClickListener(this);
                }

                if (oldRetryType != RetryType.NONE && retryType == RetryType.NONE) {
                    setImageDrawable(null);
                } else if (oldRetryType == RetryType.NONE && retryType != RetryType.NONE) {
                    setImageResource(RETRY_DRAWABLE);
                }
            }
        }
    }

    public void load(Conaco conaco, String key, String url) {
        mState = STATE_LOADING;

        mConaco = conaco;
        mKey = key;
        mUrl = url;
        conaco.load(this, key, url);
    }

    public void cancel() {
        mState = STATE_NONE;

        if (mConaco != null) {
            mConaco.cancel(this);

            // release
            mConaco = null;
            mKey = null;
            mUrl = null;
        }
    }

    @Override
    public void setBitmap(BitmapHolder bitmapHolder, Conaco.Source source) {
        mState = STATE_LOADED;

        // release
        mConaco = null;
        mKey = null;
        mUrl = null;

        BitmapHolder oldBitmapHolder = mBitmapHolder;

        mBitmapHolder = bitmapHolder;
        bitmapHolder.obtain();
        if (source != Conaco.Source.MEMORY) {
            Drawable[] layers = new Drawable[2];
            layers[0] = new ColorDrawable(Color.TRANSPARENT);
            layers[1] = new BitmapDrawable(getContext().getResources(), bitmapHolder.getBitmap());
            TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
            setImageDrawable(transitionDrawable);
            transitionDrawable.startTransition(300);
        } else {
            setImageBitmap(bitmapHolder.getBitmap());
        }

        if (oldBitmapHolder != null) {
            oldBitmapHolder.release();
        }
    }

    @Override
    public void setDrawable(Drawable drawable) {
        mState = STATE_NONE;

        setImageDrawable(drawable);
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
        mState = STATE_FAILURE;

        if (mRetryType == RetryType.CLICK) {
            setImageResource(RETRY_DRAWABLE);
            setOnClickListener(this);
        } else if (mRetryType == RetryType.LONG_CLICK) {
            setImageResource(RETRY_DRAWABLE);
            setOnLongClickListener(this);
        }
    }

    @Override
    public void onCancel() {
        // Empty, everything is done in cancel
    }

    @Override
    public void onClick(View v) {
        setOnClickListener(null);
        setClickable(false);

        load(mConaco, mKey, mUrl);
    }

    @Override
    public boolean onLongClick(View v) {
        setOnLongClickListener(null);
        setLongClickable(false);

        load(mConaco, mKey, mUrl);

        return true;
    }
}
