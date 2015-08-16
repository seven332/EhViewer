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

package com.h6ah4i.android.widget.advrecyclerview.event;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRecyclerViewEventDistributor<T> {
    protected boolean mReleased;
    protected RecyclerView mRecyclerView;
    protected List<T> mListeners;
    protected boolean mPerformingClearMethod;

    public BaseRecyclerViewEventDistributor() {
    }

    /**
     * Gets attached {@link android.support.v7.widget.RecyclerView}
     *
     * @return The {@link android.support.v7.widget.RecyclerView} instance
     */
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * Release all references.
     * This method should be called to avoid memory leaks.
     */
    public void release() {
        if (mReleased) {
            return;
        }
        mReleased = true;
        clear(true);
        onRelease();
    }

    /**
     * Indicates whether this distributor has been released
     *
     * @return True for this distributor has been released, otherwise false
     */
    public boolean isReleased() {
        return mReleased;
    }

    /**
     * Attaches {@link android.support.v7.widget.RecyclerView} instance.
     *
     * @param rv The {@link android.support.v7.widget.RecyclerView} instance
     */
    public void attachRecyclerView(RecyclerView rv) {
        final String METHOD_NAME = "attachRecyclerView()";

        if (rv == null) {
            throw new IllegalArgumentException("RecyclerView cannot be null");
        }

        verifyIsNotReleased(METHOD_NAME);
        verifyIsNotPerformingClearMethod(METHOD_NAME);

        onRecyclerViewAttached(rv);
    }

    /**
     * Add a {@link T} listener to the chain.
     *
     * @param listener The {@link T} instance
     *
     * @return True if the listener object successfully added, otherwise false. Also returns true if have already been added.
     */
    public boolean add(T listener) {
        return add(listener, -1);
    }

    /**
     * Add a {@link T} listener to the chain at the specified position.
     *
     * @param listener The {@link T} instance
     * @param index Position in the listener chain to insert this listener at.  (&lt; 0:  tail of the chain)
     *
     * @return True if the listener object successfully added, otherwise false. Also returns true if have already been added.
     */
    public boolean add(T listener, int index) {
        final String METHOD_NAME = "add()";

        if (listener == null) {
            throw new IllegalArgumentException("can not specify null for the listener");
        }

        verifyIsNotReleased(METHOD_NAME);
        verifyIsNotPerformingClearMethod(METHOD_NAME);

        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }

        if (!mListeners.contains(listener)) {
            if (index < 0) {
                // append to the tail of the list
                mListeners.add(listener);
            } else {
                // insert to the specified position
                mListeners.add(index, listener);
            }

            // raise onAddedToEventDistributor() event
            if (listener instanceof RecyclerViewEventDistributorListener) {
                ((RecyclerViewEventDistributorListener) listener).onAddedToEventDistributor(this);
            }
        }

        return true;
    }

    /**
     * Remove a {@link T} listener from the chain.
     *
     * @param listener Listener to remove
     *
     * @return True for successfully removed the listener object, otherwise false
     */
    public boolean remove(T listener) {
        final String METHOD_NAME = "remove()";

        if (listener == null) {
            throw new IllegalArgumentException("can not specify null for the listener");
        }

        verifyIsNotPerformingClearMethod(METHOD_NAME);
        verifyIsNotReleased(METHOD_NAME);

        if (mListeners == null) {
            return false;
        }

        final boolean removed = mListeners.remove(listener);

        if (removed) {
            // raise onRemovedFromEventDistributor() event
            if (listener instanceof RecyclerViewEventDistributorListener) {
                ((RecyclerViewEventDistributorListener) listener).onRemovedFromEventDistributor(this);
            }
        }

        return removed;
    }

    /**
     * Remove all listeners from the chain.
     */
    public void clear() {
        clear(false);
    }

    protected void clear(boolean calledFromRelease) {
        final String METHOD_NAME = "clear()";

        if (!calledFromRelease) {
            verifyIsNotReleased(METHOD_NAME);
        }
        verifyIsNotPerformingClearMethod(METHOD_NAME);

        if (mListeners == null) {
            return;
        }

        try {
            mPerformingClearMethod = true;

            final int n = mListeners.size();

            for (int i = n - 1; i >= 0; i--) {
                final T listener = mListeners.remove(i);

                // raise onRemovedFromEventDistributor() event
                if (listener instanceof RecyclerViewEventDistributorListener) {
                    ((RecyclerViewEventDistributorListener) listener).onRemovedFromEventDistributor(this);
                }
            }
        } finally {
            mPerformingClearMethod = false;
        }
    }

    /**
     * Gets the number of underlying listener objects.
     *
     * @return Number of underlying listener objects in the chain.
     */
    public int size() {
        if (mListeners != null) {
            return mListeners.size();
        } else {
            return mListeners.size();
        }
    }

    /**
     * Gets whether the specified listener object is contained in the chain.
     *
     * @param listener Listener to check
     *
     * @return True for the listener contains in the chain, otherwise false
     */
    public boolean contains(T listener) {
        if (mListeners != null) {
            return mListeners.contains(listener);
        } else {
            return false;
        }
    }

    protected void onRelease() {
        mRecyclerView = null;
        mListeners = null;
        mPerformingClearMethod = false;
    }

    protected void onRecyclerViewAttached(RecyclerView rv) {
        mRecyclerView = rv;
    }

    protected void verifyIsNotPerformingClearMethod(String methodName) {
        if (mPerformingClearMethod) {
            throw new IllegalStateException(methodName + " can not be called while performing the clear() method");
        }
    }

    protected void verifyIsNotReleased(String methodName) {
        if (mReleased) {
            throw new IllegalStateException(methodName + " can not be called after release() method called");
        }
    }
}
