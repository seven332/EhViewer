package com.hippo.ehviewer.gallery.glrenderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.Arrays;

public class TextTexture extends SpriteTexture {

    private char[] mCharacters;
    private float[] mWidths;
    private float mHeight;
    private float mMaxWidth;

    private TextTexture(Bitmap bitmap, int count, int[] rects, char[] characters,
            float[] widths, float height, float maxWidth) {
        super(bitmap, count, rects);
        mCharacters = characters;
        mWidths = widths;
        mHeight = height;
        mMaxWidth = maxWidth;
    }

    public float getTextWidth(String text) {
        char[] characters = mCharacters;
        float[] widths = mWidths;
        float width = 0.0f;

        for (int i = 0, n = text.length(); i < n; i++) {
            char ch = text.charAt(i);
            int index = Arrays.binarySearch(characters, ch);
            if (index >= 0) {
                width += widths[index];
            } else {
                width += mMaxWidth;
            }
        }

        return width;
    }

    public void drawText(GLCanvas canvas, String text, int x, int y) {
        char[] characters = mCharacters;
        float[] widths = mWidths;

        for (int i = 0, n = text.length(); i < n; i++) {
            char ch = text.charAt(i);
            int index = Arrays.binarySearch(characters, ch);
            if (index >= 0) {
                drawSprite(canvas, index, x, y);
                x += widths[index];
            } else {
                x += mMaxWidth;
            }
        }
    }

    /**
     * Create a TextTexture to draw text
     *
     * @param typeface the typeface
     * @param size text size
     * @param characters all Characters
     * @return the TextTexture
     */
    public static TextTexture create(Typeface typeface, int size, int color, char[] characters) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTypeface(typeface);

        Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
        int fixed = fmi.bottom;
        int height = fmi.bottom - fmi.top;

        int length = characters.length;
        float[] widths = new float[length];
        paint.getTextWidths(characters, 0, length, widths);

        // Caculate bitmap size
        float maxWidth = 0.0f;
        for (float f : widths) {
            maxWidth = Math.max(maxWidth, f);
        }
        int hCount = (int) Math.ceil(Math.sqrt(height / maxWidth * length));
        int vCount = (int) Math.ceil(Math.sqrt(maxWidth / height * length));
        if (hCount * (vCount - 1) > length) {
            vCount--;
        }
        if ((hCount - 1) * vCount > length) {
            hCount--;
        }

        Bitmap bitmap = Bitmap.createBitmap((int) Math.ceil(hCount * maxWidth),
                (int) Math.ceil(vCount * height), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(0, height - fixed);

        // Draw
        int[] rects = new int[length * 4];
        int x = 0;
        int y = 0;
        for (int i = 0; i < length; i++) {
            int offset = i * 4;
            rects[offset + 0] = x;
            rects[offset + 1] = y;
            rects[offset + 2] = (int) widths[i];
            rects[offset + 3] = height;

            canvas.drawText(characters, i, 1, x, y, paint);

            if (i % hCount == hCount - 1) {
                // The end of row
                x = 0;
                y += height;
            } else {
                x += maxWidth;
            }
        }

        return new TextTexture(bitmap, length, rects, characters, widths, height, maxWidth);
    }
}
