/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.gallery.gifdecoder;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class ByteArrayPool {

    private final Set<WeakReference<byte[]>> mReusableSet = new LinkedHashSet<>();

    public synchronized void add(byte[] bytes) {
        if (bytes != null) {
            mReusableSet.add(new WeakReference<>(bytes));
        }
    }

    public synchronized byte[] getAtLeast(int size) {
        final Iterator<WeakReference<byte[]>> iterator = mReusableSet.iterator();
        byte[] item;
        while (iterator.hasNext()) {
            item = iterator.next().get();
            if (item != null) {
                if (item.length >= size) {
                    // Remove from reusable set so it can't be used again.
                    iterator.remove();
                    return item;
                }
            } else {
                // Remove from the set if the reference has been cleared or
                // it can't be used.
                iterator.remove();
            }
        }
        return null;
    }

    public synchronized byte[] getExactly(int size) {
        final Iterator<WeakReference<byte[]>> iterator = mReusableSet.iterator();
        byte[] item;
        while (iterator.hasNext()) {
            item = iterator.next().get();
            if (item != null) {
                if (item.length == size) {
                    // Remove from reusable set so it can't be used again.
                    iterator.remove();
                    return item;
                }
            } else {
                // Remove from the set if the reference has been cleared or
                // it can't be used.
                iterator.remove();
            }
        }
        return null;
    }
}
