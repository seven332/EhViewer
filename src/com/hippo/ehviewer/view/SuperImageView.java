package com.hippo.ehviewer.view;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews.RemoteView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import com.hippo.ehviewer.R;

// TODO use native to improve gif

/**
 * Displays an arbitrary image, such as an icon. The ImageView class can load
 * images from various sources (such as resources or content providers), takes
 * care of computing its measurement from the image so that it can be used in
 * any layout manager, and provides various display options such as scaling and
 * tinting.
 * 
 * @attr ref android.R.styleable#ImageView_adjustViewBounds
 * @attr ref android.R.styleable#ImageView_src
 * @attr ref android.R.styleable#ImageView_maxWidth
 * @attr ref android.R.styleable#ImageView_maxHeight
 * @attr ref android.R.styleable#ImageView_tint
 * @attr ref android.R.styleable#ImageView_scaleType
 * @attr ref android.R.styleable#ImageView_cropToPadding
 */
@RemoteView
public class SuperImageView extends View {
    private static String TAG = "SuperImageView";
    // for gif
    private Movie mMovie = null;
    // Not 0 mean the same timer
    private long mMovieStart = 1;
    private int mCurrentAnimationTime = 0;

    private static final int DEFAULT_MOVIEW_DURATION = 1000;

    // settable by the client
    private Uri mUri;
    private int mResource = 0;
    private Matrix mMatrix;
    private ScaleType mScaleType;
    private boolean mHaveFrame = false;
    private boolean mAdjustViewBounds = false;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;

    // these are applied to the drawable
    private ColorFilter mColorFilter;
    private Xfermode mXfermode;
    private int mAlpha = 255;
    private int mViewAlphaScale = 256;
    private boolean mColorMod = false;

    private Drawable mDrawable = null;
    private int[] mState = null;
    private boolean mMergeState = false;
    private int mLevel = 0;
    private int mDrawableWidth;
    private int mDrawableHeight;
    private Matrix mDrawMatrix = null;

    // Avoid allocations...
    private RectF mTempSrc = new RectF();
    private RectF mTempDst = new RectF();

    private boolean mCropToPadding;

    private int mBaseline = -1;
    private boolean mBaselineAlignBottom = false;

    // AdjustViewBounds behavior will be in compatibility mode for older apps.
    private boolean mAdjustViewBoundsCompat = false;

    private static final ScaleType[] sScaleTypeArray = { ScaleType.MATRIX,
            ScaleType.FIT_XY, ScaleType.FIT_START, ScaleType.FIT_CENTER,
            ScaleType.FIT_END, ScaleType.CENTER, ScaleType.CENTER_CROP,
            ScaleType.CENTER_INSIDE };

    public SuperImageView(Context context) {
        super(context);
        initImageView();
    }

