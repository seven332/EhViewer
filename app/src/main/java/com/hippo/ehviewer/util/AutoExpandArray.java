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

package com.hippo.ehviewer.util;

public class AutoExpandArray<E> {

    @SuppressWarnings("unused")
    private static final String TAG = AutoExpandArray.class.getSimpleName();

    private static final int MIN_CAPACITY_INCREMENT = 12;

    private int maxValidIndex = 0;
    private transient Object[] array;

    public AutoExpandArray(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        array = new Object[capacity];
    }

    public AutoExpandArray() {
        array = new Object[MIN_CAPACITY_INCREMENT];
    }

    private synchronized void ensureCapacity(int index) {
        if (index < array.length)
            return;

        Object[] newArray = new Object[(index + 1) << 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        array = newArray;
    }

    public synchronized void setCapacity(int newCapacity) {
        if (newCapacity <= array.length)
            return;

        Object[] newArray = new Object[newCapacity];
        System.arraycopy(array, 0, newArray, 0, array.length);
        array = newArray;
    }

    public synchronized E set(int index, E object) {
        if (index > maxValidIndex)
            maxValidIndex = index;

        ensureCapacity(index);
        @SuppressWarnings("unchecked")
        E result = (E)array[index];
        array[index] = object;
        return result;
    }

    @SuppressWarnings("unchecked")
    public synchronized E get(int index) {
        if (index >= array.length || index < 0) {
            return null;
        }
        return (E)array[index];
    }

    public synchronized int length() {
        return array.length;
    }

    public synchronized int maxValidIndex() {
        return maxValidIndex;
    }
 }
