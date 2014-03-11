package com.hippo.ehviewer.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


// TODO point
// TODO when screen is locked and load image, image may not show
public class MangaImage extends OlImageView {
    
    private static ArrayList<WeakReference<MangaImage>> mangaImageList = new ArrayList<WeakReference<MangaImage>>();
    private static ArrayList<WeakReference<MangaImage>> delList = new ArrayList<WeakReference<MangaImage>>();
    
    private static final String TAG = "MangaImage";

    public static enum State {
        NONE, ONE_DOWN, DRAG, ZOOM, POINT
    };
    
    public static enum Action {
        ZOOM_IN, ZOOM_OUT
    };
    
    private static final Mode[] sModeArray = {
        Mode.CENTER, Mode.FIT_WIDTH, Mode.FIT_HEIGHT, Mode.FIT, Mode.SHARED
    };
    
    public enum Mode {
        CENTER(0),
        FIT_WIDTH(1),
        FIT_HEIGHT(2),
        FIT(3),
        SHARED(4);
        
        Mode(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;
    };
    
    private State state;
    private static Mode mMode = Mode.FIT;

    private static Matrix sharedMatrix = new Matrix();
    private Matrix matrix = new Matrix();
    private Matrix preMatrix = new Matrix();
    private float[] matrixValues = new float[9];
    private PointF firstPoint = new PointF();
    private PointF midPoint = new PointF();

    private int aWidth;
    private int aHeight;
    private int viewWidth;
    private int viewHeight;

    public MangaImage(Context context) {
        super(context);
        cleanList();
        mangaImageList.add(new WeakReference<MangaImage>(this));
        setListener();
    }

    public MangaImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MangaImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        cleanList();
        mangaImageList.add(new WeakReference<MangaImage>(this));
        setListener();
    }
    
    public static void setMode(int mode) {
        mMode = sModeArray[mode];
        for (WeakReference<MangaImage> weakMangaImage : mangaImageList) {
            MangaImage mangaImage = null;
            if ((mangaImage = weakMangaImage.get()) != null) {
                mangaImage.resetMatrix();
                mangaImage.setImageMatrix();
            }
        }
    }
    
    private static void cleanList() {
        for (WeakReference<MangaImage> weakMangaImage : mangaImageList) {
            if (weakMangaImage.get() == null)
                delList.add(weakMangaImage);
        }
        for (WeakReference<MangaImage> weakMangaImage : delList) {
            mangaImageList.remove(weakMangaImage);
        }
        delList.clear();
    }
    
    private void setListener() {
        setOnLoadListener(new OnLoadListener() {
            @Override
            public void onLoadCompleted(boolean ok) {
                if (!ok)
                    return;
                viewWidth = getWidth();
                viewHeight = getHeight();
                setScaleType(ScaleType.MATRIX);
                resetMatrix();
                matrix.getValues(matrixValues);
                aWidth = (int) (getResourceWidth() * matrixValues[Matrix.MSCALE_X]);
                aHeight = (int) (getResourceHeight() * matrixValues[Matrix.MSCALE_Y]);
                setImageMatrix();

                state = State.NONE;
                setOnTouchListener(new MangaImageListener());
            }
        });
    }
    
    public void setImageMatrix() {
        setImageMatrix(matrix);
    }
    
    private void resetMatrix() {
        matrix.reset();
        float scale;
        int actWidth = getResourceWidth();
        int actHeight = getResourceHeight();
        switch (mMode) {
        case CENTER:
            matrix.preTranslate((viewWidth - actWidth) / 2f, (viewHeight - actHeight) / 2f);
            break;
        case FIT_WIDTH:
            scale = (float)viewWidth/actWidth;
            matrix.preScale(scale, scale);
            matrix.preTranslate((viewWidth - actWidth*scale) / scale / 2f, (viewHeight - actHeight*scale) / scale / 2f);
            break;
        case FIT_HEIGHT:
            scale = (float)viewHeight/actHeight;
            matrix.preScale(scale, scale);
            matrix.preTranslate((viewWidth - actWidth*scale) / scale / 2f, (viewHeight - actHeight*scale) / scale / 2f);
            break;
        case FIT:
            float scaleX = (float)viewWidth/actWidth;
            float scaleY = (float)viewHeight/actHeight;
            scale = scaleX < scaleY ? scaleX : scaleY;
            matrix.preScale(scale, scale);
            matrix.preTranslate((viewWidth - actWidth*scale) / scale / 2f, (viewHeight - actHeight*scale) / scale / 2f);
            break;
        case SHARED:
            break;
        }
    }
    
