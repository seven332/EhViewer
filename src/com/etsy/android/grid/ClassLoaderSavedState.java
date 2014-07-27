/*
 * Copyright (C) 2013 Etsy
 * Copyright (C) 2006 The Android Open Source Project
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

package com.etsy.android.grid;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * A {@link android.os.Parcelable} implementation that should be used by inheritance
 * hierarchies to ensure the state of all classes along the chain is saved.
 */
public abstract class ClassLoaderSavedState implements Parcelable {
    public static final ClassLoaderSavedState EMPTY_STATE = new ClassLoaderSavedState() {};

    private Parcelable mSuperState = EMPTY_STATE;
    private ClassLoader mClassLoader;

    /**
     * Constructor used to make the EMPTY_STATE singleton
     */
    private ClassLoaderSavedState() {
        mSuperState = null;
        mClassLoader = null;
    }

    /**
     * Constructor called by derived classes when creating their ListSavedState objects
     *
     * @param superState The state of the superclass of this view
     */
    protected ClassLoaderSavedState(Parcelable superState, ClassLoader classLoader) {
        mClassLoader = classLoader;
        if (superState == null) {
            throw new IllegalArgumentException("superState must not be null");
        }
        else {
            mSuperState = superState != EMPTY_STATE ? superState : null;
        }
    }

    /**
     * Constructor used when reading from a parcel. Reads the state of the superclass.
     *
     * @param source
     */
    protected ClassLoaderSavedState(Parcel source) {
        // ETSY : we're using the passed super class loader unlike AbsSavedState
        Parcelable superState = source.readParcelable(mClassLoader);
        mSuperState = superState != null ? superState : EMPTY_STATE;
    }

    final public Parcelable getSuperState() {
        return mSuperState;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mSuperState, flags);
    }

    public static final Parcelable.Creator<ClassLoaderSavedState> CREATOR
            = new Parcelable.Creator<ClassLoaderSavedState>() {

        public ClassLoaderSavedState createFromParcel(Parcel in) {
            Parcelable superState = in.readParcelable(null);
            if (superState != null) {
                throw new IllegalStateException("superState must be null");
            }
            return EMPTY_STATE;
        }

        public ClassLoaderSavedState[] newArray(int size) {
            return new ClassLoaderSavedState[size];
        }
    };
}
