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

import java.lang.ref.WeakReference;

public class RecyclerViewRecyclerEventDistributor extends BaseRecyclerViewEventDistributor<RecyclerView.RecyclerListener> {

    private InternalRecyclerListener mInternalRecyclerListener;

    public RecyclerViewRecyclerEventDistributor() {
        super();

        mInternalRecyclerListener = new InternalRecyclerListener(this);
    }

    @Override
    protected void onRecyclerViewAttached(RecyclerView rv) {
        super.onRecyclerViewAttached(rv);

        rv.setRecyclerListener(mInternalRecyclerListener);
    }

    @Override
    protected void onRelease() {
        super.onRelease();

        if (mInternalRecyclerListener != null) {
            mInternalRecyclerListener.release();
            mInternalRecyclerListener = null;
        }
    }

    /*package*/ void handleOnViewRecycled(RecyclerView.ViewHolder holder) {
        if (mListeners == null) {
            return;
        }

        for (RecyclerView.RecyclerListener listener : mListeners) {
            listener.onViewRecycled(holder);
        }
    }

    private static class InternalRecyclerListener implements RecyclerView.RecyclerListener {
        private WeakReference<RecyclerViewRecyclerEventDistributor> mRefDistributor;

        public InternalRecyclerListener(RecyclerViewRecyclerEventDistributor distributor) {
            super();
            mRefDistributor = new WeakReference<>(distributor);
        }

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            final RecyclerViewRecyclerEventDistributor distributor = mRefDistributor.get();
            
            if (distributor != null) {
                distributor.handleOnViewRecycled(holder);
            }
        }

        public void release() {
            mRefDistributor.clear();
        }
    }
}