    public boolean canScroll(int direction) {

        matrix.getValues(matrixValues);
        int xOffset = (int) matrixValues[Matrix.MTRANS_X];

        if (state == State.ZOOM)
            return true;

        if (aWidth <= viewWidth) {
            return false;

        } else if (xOffset == 0 && direction > 0) {
            return false;

        } else if (xOffset == viewWidth - aWidth && direction < 0) {
            return false;
        }

        return true;
    }

    private class MangaImageListener implements OnTouchListener {
        float dist = 0f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                preMatrix.set(matrix);
                firstPoint.set(event.getX(), event.getY());
                state = State.ONE_DOWN;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                dist = spacing(event);
                if (dist > 10f) {
                    preMatrix.set(matrix);
                    midPoint(event);
                    state = State.ZOOM;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (state == State.ONE_DOWN)
                    state = State.DRAG;
                if (state == State.DRAG) {
                    matrix.set(preMatrix);
                    matrix.postTranslate(event.getX() - firstPoint.x,
                            event.getY() - firstPoint.y);
                } else if (state == State.ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(preMatrix);
                        float tScale = newDist / dist;
                        matrix.postScale(tScale, tScale, midPoint.x, midPoint.y);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (state == State.ONE_DOWN)
                    state = State.POINT;
                else
                    state = State.NONE;
                break;
            case MotionEvent.ACTION_CANCEL:
                state = State.NONE;
                break;
            case MotionEvent.ACTION_OUTSIDE:
                state = State.NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getActionIndex() == 1) {
                    preMatrix.set(matrix);
                    firstPoint.set(event.getX(0), event.getY(0));
                    state = State.DRAG;
                } else if (event.getActionIndex() == 0) {
                    preMatrix.set(matrix);
                    firstPoint.set(event.getX(1), event.getY(1));
                    state = State.DRAG;
                }
                break;
            }
            if (state == State.POINT) {
                Toast.makeText(
                        MangaImage.this.getContext(),
                        "Point : x = " + firstPoint.x + ", y = " + firstPoint.y,
                        Toast.LENGTH_SHORT).show();
                //int pointAction = 
                state = State.NONE;
            } else if (state == State.DRAG || state == State.ZOOM) {
                checkBorder();
                MangaImage.this.setImageMatrix(matrix);
            }
            return true;
        }

        private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }

        private void midPoint(MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            midPoint.set(x / 2, y / 2);
        }

        private void checkBorder() {
            matrix.getValues(matrixValues);
            aWidth = (int) (MangaImage.this.getResourceWidth() * matrixValues[Matrix.MSCALE_X]);
            aHeight = (int) (MangaImage.this.getResourceHeight() * matrixValues[Matrix.MSCALE_Y]);
            viewWidth = MangaImage.this.getWidth();
            viewHeight = MangaImage.this.getHeight();

            if (aWidth > viewWidth) {
                if (matrixValues[Matrix.MTRANS_X] > 0)
                    matrixValues[Matrix.MTRANS_X] = 0;
                else if (matrixValues[Matrix.MTRANS_X] < viewWidth - aWidth)
                    matrixValues[Matrix.MTRANS_X] = viewWidth - aWidth;
            } else {
                matrixValues[Matrix.MTRANS_X] = (viewWidth - aWidth) / 2;
            }
            if (aHeight > viewHeight) {
                if (matrixValues[Matrix.MTRANS_Y] > 0)
                    matrixValues[Matrix.MTRANS_Y] = 0;
                else if (matrixValues[5] < viewHeight - aHeight)
                    matrixValues[Matrix.MTRANS_Y] = viewHeight - aHeight;
            } else {
                matrixValues[Matrix.MTRANS_Y] = (viewHeight - aHeight) / 2;
            }
            matrix.setValues(matrixValues);
        }
    }

}
