/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.hippo.unifile;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DocumentFile extends UniFile {

    private Context mContext;
    private Uri mUri;

    DocumentFile(UniFile parent, Context context, Uri uri) {
        super(parent);
        mContext = context;
        mUri = uri;
    }

    @Override
    public UniFile createFile(String displayName) {
        UniFile child = contains(displayName);

        if (child != null) {
            if (child.isFile()) {
                return child;
            } else {
                return null;
            }
        } else {
            final Uri result = DocumentsContractApi21.createFile(mContext, mUri, "application/octet-stream", displayName);
            return (result != null) ? new DocumentFile(this, mContext, result) : null;
        }
    }

    @Override
    public UniFile createDirectory(String displayName) {
        UniFile child = contains(displayName);

        if (child != null) {
            if (child.isDirectory()) {
                return child;
            } else {
                return null;
            }
        } else {
            final Uri result = DocumentsContractApi21.createDirectory(mContext, mUri, displayName);
            return (result != null) ? new DocumentFile(this, mContext, result) : null;
        }
    }

    @Override
    public Uri getUri() {
        return mUri;
    }

    @Override
    public String getName() {
        return DocumentsContractApi19.getName(mContext, mUri);
    }

    @Override
    public String getType() {
        return DocumentsContractApi19.getType(mContext, mUri);
    }

    @Override
    public boolean isDirectory() {
        return DocumentsContractApi19.isDirectory(mContext, mUri);
    }

    @Override
    public boolean isFile() {
        return DocumentsContractApi19.isFile(mContext, mUri);
    }

    @Override
    public long lastModified() {
        return DocumentsContractApi19.lastModified(mContext, mUri);
    }

    @Override
    public long length() {
        return DocumentsContractApi19.length(mContext, mUri);
    }

    @Override
    public boolean canRead() {
        return DocumentsContractApi19.canRead(mContext, mUri);
    }

    @Override
    public boolean canWrite() {
        return DocumentsContractApi19.canWrite(mContext, mUri);
    }

    @Override
    public boolean delete() {
        return DocumentsContractApi19.delete(mContext, mUri);
    }

    @Override
    public boolean exists() {
        return DocumentsContractApi19.exists(mContext, mUri);
    }

    @Override
    public UniFile contains(String displayName) {
        Uri childUri = DocumentsContractApi21.buildChildUri(mUri, displayName);
        return DocumentsContractApi19.exists(mContext, childUri) ?
                new DocumentFile(this, mContext, childUri) : null;
    }

    @Override
    public UniFile[] listFiles() {
        final Uri[] result = DocumentsContractApi21.listFiles(mContext, mUri);
        final UniFile[] resultFiles = new UniFile[result.length];
        for (int i = 0; i < result.length; i++) {
            resultFiles[i] = new DocumentFile(this, mContext, result[i]);
        }
        return resultFiles;
    }

    @Override
    public boolean renameTo(String displayName) {
        final Uri result = DocumentsContractApi21.renameTo(mContext, mUri, displayName);
        if (result != null) {
            mUri = result;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return mContext.getContentResolver().openOutputStream(mUri);
    }

    @Override
    public OutputStream openOutputStream(boolean append) throws IOException {
        return mContext.getContentResolver().openOutputStream(mUri, append ? "wa" : "w");
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return mContext.getContentResolver().openInputStream(mUri);
    }
}
