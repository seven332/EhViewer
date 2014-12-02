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

package com.hippo.ehviewer.gallery.glrenderer;

import javax.microedition.khronos.opengles.GL11;

import android.opengl.GLES20;
import android.util.Log;

import com.hippo.ehviewer.gallery.image.Image;

public class ImageTexture extends BasicTexture implements Uploaded {

    private static final String TAG = ImageTexture.class.getSimpleName();

    private Image mImage;
    private boolean mContentValid = true;
    private boolean mOpaque = true;

    public ImageTexture(Image image) {
        mImage = image;
        setSize(image.getWidth(), image.getHeight());
    }

    public void start() {
        if (mImage != null)
            mImage.start();
    }

    public void stop() {
        if (mImage != null)
            mImage.stop();
    }

    public boolean isAnimated() {
        if (mImage == null || mImage.isRecycled())
            return false;
        else
            return mImage.isAnimated();
    }

    @Override
    public boolean isOpaque() {
        return mOpaque;
    }

    public void setOpaque(boolean opaque) {
        mOpaque = opaque;
    }

    /**
     * Whether the content on GPU is valid.
     */
    @Override
    public boolean isContentValid() {
        return isLoaded() && mContentValid;
    }

    @Override
    protected int getTarget() {
        return GL11.GL_TEXTURE_2D;
    }

    public int getFormat() {
        return mImage.getFormat();
    }

    public int getType() {
        return mImage.getType();
    }

    public static void checkError() {
        int error = GLES20.glGetError();
        if (error != 0) {
            Throwable t = new Throwable();
            Log.e(TAG, "GL error: " + error, t);
        }
    }

    private void uploadToCanvas(GLCanvas canvas) {
        mId = canvas.getGLId().generateTexture();
        int format = getFormat();
        int type = getType();

        canvas.setTextureParameters(this);
        canvas.initializeTextureSize(this, format, type);

        GLES20.glBindTexture(getTarget(), mId);
        checkError();
        mImage.render();

        setAssociatedCanvas(canvas);
        mState = STATE_LOADED;
        mContentValid = true;
    }

    @Override
    public void updateContent(GLCanvas canvas) {
        if (!isLoaded()) {
            uploadToCanvas(canvas);
        } else if (isAnimated()) {
            GLES20.glBindTexture(getTarget(), mId);
            checkError();
            mImage.render();

            mContentValid = true;
        }
    }

    @Override
    protected boolean onBind(GLCanvas canvas) {
        if (mImage == null || mImage.isRecycled()) {
            return false;
        } else {
            updateContent(canvas);
            return isContentValid();
        }
    }

    @Override
    public void recycle() {
        super.recycle();
        if (mImage != null) {
            mImage.recycle();
            mImage = null;
        }
    }
}
