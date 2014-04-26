package com.hippo.ehviewer.gallery.glrenderer;

import junit.framework.Assert;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import com.hippo.ehviewer.util.Log;

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
