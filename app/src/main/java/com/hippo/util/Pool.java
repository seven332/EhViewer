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

package com.hippo.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Pool<E> {

    private static final int MAX_POOL_SIZE = 50;

    private final Object mPoolSync = new Object();
    private int mPoolMaxSize;
    private int mPoolSize = 0;

    private @Nullable Node<E> mFirstNode = null;
    private @Nullable Node<E> mLastNode = null;

    public Pool() {
        this(MAX_POOL_SIZE);
    }

    public Pool(int size) {
        if (size <= 0) {
            throw new IllegalStateException("Pool max size must 1 or larger");
        }
        mPoolMaxSize = size;
    }

    /**
     * @return null if no item in pool
     */
    public @Nullable E obtain() {
        synchronized (mPoolSync) {
            if (mFirstNode != null && mFirstNode.value != null) {
                Node<E> node = mFirstNode;
                mFirstNode = node.next;
                if (mFirstNode != null) {
                    mFirstNode.pre = null;
                }
                E e = node.value;
                recycleNode(node);
                mPoolSize--;
                return e;
            }
        }
        return null;
    }

    /**
     * Add item to pool
     *
     * @param e the item
     */
    public void recycle(@NonNull E e) {
        synchronized (mPoolSync) {
            if (mPoolSize < mPoolMaxSize) {
                Node<E> node = obtainEmptyNode();
                node.value = e;
                node.next = mFirstNode;
                if (mFirstNode != null) {
                    mFirstNode.pre = node;
                }
                mFirstNode = node;
                mPoolSize++;

                if (mLastNode == null) {
                    mLastNode = node;
                }
            }
        }
    }

    private @NonNull Node<E> obtainEmptyNode() {
        if (mLastNode == null || mLastNode.value != null) {
            // No empty node
            return new Node<>();
        } else {
            Node<E> node = mLastNode;
            mLastNode = node.pre;

            if (mLastNode == null) {
                mFirstNode = null;
            } else {
                mLastNode.next = null;
            }

            node.pre = null;
            return node;
        }
    }

    private void recycleNode(@NonNull Node<E> node) {
        if (mLastNode == null) {
            node.value = null;
            node.pre = null;
            node.next = null;
            mFirstNode = node;
            mLastNode = node;
        } else {
            node.value = null;
            mLastNode.next = node;
            node.pre = mLastNode;
            node.next = null;
            mLastNode = node;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Node<E> node = mFirstNode; node != null; node = node.next) {
            sb.append(node.value).append(';');
        }
        return sb.toString();
    }

    private static class Node<V> {
        public @Nullable V value;
        public @Nullable Node<V> pre;
        public @Nullable Node<V> next;
    }
}
