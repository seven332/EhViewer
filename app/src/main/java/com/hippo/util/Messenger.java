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

import android.util.SparseArray;

import java.util.HashSet;
import java.util.Set;

public class Messenger {

    private IntIdGenerator mIdGenerator;

    private SparseArray<Set<Receiver>> mReceiverSetMap;

    private static Messenger sInstance;

    public static Messenger getInstance() {
        if (sInstance == null) {
            sInstance = new Messenger();
        }
        return sInstance;
    }

    private Messenger() {
        mIdGenerator = IntIdGenerator.create();
        mReceiverSetMap = new SparseArray<>();
    }

    public int newId() {
        return mIdGenerator.nextId();
    }

    public void notify(final int id, final Object obj) {
        // Make sure do it in UI thread
        AppHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                Set<Receiver> receiverSet = mReceiverSetMap.get(id);
                if (receiverSet != null) {
                    for (Receiver receiver : receiverSet) {
                        receiver.onReceive(id, obj);
                    }
                }
            }
        });
    }

    public void register(int id, Receiver receiver) {
        Set<Receiver> receiverSet = mReceiverSetMap.get(id);
        if (receiverSet == null) {
            receiverSet = new HashSet<>();
            mReceiverSetMap.put(id, receiverSet);
        }

        receiverSet.add(receiver);
    }

    public void unregister(int id, Receiver receiver) {
        Set<Receiver> receiverSet = mReceiverSetMap.get(id);
        if (receiverSet != null) {
            receiverSet.remove(receiver);
            if (receiverSet.isEmpty()) {
                mReceiverSetMap.remove(id);
            }
        }
    }

    public interface Receiver {
        void onReceive(final int id, final Object obj);
    }
}
