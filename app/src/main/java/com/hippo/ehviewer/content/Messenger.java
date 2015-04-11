package com.hippo.ehviewer.content;

import android.util.SparseArray;

import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.util.IntIdGenerator;

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
                        receiver.onReceive(obj);
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
        void onReceive (final Object obj);
    }
}
