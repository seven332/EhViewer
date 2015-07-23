/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.hippo.ehviewer.gallery.util;

import android.util.Log;

import com.hippo.yorozuya.IOUtils;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

// ProfileData keeps profiling samples in a tree structure.
// The addSample() method adds a sample. The dumpToFile() method saves the data
// to a file. The reset() method clears all samples.
public class ProfileData {
    @SuppressWarnings("unused")
    private static final String TAG = "ProfileData";

    private static class Node {
        public int id;  // this is the name of this node, mapped from mNameToId
        public Node parent;
        public int sampleCount;
        public ArrayList<Node> children;
        public Node(Node parent, int id) {
            this.parent = parent;
            this.id = id;
        }
    }

    private Node mRoot;
    private int mNextId;
    private HashMap<String, Integer> mNameToId;
    private DataOutputStream mOut;
    private byte mScratch[] = new byte[4];  // scratch space for writeInt()

    public ProfileData() {
        mRoot = new Node(null, -1);  // The id of the root node is unused.
        mNameToId = new HashMap<>();
    }

    public void reset() {
        mRoot = new Node(null, -1);
        mNameToId.clear();
        mNextId = 0;
    }

    private int nameToId(String name) {
        Integer id = mNameToId.get(name);
        if (id == null) {
            id = ++mNextId;  // The tool doesn't want id=0, so we start from 1.
            mNameToId.put(name, id);
        }
        return id;
    }

    public void addSample(String[] stack) {
        int[] ids = new int[stack.length];
        for (int i = 0; i < stack.length; i++) {
            ids[i] = nameToId(stack[i]);
        }

        Node node = mRoot;
        for (int i = stack.length - 1; i >= 0; i--) {
            if (node.children == null) {
                node.children = new ArrayList<>();
            }

            int id = ids[i];
            ArrayList<Node> children = node.children;
            int j;
            for (j = 0; j < children.size(); j++) {
                if (children.get(j).id == id) break;
            }
            if (j == children.size()) {
                children.add(new Node(node, id));
            }

            node = children.get(j);
        }

        node.sampleCount++;
    }

    public void dumpToFile(String filename) {
        try {
            mOut = new DataOutputStream(new FileOutputStream(filename));
            // Start record
            writeInt(0);
            writeInt(3);
            writeInt(1);
            writeInt(20000);  // Sampling period: 20ms
            writeInt(0);

            // Samples
            writeAllStacks(mRoot, 0);

            // End record
            writeInt(0);
            writeInt(1);
            writeInt(0);
            writeAllSymbols();
        } catch (IOException ex) {
            Log.w("Failed to dump to file", ex);
        } finally {
            IOUtils.closeQuietly(mOut);
        }
    }

    // Writes out one stack, consisting of N+2 words:
    // first word: sample count
    // second word: depth of the stack (N)
    // N words: each word is the id of one address in the stack
    private void writeOneStack(Node node, int depth) throws IOException {
        writeInt(node.sampleCount);
        writeInt(depth);
        while (depth-- > 0) {
            writeInt(node.id);
            node = node.parent;
        }
    }

    private void writeAllStacks(Node node, int depth) throws IOException {
        if (node.sampleCount > 0) {
            writeOneStack(node, depth);
        }

        ArrayList<Node> children = node.children;
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                writeAllStacks(children.get(i), depth + 1);
            }
        }
    }

    // Writes out the symbol table. Each line is like:
    // 0x17e java.util.ArrayList.isEmpty(ArrayList.java:319)
    private void writeAllSymbols() throws IOException {
        for (Entry<String, Integer> entry : mNameToId.entrySet()) {
            mOut.writeBytes(String.format("0x%x %s\n", entry.getValue(), entry.getKey()));
        }
    }

    private void writeInt(int v) throws IOException {
        mScratch[0] = (byte) v;
        mScratch[1] = (byte) (v >> 8);
        mScratch[2] = (byte) (v >> 16);
        mScratch[3] = (byte) (v >> 24);
        mOut.write(mScratch);
    }
}