    public SuperImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SuperImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initImageView();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SuperImageView, defStyle, 0);

        mBaselineAlignBottom = a.getBoolean(
                R.styleable.SuperImageView_baselineAlignBottom, false);

        mBaseline = a.getDimensionPixelSize(
                R.styleable.SuperImageView_baseline, -1);

        setAdjustViewBounds(a.getBoolean(
                R.styleable.SuperImageView_adjustViewBounds, false));

        setMaxWidth(a.getDimensionPixelSize(
                R.styleable.SuperImageView_maxWidth, Integer.MAX_VALUE));

        setMaxHeight(a.getDimensionPixelSize(
                R.styleable.SuperImageView_maxHeight, Integer.MAX_VALUE));

        int index = a.getInt(R.styleable.SuperImageView_scaleType, -1);
        if (index >= 0) {
            setScaleType(sScaleTypeArray[index]);
        }

        int tint = a.getInt(R.styleable.SuperImageView_tint, 0);
        if (tint != 0) {
            setColorFilter(tint);
        }

        int alpha = a.getInt(R.styleable.SuperImageView_drawableAlpha, 255);
        if (alpha != 255) {
            setAlpha(alpha);
        }

        mCropToPadding = a.getBoolean(R.styleable.SuperImageView_cropToPadding,
                false);

        int resId = a.getResourceId(R.styleable.SuperImageView_src, -1);
        if (resId == -1) {
            Drawable d = a.getDrawable(R.styleable.SuperImageView_src);
            if (d != null) {
                setImageDrawable(d);
            }
        } else
            this.setImageResource(resId);

        a.recycle();

        // need inflate syntax/reader for matrix
    }

    private void initImageView() {
        mMatrix = new Matrix();
        mScaleType = ScaleType.FIT_CENTER;
        mAdjustViewBoundsCompat = getContext().getApplicationInfo().targetSdkVersion <= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    @Override
    protected boolean verifyDrawable(Drawable dr) {
        return mDrawable == dr || super.verifyDrawable(dr);
    }

    protected boolean verifyMovie(Movie mv) {
        return mMovie == mv;
    }

    @SuppressLint("NewApi")
    @Override
    public void jumpDrawablesToCurrentState() {
        if (Build.VERSION.SDK_INT < 11) {
            return;
        }

        super.jumpDrawablesToCurrentState();
        if (mDrawable != null)
            mDrawable.jumpToCurrentState();
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (dr == mDrawable) {
            /*
             * we invalidate the whole view in this case because it's very hard
             * to know where the drawable actually is. This is made complicated
             * because of the offsets and transformations that can be applied.
             * In theory we could get the drawable's bounds and run them through
             * the transformation and offsets, but this is probably not worth
             * the effort.
             */
            invalidate();
        } else {
            super.invalidateDrawable(dr);
        }
    }

    public void invalidateMovie(Movie mv) {
        if (mv == mMovie) {
            /*
             * we invalidate the whole view in this case because it's very hard
             * to know where the drawable actually is. This is made complicated
             * because of the offsets and transformations that can be applied.
             * In theory we could get the drawable's bounds and run them through
             * the transformation and offsets, but this is probably not worth
             * the effort.
             */
            invalidate();
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return (getBackground() != null && getBackground().getCurrent() != null);
    }

    @SuppressLint("NewApi")
    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }

        super.onPopulateAccessibilityEvent(event);
        CharSequence contentDescription = getContentDescription();
        if (!TextUtils.isEmpty(contentDescription)) {
            event.getText().add(contentDescription);
        }
    }

    /**
     * True when ImageView is adjusting its bounds to preserve the aspect ratio
     * of its drawable
     * 
     * @return whether to adjust the bounds of this view to presrve the original
     *         aspect ratio of the drawable
     * 
     * @see #setAdjustViewBounds(boolean)
     * 
     * @attr ref android.R.styleable#ImageView_adjustViewBounds
     */
    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    /**
     * Set this to true if you want the ImageView to adjust its bounds to
     * preserve the aspect ratio of its drawable.
     * 
     * <p>
     * <strong>Note:</strong> If the application targets API level 17 or lower,
     * adjustViewBounds will allow the drawable to shrink the view bounds, but
     * not grow to fill available measured space in all cases. This is for
     * compatibility with legacy {@link android.view.View.MeasureSpec
     * MeasureSpec} and {@link android.widget.RelativeLayout RelativeLayout}
     * behavior.
     * </p>
     * 
     * @param adjustViewBounds
     *            Whether to adjust the bounds of this view to preserve the
     *            original aspect ratio of the drawable.
     * 
     * @see #getAdjustViewBounds()
     * 
     * @attr ref android.R.styleable#ImageView_adjustViewBounds
     */
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        mAdjustViewBounds = adjustViewBounds;
        if (adjustViewBounds) {
            setScaleType(ScaleType.FIT_CENTER);
        }
    }

    /**
     * The maximum width of this view.
     * 
     * @return The maximum width of this view
     * 
     * @see #setMaxWidth(int)
     * 
     * @attr ref android.R.styleable#ImageView_maxWidth
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    /**
     * An optional argument to supply a maximum width for this view. Only valid
     * if {@link #setAdjustViewBounds(boolean)} has been set to true. To set an
     * image to be a maximum of 100 x 100 while preserving the original aspect
     * ratio, do the following: 1) set adjustViewBounds to true 2) set maxWidth
     * and maxHeight to 100 3) set the height and width layout params to
     * WRAP_CONTENT.
     * 
     * <p>
     * Note that this view could be still smaller than 100 x 100 using this
     * approach if the original image is small. To set an image to a fixed size,
     * specify that size in the layout params and then use
     * {@link #setScaleType(android.widget.ImageView.ScaleType)} to determine
     * how to fit the image within the bounds.
     * </p>
     * 
     * @param maxWidth
     *            maximum width for this view
     * 
     * @see #getMaxWidth()
     * 
     * @attr ref android.R.styleable#ImageView_maxWidth
     */
    public void setMaxWidth(int maxWidth) {
        mMaxWidth = maxWidth;
    }

    /**
     * The maximum height of this view.
     * 
     * @return The maximum height of this view
     * 
     * @see #setMaxHeight(int)
     * 
     * @attr ref android.R.styleable#ImageView_maxHeight
     */
    public int getMaxHeight() {
        return mMaxHeight;
    }

    /**
     * An optional argument to supply a maximum height for this view. Only valid
     * if {@link #setAdjustViewBounds(boolean)} has been set to true. To set an
     * image to be a maximum of 100 x 100 while preserving the original aspect
     * ratio, do the following: 1) set adjustViewBounds to true 2) set maxWidth
     * and maxHeight to 100 3) set the height and width layout params to
     * WRAP_CONTENT.
     * 
     * <p>
     * Note that this view could be still smaller than 100 x 100 using this
     * approach if the original image is small. To set an image to a fixed size,
     * specify that size in the layout params and then use
     * {@link #setScaleType(android.widget.ImageView.ScaleType)} to determine
     * how to fit the image within the bounds.
     * </p>
     * 
     * @param maxHeight
     *            maximum height for this view
     * 
     * @see #getMaxHeight()
     * 
     * @attr ref android.R.styleable#ImageView_maxHeight
     */
    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
    }

    /**
     * Return the view's drawable, or null if no drawable has been assigned.
     */
    public Drawable getDrawable() {
        return mDrawable;
    }
    
    public int getResourceWidth() {
        return mDrawableWidth;
    }
    
    public int getResourceHeight() {
        return mDrawableHeight;
    }
    
    public Movie getMovie() {
        return mMovie;
    }
    
    public boolean isEmpty() {
        if (mMovie == null && mDrawable == null)
            return true;
        else
            return false;
    }
    
    /**
     * Sets a drawable as the content of this ImageView.
     * 
     * <p class="note">
     * This does Bitmap reading and decoding on the UI thread, which can cause a
     * latency hiccup. If that's a concern, consider using
     * {@link #setImageDrawable(android.graphics.drawable.Drawable)} or
     * {@link #setImageBitmap(android.graphics.Bitmap)} and
     * {@link android.graphics.BitmapFactory} instead.
     * </p>
     * 
     * @param resId
     *            the resource identifier of the drawable
     * 
     * @attr ref android.R.styleable#ImageView_src
     */
    public void setImageResource(int resId) {
        if (mUri != null || mResource != resId) {
            updateDrawable(null);
            updateMovie(null);
            mResource = resId;
            mUri = null;

            final int oldWidth = mDrawableWidth;
            final int oldHeight = mDrawableHeight;

            resolveUri();

            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout();
            }
            invalidate();
        }
    }

    /**
     * Sets the content of this ImageView to the specified Uri.
     * 
     * <p class="note">
     * This does Bitmap reading and decoding on the UI thread, which can cause a
     * latency hiccup. If that's a concern, consider using
     * {@link #setImageDrawable(android.graphics.drawable.Drawable)} or
     * {@link #setImageBitmap(android.graphics.Bitmap)} and
     * {@link android.graphics.BitmapFactory} instead.
     * </p>
     * 
     * @param uri
     *            The Uri of an image
     */
    public void setImageURI(Uri uri) {
        if (mResource != 0
                || (mUri != uri && (uri == null || mUri == null || !uri
                        .equals(mUri)))) {
            updateDrawable(null);
            updateMovie(null);
            mResource = 0;
            mUri = uri;

            final int oldWidth = mDrawableWidth;
            final int oldHeight = mDrawableHeight;

            resolveUri();

            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout();
            }
            invalidate();
        }
    }

    /**
     * Sets a drawable as the content of this ImageView.
     * 
     * @param drawable
     *            The drawable to set
     */
    public void setImageDrawable(Drawable drawable) {
        if (mDrawable != drawable) {
            mResource = 0;
            mUri = null;
            mMovie = null;

            final int oldWidth = mDrawableWidth;
            final int oldHeight = mDrawableHeight;

            updateDrawable(drawable);
            updateMovie(null);

            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout();
            }
            invalidate();
        }
    }

    /**
     * Sets a Movie as the content of this SuperImageView.
     * 
     * @param mv
     *            The movie to set
     */
    public void setImageMovie(Movie mv) {
        if (mMovie != mv) {
            mResource = 0;
            mUri = null;
            mDrawable = null;

            final int oldWidth = mDrawableWidth;
            final int oldHeight = mDrawableHeight;

            updateDrawable(null);
            updateMovie(mv);

            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout();
            }
            invalidate();
        }
    }

    /**
     * Sets a InputStream as the content of this SuperImageView.
     * 
     * @param is
     *            The InputStream to set
     */
    public void setImageInputStream(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            is.close();
            baos.flush();

            byte[] bytes = baos.toByteArray();
            Movie m = Movie.decodeByteArray(bytes, 0, bytes.length);
            if (m == null) {
                Bitmap b = BitmapFactory
                        .decodeByteArray(bytes, 0, bytes.length);
                setImageBitmap(b);
            } else
                setImageMovie(m);

            baos.close();
        } catch (IOException e) {
            Log.e(TAG, "Set Image from InputStream error !");
        }
    }

    /**
     * Sets a Bitmap as the content of this ImageView.
     * 
     * @param bm
     *            The bitmap to set
     */
    public void setImageBitmap(Bitmap bm) {
        // if this is used frequently, may handle bitmaps explicitly
        // to reduce the intermediate drawable object
        setImageDrawable(new BitmapDrawable(getContext().getResources(), bm));
    }

    public void setImageState(int[] state, boolean merge) {
        mState = state;
        mMergeState = merge;
        if (mDrawable != null) {
            refreshDrawableState();
            resizeFromResource();
        }
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        resizeFromResource();
    }

    /**
     * Sets the image level, when it is constructed from a
     * {@link android.graphics.drawable.LevelListDrawable}.
     * 
     * @param level
     *            The new level for the image.
     */
    public void setImageLevel(int level) {
        mLevel = level;
        if (mDrawable != null) {
            mDrawable.setLevel(level);
            resizeFromResource();
        }
    }

    /**
     * Options for scaling the bounds of an image to the bounds of this view.
     */
    public enum ScaleType {
        /**
         * Scale using the image matrix when drawing. The image matrix can be
         * set using {@link SuperImageView#setImageMatrix(Matrix)}. From XML,
         * use this syntax: <code>android:scaleType="matrix"</code>.
         */
        MATRIX(0),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#FILL}. From XML, use
         * this syntax: <code>android:scaleType="fitXY"</code>.
         */
        FIT_XY(1),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#START}. From XML, use
         * this syntax: <code>android:scaleType="fitStart"</code>.
         */
        FIT_START(2),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#CENTER}. From XML, use
         * this syntax: <code>android:scaleType="fitCenter"</code>.
         */
        FIT_CENTER(3),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#END}. From XML, use
         * this syntax: <code>android:scaleType="fitEnd"</code>.
         */
        FIT_END(4),
        /**
         * Center the image in the view, but perform no scaling. From XML, use
         * this syntax: <code>android:scaleType="center"</code>.
         */
        CENTER(5),
        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so that
         * both dimensions (width and height) of the image will be equal to or
         * larger than the corresponding dimension of the view (minus padding).
         * The image is then centered in the view. From XML, use this syntax:
         * <code>android:scaleType="centerCrop"</code>.
         */
        CENTER_CROP(6),
        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so that
         * both dimensions (width and height) of the image will be equal to or
         * less than the corresponding dimension of the view (minus padding).
         * The image is then centered in the view. From XML, use this syntax:
         * <code>android:scaleType="centerInside"</code>.
         */
        CENTER_INSIDE(7);

        ScaleType(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;
    }

    /**
     * Controls how the image should be resized or moved to match the size of
     * this ImageView.
     * 
     * @param scaleType
     *            The desired scaling mode.
     * 
     * @attr ref android.R.styleable#ImageView_scaleType
     */
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == null) {
            throw new NullPointerException();
        }

        if (mScaleType != scaleType) {
            mScaleType = scaleType;

            setWillNotCacheDrawing(mScaleType == ScaleType.CENTER);

            requestLayout();
            invalidate();
        }
    }

    /**
     * Return the current scale type in use by this ImageView.
     * 
     * @see SuperImageView.ScaleType
     * 
     * @attr ref android.R.styleable#ImageView_scaleType
     */
    public ScaleType getScaleType() {
        return mScaleType;
    }

    /**
     * Return the view's optional matrix. This is applied to the view's drawable
     * when it is drawn. If there is not matrix, this method will return an
     * identity matrix. Do not change this matrix in place but make a copy. If
     * you want a different matrix applied to the drawable, be sure to call
     * setImageMatrix().
     */
    public Matrix getImageMatrix() {
        if (mDrawMatrix == null) {
            // return new Matrix(Matrix.IDENTITY_MATRIX); //TODO Fix here
            return new Matrix();
        }
        return mDrawMatrix;
    }

    public void setImageMatrix(Matrix matrix) {
        // collaps null and identity to just null
        if (matrix != null && matrix.isIdentity()) {
            matrix = null;
        }

        // don't invalidate unless we're actually changing our matrix
        if (matrix == null && !mMatrix.isIdentity() || matrix != null
                && !mMatrix.equals(matrix)) {
            mMatrix.set(matrix);
            configureBounds();
            invalidate();
        }
    }

    /**
     * Return whether this ImageView crops to padding.
     * 
     * @return whether this ImageView crops to padding
     * 
     * @see #setCropToPadding(boolean)
     * 
     * @attr ref android.R.styleable#ImageView_cropToPadding
     */
    public boolean getCropToPadding() {
        return mCropToPadding;
    }

    /**
     * Sets whether this ImageView will crop to padding.
     * 
     * @param cropToPadding
     *            whether this ImageView will crop to padding
     * 
     * @see #getCropToPadding()
     * 
     * @attr ref android.R.styleable#ImageView_cropToPadding
     */
    public void setCropToPadding(boolean cropToPadding) {
        if (mCropToPadding != cropToPadding) {
            mCropToPadding = cropToPadding;
            requestLayout();
            invalidate();
        }
    }

    private void resolveUri() {
        if (mDrawable != null && mMovie != null) {
            return;
        }

        Resources rsrc = getResources();
        if (rsrc == null) {
            return;
        }

        Drawable d = null;
        Movie m = null;

        if (mResource != 0) {
            try {
                InputStream is = rsrc.openRawResource(mResource);
                m = Movie.decodeStream(is);
                if (m == null) {
                    d = rsrc.getDrawable(mResource);
                }
            } catch (Exception e) {
                Log.w("ImageView", "Unable to find resource: " + mResource, e);
                // Don't try again.
                mUri = null;
            }
        } else if (mUri != null) {
            String scheme = mUri.getScheme();
            /*
             * if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)) { //
             * TODO Can't fix here try { // Load drawable through Resources, to
             * get the source density information
             * ContentResolver.OpenResourceIdResult r =
             * getContext().getContentResolver().getResourceId(mUri); d =
             * r.r.getDrawable(r.id); } catch (Exception e) { Log.w("ImageView",
             * "Unable to open content: " + mUri, e); } } else
             */if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                    || ContentResolver.SCHEME_FILE.equals(scheme)) {
                InputStream stream = null;
                try {
                    stream = getContext().getContentResolver().openInputStream(
                            mUri);
                    m = Movie.decodeStream(stream);
                    if (m == null) {
                        stream = getContext().getContentResolver()
                                .openInputStream(mUri);
                        d = Drawable.createFromStream(stream, null);
                    }
                } catch (Exception e) {
                    Log.w("ImageView", "Unable to open content: " + mUri, e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            Log.w("ImageView", "Unable to close content: "
                                    + mUri, e);
                        }
                    }
                }
            } else {
                d = Drawable.createFromPath(mUri.toString());
            }
            if (d == null && m == null) {
                System.out.println("resolveUri failed on bad bitmap uri: "
                        + mUri);
                // Don't try again.
                mUri = null;
            }
        } else {
            return;
        }

        updateDrawable(d);
        updateMovie(m);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (mState == null) {
            return super.onCreateDrawableState(extraSpace);
        } else if (!mMergeState) {
            return mState;
        } else {
            return mergeDrawableStates(
                    super.onCreateDrawableState(extraSpace + mState.length),
                    mState);
        }
    }

    private void updateDrawable(Drawable d) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
            unscheduleDrawable(mDrawable);
        }
        mDrawable = d;
        if (d != null) {
            d.setCallback(this);
            if (d.isStateful()) {
                d.setState(getDrawableState());
            }
            d.setLevel(mLevel);
            // d.setLayoutDirection(getLayoutDirection()); // TODO Can't fix, reflection?
            // here
            d.setVisible(getVisibility() == VISIBLE, true);
            mDrawableWidth = d.getIntrinsicWidth();
            mDrawableHeight = d.getIntrinsicHeight();
            applyColorMod();
            configureBounds();
        } else {
            mDrawableWidth = mDrawableHeight = -1;
        }
    }

    private void updateMovie(Movie m) {
        mMovie = m;
        if (m != null) {
            mMovieStart = 0;
            mCurrentAnimationTime = 0;
            mDrawableWidth = m.width();
            mDrawableHeight = m.height();
            applyColorMod();
            configureBounds();
        }
    }

    private void resizeFromResource() {
        Drawable d = mDrawable;
        Movie m = mMovie;
        if (d != null) {
            int w = d.getIntrinsicWidth();
            if (w < 0)
                w = mDrawableWidth;
            int h = d.getIntrinsicHeight();
            if (h < 0)
                h = mDrawableHeight;
            if (w != mDrawableWidth || h != mDrawableHeight) {
                mDrawableWidth = w;
                mDrawableHeight = h;
                requestLayout();
            }
        } else if (m != null) {
            int w = m.width();
            if (w < 0)
                w = mDrawableWidth;
            int h = m.height();
            if (h < 0)
                h = mDrawableHeight;
            if (w != mDrawableWidth || h != mDrawableHeight) {
                mDrawableWidth = w;
                mDrawableHeight = h;
                requestLayout();
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (Build.VERSION.SDK_INT < 17) {
            return;
        }

        super.onRtlPropertiesChanged(layoutDirection);

        if (mDrawable != null) {
            // mDrawable.setLayoutDirection(layoutDirection); // TODO Can't fix, reflection?
            // here
        }
    }

    private static final Matrix.ScaleToFit[] sS2FArray = {
            Matrix.ScaleToFit.FILL, Matrix.ScaleToFit.START,
            Matrix.ScaleToFit.CENTER, Matrix.ScaleToFit.END };

    private static Matrix.ScaleToFit scaleTypeToScaleToFit(ScaleType st) {
        // ScaleToFit enum to their corresponding Matrix.ScaleToFit values
        return sS2FArray[st.nativeInt - 1];
    }

    @SuppressLint("NewApi")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        resolveUri();
        int w;
        int h;

        // Desired aspect ratio of the view's contents (not including padding)
        float desiredAspect = 0.0f;

        // We are allowed to change the view's width
        boolean resizeWidth = false;

        // We are allowed to change the view's height
        boolean resizeHeight = false;

        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        if (mDrawable == null && mMovie == null) {
            // If no drawable, its intrinsic size is 0.
            mDrawableWidth = -1;
            mDrawableHeight = -1;
            w = h = 0;
        } else {
            w = mDrawableWidth;
            h = mDrawableHeight;
            if (w <= 0)
                w = 1;
            if (h <= 0)
                h = 1;

            // We are supposed to adjust view bounds to match the aspect
            // ratio of our drawable. See if that is possible.
            if (mAdjustViewBounds) {
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;

                desiredAspect = (float) w / (float) h;
            }
        }

        int pleft = getPaddingLeft();
        int pright = getPaddingRight();
        int ptop = getPaddingTop();
        int pbottom = getPaddingBottom();

        int widthSize;
        int heightSize;

        if (resizeWidth || resizeHeight) {
            /*
             * If we get here, it means we want to resize to match the drawables
             * aspect ratio, and we have the freedom to change at least one
             * dimension.
             */

            // Get the max possible width given our constraints
            widthSize = resolveAdjustedSize(w + pleft + pright, mMaxWidth,
                    widthMeasureSpec);

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h + ptop + pbottom, mMaxHeight,
                    heightMeasureSpec);

            if (desiredAspect != 0.0f) {
                // See what our actual aspect ratio is
                float actualAspect = (float) (widthSize - pleft - pright)
                        / (heightSize - ptop - pbottom);

                if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {

                    boolean done = false;

                    // Try adjusting width to be proportional to height
                    if (resizeWidth) {
                        int newWidth = (int) (desiredAspect * (heightSize
                                - ptop - pbottom))
                                + pleft + pright;

                        // Allow the width to outgrow its original estimate if
                        // height is fixed.
                        if (!resizeHeight && !mAdjustViewBoundsCompat) {
                            widthSize = resolveAdjustedSize(newWidth,
                                    mMaxWidth, widthMeasureSpec);
                        }

                        if (newWidth <= widthSize) {
                            widthSize = newWidth;
                            done = true;
                        }
                    }

                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        int newHeight = (int) ((widthSize - pleft - pright) / desiredAspect)
                                + ptop + pbottom;

                        // Allow the height to outgrow its original estimate if
                        // width is fixed.
                        if (!resizeWidth && !mAdjustViewBoundsCompat) {
                            heightSize = resolveAdjustedSize(newHeight,
                                    mMaxHeight, heightMeasureSpec);
                        }

                        if (newHeight <= heightSize) {
                            heightSize = newHeight;
                        }
                    }
                }
            }
        } else {
            /*
             * We are either don't want to preserve the drawables aspect ratio,
             * or we are not allowed to change view dimensions. Just measure in
             * the normal way.
             */
            w += pleft + pright;
            h += ptop + pbottom;

            w = Math.max(w, getSuggestedMinimumWidth());
            h = Math.max(h, getSuggestedMinimumHeight());

            if (Build.VERSION.SDK_INT < 11) {
                widthSize = resolveSize(w, widthMeasureSpec);
                heightSize = resolveSize(h, heightMeasureSpec);
            } else {
                widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
                heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    private int resolveAdjustedSize(int desiredSize, int maxSize,
            int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
            /*
             * Parent says we can be as big as we want. Just don't be larger
             * than max size imposed on ourselves.
             */
            result = Math.min(desiredSize, maxSize);
            break;
        case MeasureSpec.AT_MOST:
            // Parent says we can be as big as we want, up to specSize.
            // Don't be larger than specSize, and don't be larger than
            // the max size imposed on ourselves.
            result = Math.min(Math.min(desiredSize, specSize), maxSize);
            break;
        case MeasureSpec.EXACTLY:
            // No choice. Do what we are told.
            result = specSize;
            break;
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        configureBounds();
    }

    private void configureBounds() {
        if (mDrawable == null && mMovie == null) {
            return;
        }

        int dwidth = mDrawableWidth;
        int dheight = mDrawableHeight;

        int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

        boolean fits = (dwidth < 0 || vwidth == dwidth)
                && (dheight < 0 || vheight == dheight);

        if (dwidth <= 0 || dheight <= 0 || ScaleType.FIT_XY == mScaleType) {
            /*
             * If the drawable has no intrinsic size, or we're told to
             * scaletofit, then we just fill our entire view.
             */
            if (mDrawable != null) {
                mDrawable.setBounds(0, 0, vwidth, vheight);
                mDrawMatrix = null;
            } else if (dheight > 0 && dheight > 0) {
                mDrawMatrix = new Matrix();
                mDrawMatrix.setScale((float) vwidth / dwidth, (float) vheight
                        / dheight);
            }

        } else {
            // We need to do the scaling ourself, so have the drawable
            // use its native size.
            if (mDrawable != null)
                mDrawable.setBounds(0, 0, dwidth, dheight);

            if (ScaleType.MATRIX == mScaleType) {
                // Use the specified matrix as-is.
                if (mMatrix.isIdentity()) {
                    mDrawMatrix = null;
                } else {
                    mDrawMatrix = mMatrix;
                }
            } else if (fits) {
                // The bitmap fits exactly, no transform needed.
                mDrawMatrix = null;
            } else if (ScaleType.CENTER == mScaleType) {
                // Center bitmap in view, no scaling.
                mDrawMatrix = mMatrix;
                mDrawMatrix.setTranslate(
                        (int) ((vwidth - dwidth) * 0.5f + 0.5f),
                        (int) ((vheight - dheight) * 0.5f + 0.5f));
            } else if (ScaleType.CENTER_CROP == mScaleType) {
                mDrawMatrix = mMatrix;

                float scale;
                float dx = 0, dy = 0;

                if (dwidth * vheight > vwidth * dheight) {
                    scale = (float) vheight / (float) dheight;
                    dx = (vwidth - dwidth * scale) * 0.5f;
                } else {
                    scale = (float) vwidth / (float) dwidth;
                    dy = (vheight - dheight * scale) * 0.5f;
                }

                mDrawMatrix.setScale(scale, scale);
                mDrawMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            } else if (ScaleType.CENTER_INSIDE == mScaleType) {
                mDrawMatrix = mMatrix;
                float scale;
                float dx;
                float dy;

                if (dwidth <= vwidth && dheight <= vheight) {
                    scale = 1.0f;
                } else {
                    scale = Math.min((float) vwidth / (float) dwidth,
                            (float) vheight / (float) dheight);
                }

                dx = (int) ((vwidth - dwidth * scale) * 0.5f + 0.5f);
                dy = (int) ((vheight - dheight * scale) * 0.5f + 0.5f);

                mDrawMatrix.setScale(scale, scale);
                mDrawMatrix.postTranslate(dx, dy);
            } else {
                // Generate the required transform.
                mTempSrc.set(0, 0, dwidth, dheight);
                mTempDst.set(0, 0, vwidth, vheight);

                mDrawMatrix = mMatrix;
                mDrawMatrix.setRectToRect(mTempSrc, mTempDst,
                        scaleTypeToScaleToFit(mScaleType));
            }
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable d = mDrawable;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mDrawable == null && mMovie == null) {
            return; // couldn't resolve the URI
        }

        if (mDrawableWidth == 0 || mDrawableHeight == 0) {
            return; // nothing to draw (empty bounds)
        }

        int pLeft = getPaddingLeft();

        if (mDrawMatrix == null && getPaddingTop() == 0 && pLeft == 0) {
            if (mDrawable != null)
                mDrawable.draw(canvas);
            else
                drawMovieFrame(canvas);
        } else {
            int saveCount = canvas.getSaveCount();
            canvas.save();

            if (mCropToPadding) {
                final int scrollX = getScrollX();
                final int scrollY = getScrollY();
                canvas.clipRect(scrollX + pLeft, scrollY + getPaddingTop(),
                        scrollX + getRight() - getLeft() - getPaddingRight(), scrollY
                                + getBottom() - getTop() - getPaddingBottom());
            }

            canvas.translate(pLeft, getPaddingTop());

            if (mDrawMatrix != null) {
                canvas.concat(mDrawMatrix);
            }
            if (mDrawable != null)
                mDrawable.draw(canvas);
            else
                drawMovieFrame(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    @SuppressLint("NewApi")
    private void drawMovieFrame(Canvas canvas) {
        // if (!mPaused)
        updateAnimationTime();

        mMovie.setTime(mCurrentAnimationTime);
        mMovie.draw(canvas, 0, 0);

        // if (!mPaused)
        if (Build.VERSION.SDK_INT < 16) {
            invalidate();
        } else {
            postInvalidateOnAnimation();
        }
    }

    private void updateAnimationTime() {
        long now = android.os.SystemClock.uptimeMillis();

        if (mMovieStart == 0) {
            mMovieStart = now;
        }

        int dur = mMovie.duration();

        if (dur == 0) {
            dur = DEFAULT_MOVIEW_DURATION;
        }

        mCurrentAnimationTime = (int) ((now - mMovieStart) % dur);
    }

    /**
     * <p>
     * Return the offset of the widget's text baseline from the widget's top
     * boundary.
     * </p>
     * 
     * @return the offset of the baseline within the widget's bounds or -1 if
     *         baseline alignment is not supported.
     */
    @Override
    @ViewDebug.ExportedProperty(category = "layout")
    public int getBaseline() {
        if (mBaselineAlignBottom) {
            return getMeasuredHeight();
        } else {
            return mBaseline;
        }
    }

    /**
     * <p>
     * Set the offset of the widget's text baseline from the widget's top
     * boundary. This value is overridden by the
     * {@link #setBaselineAlignBottom(boolean)} property.
     * </p>
     * 
     * @param baseline
     *            The baseline to use, or -1 if none is to be provided.
     * 
     * @see #setBaseline(int)
     * @attr ref android.R.styleable#ImageView_baseline
     */
    public void setBaseline(int baseline) {
        if (mBaseline != baseline) {
            mBaseline = baseline;
            requestLayout();
        }
    }

    /**
     * Set whether to set the baseline of this view to the bottom of the view.
     * Setting this value overrides any calls to setBaseline.
     * 
     * @param aligned
     *            If true, the image view will be baseline aligned with based on
     *            its bottom edge.
     * 
     * @attr ref android.R.styleable#ImageView_baselineAlignBottom
     */
    public void setBaselineAlignBottom(boolean aligned) {
        if (mBaselineAlignBottom != aligned) {
            mBaselineAlignBottom = aligned;
            requestLayout();
        }
    }

    /**
     * Return whether this view's baseline will be considered the bottom of the
     * view.
     * 
     * @see #setBaselineAlignBottom(boolean)
     */
    public boolean getBaselineAlignBottom() {
        return mBaselineAlignBottom;
    }

    /**
     * Set a tinting option for the image.
     * 
     * @param color
     *            Color tint to apply.
     * @param mode
     *            How to apply the color. The standard mode is
     *            {@link PorterDuff.Mode#SRC_ATOP}
     * 
     * @attr ref android.R.styleable#ImageView_tint
     */
    public final void setColorFilter(int color, PorterDuff.Mode mode) {
        setColorFilter(new PorterDuffColorFilter(color, mode));
    }

    /**
     * Set a tinting option for the image. Assumes
     * {@link PorterDuff.Mode#SRC_ATOP} blending mode.
     * 
     * @param color
     *            Color tint to apply.
     * @attr ref android.R.styleable#ImageView_tint
     */
    public final void setColorFilter(int color) {
        setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public final void clearColorFilter() {
        setColorFilter(null);
    }

    /**
     * @hide Candidate for future API inclusion
     */
    public final void setXfermode(Xfermode mode) {
        if (mXfermode != mode) {
            mXfermode = mode;
            mColorMod = true;
            applyColorMod();
            invalidate();
        }
    }

    /**
     * Returns the active color filter for this ImageView.
     * 
     * @return the active color filter for this ImageView
     * 
     * @see #setColorFilter(android.graphics.ColorFilter)
     */
    public ColorFilter getColorFilter() {
        return mColorFilter;
    }

    /**
     * Apply an arbitrary colorfilter to the image.
     * 
     * @param cf
     *            the colorfilter to apply (may be null)
     * 
     * @see #getColorFilter()
     */
    public void setColorFilter(ColorFilter cf) {
        if (mColorFilter != cf) {
            mColorFilter = cf;
            mColorMod = true;
            applyColorMod();
            invalidate();
        }
    }

    /**
     * Returns the alpha that will be applied to the drawable of this ImageView.
     * 
     * @return the alpha that will be applied to the drawable of this ImageView
     * 
     * @see #setImageAlpha(int)
     */
    public int getImageAlpha() {
        return mAlpha;
    }

    /**
     * Sets the alpha value that should be applied to the image.
     * 
     * @param alpha
     *            the alpha value that should be applied to the image
     * 
     * @see #getImageAlpha()
     */
    public void setImageAlpha(int alpha) {
        setAlpha(alpha);
    }

    /**
     * Sets the alpha value that should be applied to the image.
     * 
     * @param alpha
     *            the alpha value that should be applied to the image
     * 
     * @deprecated use #setImageAlpha(int) instead
     */
    @Deprecated
    public void setAlpha(int alpha) {
        alpha &= 0xFF; // keep it legal
        if (mAlpha != alpha) {
            mAlpha = alpha;
            mColorMod = true;
            applyColorMod();
            invalidate();
        }
    }

    private void applyColorMod() {
        // Only mutate and apply when modifications have occurred. This should
        // not reset the mColorMod flag, since these filters need to be
        // re-applied if the Drawable is changed.
        if (mDrawable != null && mColorMod) {
            mDrawable = mDrawable.mutate();
            mDrawable.setColorFilter(mColorFilter);
            // mDrawable.setXfermode(mXfermode); // Can't fix here
            mDrawable.setAlpha(mAlpha * mViewAlphaScale >> 8);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mDrawable != null) {
            mDrawable.setVisible(visibility == VISIBLE, false);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mDrawable != null) {
            mDrawable.setVisible(getVisibility() == VISIBLE, false);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mDrawable != null) {
            mDrawable.setVisible(false, false);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }

        super.onInitializeAccessibilityEvent(event);
        event.setClassName(SuperImageView.class.getName());
    }

    @SuppressLint("NewApi")
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(SuperImageView.class.getName());
    }
}