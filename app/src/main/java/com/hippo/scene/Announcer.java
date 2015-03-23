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

package com.hippo.scene;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.io.Serializable;

public class Announcer {

    private @Nullable Bundle mExtras;

    public boolean getBooleanExtra(String name, boolean defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getBoolean(name, defaultValue);
    }

    public byte getByteExtra(String name, byte defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getByte(name, defaultValue);
    }

    public short getShortExtra(String name, short defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getShort(name, defaultValue);
    }

    public char getCharExtra(String name, char defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getChar(name, defaultValue);
    }

    public int getIntExtra(String name, int defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getInt(name, defaultValue);
    }

    public long getLongExtra(String name, long defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getLong(name, defaultValue);
    }

    public float getFloatExtra(String name, float defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getFloat(name, defaultValue);
    }

    public double getDoubleExtra(String name, double defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getDouble(name, defaultValue);
    }

    public String getStringExtra(String name) {
        return mExtras == null ? null : mExtras.getString(name);
    }

    public CharSequence getCharSequenceExtra(String name) {
        return mExtras == null ? null : mExtras.getCharSequence(name);
    }

    public <T extends Parcelable> T getParcelableExtra(String name) {
        return mExtras == null ? null : mExtras.<T>getParcelable(name);
    }

    public Parcelable[] getParcelableArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getParcelableArray(name);
    }

    public Serializable getSerializableExtra(String name) {
        return mExtras == null ? null : mExtras.getSerializable(name);
    }

    public boolean[] getBooleanArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getBooleanArray(name);
    }

    public byte[] getByteArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getByteArray(name);
    }

    public short[] getShortArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getShortArray(name);
    }

    public char[] getCharArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getCharArray(name);
    }

    public int[] getIntArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getIntArray(name);
    }

    public long[] getLongArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getLongArray(name);
    }

    public float[] getFloatArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getFloatArray(name);
    }

    public double[] getDoubleArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getDoubleArray(name);
    }

    public String[] getStringArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getStringArray(name);
    }

    public CharSequence[] getCharSequenceArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getCharSequenceArray(name);
    }

    public Bundle getBundleExtra(String name) {
        return mExtras == null ? null : mExtras.getBundle(name);
    }

    public Bundle getExtras() {
        return (mExtras != null)
                ? new Bundle(mExtras)
                : null;
    }

    public Announcer putExtra(String name, boolean value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBoolean(name, value);
        return this;
    }

    public Announcer putExtra(String name, byte value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putByte(name, value);
        return this;
    }

    public Announcer putExtra(String name, char value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putChar(name, value);
        return this;
    }

    public Announcer putExtra(String name, short value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putShort(name, value);
        return this;
    }

    public Announcer putExtra(String name, int value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putInt(name, value);
        return this;
    }

    public Announcer putExtra(String name, long value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLong(name, value);
        return this;
    }

    public Announcer putExtra(String name, float value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloat(name, value);
        return this;
    }

    public Announcer putExtra(String name, double value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDouble(name, value);
        return this;
    }

    public Announcer putExtra(String name, String value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putString(name, value);
        return this;
    }

    public Announcer putExtra(String name, CharSequence value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharSequence(name, value);
        return this;
    }

    public Announcer putExtra(String name, Parcelable value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putParcelable(name, value);
        return this;
    }

    public Announcer putExtra(String name, Parcelable[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putParcelableArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, Serializable value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putSerializable(name, value);
        return this;
    }

    public Announcer putExtra(String name, boolean[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBooleanArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, byte[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putByteArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, short[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putShortArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, char[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, int[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putIntArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, long[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLongArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, float[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloatArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, double[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDoubleArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, String[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, CharSequence[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharSequenceArray(name, value);
        return this;
    }

    public Announcer putExtra(String name, Bundle value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBundle(name, value);
        return this;
    }

    public Announcer putExtras(Announcer src) {
        if (src.mExtras != null) {
            if (mExtras == null) {
                mExtras = new Bundle(src.mExtras);
            } else {
                mExtras.putAll(src.mExtras);
            }
        }
        return this;
    }

    public Announcer putExtras(Bundle extras) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putAll(extras);
        return this;
    }

    public void removeExtra(String name) {
        if (mExtras != null) {
            mExtras.remove(name);
            if (mExtras.size() == 0) {
                mExtras = null;
            }
        }
    }
}
