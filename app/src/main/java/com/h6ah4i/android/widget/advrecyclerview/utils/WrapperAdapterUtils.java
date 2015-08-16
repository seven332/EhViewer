/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.widget.advrecyclerview.utils;

import android.support.v7.widget.RecyclerView;

public class WrapperAdapterUtils {

    private WrapperAdapterUtils() {
    }

    public static <T> T findWrappedAdapter(RecyclerView.Adapter adapter, Class<T> clazz) {
        if (clazz.isInstance(adapter)) {
            return clazz.cast(adapter);
        } else if (adapter instanceof BaseWrapperAdapter) {
            final RecyclerView.Adapter wrappedAdapter = ((BaseWrapperAdapter) adapter).getWrappedAdapter();
            return findWrappedAdapter(wrappedAdapter, clazz);
        } else {
            return null;
        }
    }

    public static RecyclerView.Adapter releaseAll(RecyclerView.Adapter adapter) {
        return releaseCyclically(adapter);
    }

    private static RecyclerView.Adapter releaseCyclically(RecyclerView.Adapter adapter) {
        if (!(adapter instanceof BaseWrapperAdapter)) {
            return adapter;
        }

        final BaseWrapperAdapter wrapperAdapter = (BaseWrapperAdapter) adapter;
        final RecyclerView.Adapter wrappedAdapter = wrapperAdapter.getWrappedAdapter();

        wrapperAdapter.release();

        return releaseCyclically(wrappedAdapter);
    }
}
