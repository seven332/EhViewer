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

package com.hippo.scene.preference;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PreferenceCenter {

    static final int TYPE_PEFERENCE_DIVIDER = 0;
    static final int TYPE_PEFERENCE_CATEGORY = 1;
    static final int TYPE_PEFERENCE = 2;
    static final int TYPE_SWITCH_PEFERENCE = 3;
    static final int TYPE_DIALOG_PEFERENCE = 4;

    static SparseArray<Method> mMethodArray = new SparseArray<>();

    static {
        register(TYPE_PEFERENCE_DIVIDER, PreferenceDivider.class);
        register(TYPE_PEFERENCE_CATEGORY, PreferenceCategory.class);
        register(TYPE_PEFERENCE, Preference.class);
        register(TYPE_SWITCH_PEFERENCE, SwitchPreference.class);
        register(TYPE_DIALOG_PEFERENCE, DialogPreference.class);
    }

    @SuppressWarnings("unchecked")
    public static void register(int type, Class clazz) {
        try {
            Method method = clazz.getMethod("createViewHolder", Context.class, ViewGroup.class);
            mMethodArray.append(type, method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    public static RecyclerView.ViewHolder createViewHolder(int type, Context content, ViewGroup parent) {
        Method method = mMethodArray.get(type);
        if (method == null) {
            throw new IllegalStateException("You must register for type " + type);
        }

        try {
            return (RecyclerView.ViewHolder) method.invoke(null, content, parent);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
