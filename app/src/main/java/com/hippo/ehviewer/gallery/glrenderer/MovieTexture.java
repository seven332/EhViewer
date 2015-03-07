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

import junit.framework.Assert;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;

public class MovieTexture extends CanvasTexture {
    private static final int DEFAULT_MOVIE_DURATION = 1000;
    
    private final Movie mMovie;
    private final int mDuration;
    private final long mMovieStart; 
    
    private MovieTexture(Movie movie, int width, int height) {
        super(width, height);
        mMovie = movie;
        int dur = mMovie.duration();
        if (dur == 0)
            mDuration = DEFAULT_MOVIE_DURATION;
        else
            mDuration = dur;
        mMovieStart = System.currentTimeMillis();
    }
    
    public static MovieTexture newInstance(Movie movie) {
        Assert.assertTrue(movie != null);
        return new MovieTexture(movie, movie.width(), movie.height());
    }
    
    @Override
    protected boolean onBind(GLCanvas canvas) {
        invalidateContent();
        setSize(mMovie.width(), mMovie.height());
        return super.onBind(canvas);
    }
    
    @Override
    protected void onDraw(Canvas canvas, Bitmap backing) {
        mMovie.setTime((int)((System.currentTimeMillis() - mMovieStart) % mDuration));
        mMovie.draw(canvas, 0, 0);
    }

}
