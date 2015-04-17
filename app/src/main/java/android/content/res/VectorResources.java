/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.res;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LongSparseArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;

import com.hippo.drawable.AnimatedVectorDrawable;
import com.hippo.drawable.VectorDrawable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

// TODO Only load register drawable from itself
/**
 * It is a wrap
 */
public class VectorResources extends Resources {

    private static final String TAG = VectorResources.class.getSimpleName();

    private Context mContext;
    private Resources mBase;

    // These are protected by mAccessLock.

    /*package*/ final Object mAccessLock = new Object();
    /*package*/ final LongSparseArray<WeakReference<Drawable.ConstantState> > mDrawableCache
            = new LongSparseArray<WeakReference<Drawable.ConstantState> >(0);
    /*package*/ final LongSparseArray<WeakReference<Drawable.ConstantState> > mColorDrawableCache
            = new LongSparseArray<WeakReference<Drawable.ConstantState> >(0);

    public VectorResources(Context context, Resources res) {
        super(res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());
        mContext = context;
        mBase = res;
    }

    public boolean isBase(Resources res) {
        return mBase == res;
    }

    @Override
    Drawable loadDrawable(TypedValue value, int id)
            throws NotFoundException {
        boolean isColorDrawable = false;
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            isColorDrawable = true;
        }
        final long key = isColorDrawable ? value.data :
                (((long) value.assetCookie) << 32) | value.data;

        Drawable dr = getCachedDrawable(isColorDrawable ? mColorDrawableCache : mDrawableCache, key);

        if (dr != null) {
            return dr;
        }

        if (isColorDrawable) {
            dr = new ColorDrawable(value.data);
        }

        if (dr == null) {
            if (value.string == null) {
                throw new NotFoundException(
                        "Resource is not a Drawable (color or path): " + value);
            }

            String file = value.string.toString();

            if (file.endsWith(".xml")) {
                // Trace.traceBegin(Trace.TRACE_TAG_RESOURCES, file);
                try {
                    XmlResourceParser rp = getXml(id);
                    // XmlResourceParser rp = loadXmlResourceParser(
                    //         file, id, value.assetCookie, "drawable");
                    dr = createDrawableFromXml(rp);
                    rp.close();
                } catch (Exception e) {
                    // Trace.traceEnd(Trace.TRACE_TAG_RESOURCES);
                    NotFoundException rnf = new NotFoundException(
                            "File " + file + " from drawable resource ID #0x"
                                    + Integer.toHexString(id));
                    rnf.initCause(e);
                    throw rnf;
                }
                // Trace.traceEnd(Trace.TRACE_TAG_RESOURCES);

            } else {
                // Trace.traceBegin(Trace.TRACE_TAG_RESOURCES, file);
                try {
                    InputStream is = openRawResource(id, value);
                    //InputStream is = mAssets.openNonAsset(
                    //        value.assetCookie, file, AssetManager.ACCESS_STREAMING);
                    //                System.out.println("Opened file " + file + ": " + is);
                    dr = Drawable.createFromResourceStream(this, value, is,
                            file, null);
                    is.close();
                    //                System.out.println("Created stream: " + dr);
                } catch (Exception e) {
                    // Trace.traceEnd(Trace.TRACE_TAG_RESOURCES);
                    NotFoundException rnf = new NotFoundException(
                            "File " + file + " from drawable resource ID #0x"
                                    + Integer.toHexString(id));
                    rnf.initCause(e);
                    throw rnf;
                }
                // Trace.traceEnd(Trace.TRACE_TAG_RESOURCES);
            }
        }
        Drawable.ConstantState cs;
        if (dr != null) {
            dr.setChangingConfigurations(value.changingConfigurations);
            cs = dr.getConstantState();
            if (cs != null) {
                synchronized (mAccessLock) {
                    //Log.i(TAG, "Saving cached drawable @ #" +
                    //        Integer.toHexString(key.intValue())
                    //        + " in " + this + ": " + cs);
                    if (isColorDrawable) {
                        mColorDrawableCache.put(key, new WeakReference<Drawable.ConstantState>(cs));
                    } else {
                        mDrawableCache.put(key, new WeakReference<Drawable.ConstantState>(cs));
                    }
                }
            }
        }

        return dr;
    }

    private Drawable getCachedDrawable(
            LongSparseArray<WeakReference<Drawable.ConstantState>> drawableCache,
            long key) {
        synchronized (mAccessLock) {
            WeakReference<Drawable.ConstantState> wr = drawableCache.get(key);
            if (wr != null) {   // we have the key
                Drawable.ConstantState entry = wr.get();
                if (entry != null) {
                    //Log.i(TAG, "Returning cached drawable @ #" +
                    //        Integer.toHexString(((Integer)key).intValue())
                    //        + " in " + this + ": " + entry);
                    return entry.newDrawable(this);
                }
                else {  // our entry has been purged
                    drawableCache.delete(key);
                }
            }
        }
        return null;
    }

    private Drawable createDrawableFromXml(XmlResourceParser parser)
            throws XmlPullParserException, IOException {
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        while ((type=parser.next()) != XmlPullParser.START_TAG &&
                type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        Drawable drawable = createDrawableFromXmlInner(parser, attrs);

        if (drawable == null) {
            throw new RuntimeException("Unknown initial tag: " + parser.getName());
        }

        return drawable;
    }

    private Drawable createDrawableFromXmlInner(XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        final Drawable drawable;
        final String name = parser.getName();
        switch (name) {
            case "vector":
                VectorDrawable vectorDrawable = new VectorDrawable();
                vectorDrawable.inflate(this, parser, attrs);
                drawable = vectorDrawable;
                break;
            case "animated-vector":
                AnimatedVectorDrawable animatedVectorDrawable = new AnimatedVectorDrawable();
                animatedVectorDrawable.inflate(mContext, parser, attrs);
                drawable = animatedVectorDrawable;
                break;
            default:
                drawable = Drawable.createFromXmlInner(this, parser, attrs);
        }
        return drawable;
    }
}
