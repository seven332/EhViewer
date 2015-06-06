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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Representation of a document backed by either a
 * {@link android.provider.DocumentsProvider} or a raw file on disk. This is a
 * utility class designed to emulate the traditional {@link File} interface. It
 * offers a simplified view of a tree of documents, but it has substantial
 * overhead. For optimal performance and a richer feature set, use the
 * {@link android.provider.DocumentsContract} methods and constants directly.
 * <p>
 * There are several differences between documents and traditional files:
 * <ul>
 * <li>Documents express their display name and MIME type as separate fields,
 * instead of relying on file extensions. Some documents providers may still
 * choose to append extensions to their display names, but that's an
 * implementation detail.
 * <li>A single document may appear as the child of multiple directories, so it
 * doesn't inherently know who its parent is. That is, documents don't have a
 * strong notion of path. You can easily traverse a tree of documents from
 * parent to child, but not from child to parent.
 * <li>Each document has a unique identifier within that provider. This
 * identifier is an <em>opaque</em> implementation detail of the provider, and
 * as such it must not be parsed.
 * </ul>
 * <p>
 * Before using this class, first consider if you really need access to an
 * entire subtree of documents. The principle of least privilege dictates that
 * you should only ask for access to documents you really need. If you only need
 * the user to pick a single file, use {@link Intent#ACTION_OPEN_DOCUMENT} or
 * {@link Intent#ACTION_GET_CONTENT}. If you want to let the user pick multiple
 * files, add {@link Intent#EXTRA_ALLOW_MULTIPLE}. If you only need the user to
 * save a single file, use {@link Intent#ACTION_CREATE_DOCUMENT}.
 * <p>
 * If you really do need full access to an entire subtree of documents, start by
 * launching {@link Intent#ACTION_OPEN_DOCUMENT_TREE} to let the user pick a
 * directory. Then pass the resulting {@link Intent#getData()} into
 * {@link #fromTreeUri(Context, Uri)} to start working with the user selected
 * tree.
 * <p>
 * As you navigate the tree of UniFile instances, you can always use
 * {@link #getUri()} to obtain the Uri representing the underlying document for
 * that object, for use with {@link ContentResolver#openInputStream(Uri)}, etc.
 * <p>
 * To simplify your code on devices running
 * {@link Build.VERSION_CODES#KITKAT} or earlier, you can use
 * {@link #fromFile(File)} which emulates the behavior of a
 * {@link android.provider.DocumentsProvider}.
 *
 * @see android.provider.DocumentsProvider
 * @see android.provider.DocumentsContract
 */
public abstract class UniFile {
    static final String TAG = "UniFile";

    private final UniFile mParent;

    UniFile(UniFile parent) {
        mParent = parent;
    }

    /**
     * Create a {@link UniFile} representing the filesystem tree rooted at
     * the given {@link Uri}. This doesn't give you any additional access to the
     * underlying files beyond what your app already has.
     * <p>
     * {@link #getUri()} will return {@code file://} Uris for files explored
     * through this tree.
     */
    public static UniFile fromFile(File file) {
        return new RawFile(null, file);
    }

    /**
     * Create a {@link UniFile} representing the document tree rooted at
     * the given {@link Uri}. This is only useful on devices running
     * {@link Build.VERSION_CODES#LOLLIPOP} or later, and will return
     * {@code null} when called on earlier platform versions.
     *
     * @param treeUri the {@link Intent#getData()} from a successful
     *            {@link Intent#ACTION_OPEN_DOCUMENT_TREE} request.
     */
    public static UniFile fromTreeUri(Context context, Uri treeUri) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 21) {
            return new DocumentFile(null, context,
                    DocumentsContractApi21.prepareTreeUri(treeUri));
        } else {
            return null;
        }
    }

    /**
     * Test if given Uri is backed by a
     * {@link android.provider.DocumentsProvider}.
     */
    public static boolean isDocumentUri(Context context, Uri uri) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            return DocumentsContractApi19.isDocumentUri(context, uri);
        } else {
            return false;
        }
    }

    /**
     * Create a new document as a direct child of this directory.
     *
     * @param displayName name of new document, without any file extension
     *            appended; the underlying provider may choose to append the
     *            extension
     * @return file representing newly created document, or null if failed
     * @see android.provider.DocumentsContract#createDocument(ContentResolver,
     *      Uri, String, String)
     */
    public abstract UniFile createFile(String displayName);

    /**
     * Create a new directory as a direct child of this directory.
     *
     * @param displayName name of new directory
     * @return file representing newly created directory, or null if failed
     * @see android.provider.DocumentsContract#createDocument(ContentResolver,
     *      Uri, String, String)
     */
    public abstract UniFile createDirectory(String displayName);

    /**
     * Return a Uri for the underlying document represented by this file. This
     * can be used with other platform APIs to manipulate or share the
     * underlying content. You can use {@link #isDocumentUri(Context, Uri)} to
     * test if the returned Uri is backed by a
     * {@link android.provider.DocumentsProvider}.
     *
     * @see Intent#setData(Uri)
     * @see Intent#setClipData(android.content.ClipData)
     * @see ContentResolver#openInputStream(Uri)
     * @see ContentResolver#openOutputStream(Uri)
     * @see ContentResolver#openFileDescriptor(Uri, String)
     */
    public abstract Uri getUri();

    /**
     * Return the display name of this document.
     *
     * @see android.provider.DocumentsContract.Document#COLUMN_DISPLAY_NAME
     */
    public abstract String getName();

    /**
     * Return the MIME type of this document.
     *
     * @see android.provider.DocumentsContract.Document#COLUMN_MIME_TYPE
     */
    public abstract String getType();

    /**
     * Return the parent file of this document. Only defined inside of the
     * user-selected tree; you can never escape above the top of the tree.
     * <p>
     * The underlying {@link android.provider.DocumentsProvider} only defines a
     * forward mapping from parent to child, so the reverse mapping of child to
     * parent offered here is purely a convenience method, and it may be
     * incorrect if the underlying tree structure changes.
     */
    public UniFile getParentFile() {
        return mParent;
    }

    /**
     * Indicates if this file represents a <em>directory</em>.
     *
     * @return {@code true} if this file is a directory, {@code false}
     *         otherwise.
     * @see android.provider.DocumentsContract.Document#MIME_TYPE_DIR
     */
    public abstract boolean isDirectory();

    /**
     * Indicates if this file represents a <em>file</em>.
     *
     * @return {@code true} if this file is a file, {@code false} otherwise.
     * @see android.provider.DocumentsContract.Document#COLUMN_MIME_TYPE
     */
    public abstract boolean isFile();

    /**
     * Returns the time when this file was last modified, measured in
     * milliseconds since January 1st, 1970, midnight. Returns 0 if the file
     * does not exist, or if the modified time is unknown.
     *
     * @return the time when this file was last modified.
     * @see android.provider.DocumentsContract.Document#COLUMN_LAST_MODIFIED
     */
    public abstract long lastModified();

    /**
     * Returns the length of this file in bytes. Returns 0 if the file does not
     * exist, or if the length is unknown. The result for a directory is not
     * defined.
     *
     * @return the number of bytes in this file.
     * @see android.provider.DocumentsContract.Document#COLUMN_SIZE
     */
    public abstract long length();

    /**
     * Indicates whether the current context is allowed to read from this file.
     *
     * @return {@code true} if this file can be read, {@code false} otherwise.
     */
    public abstract boolean canRead();

    /**
     * Indicates whether the current context is allowed to write to this file.
     *
     * @return {@code true} if this file can be written, {@code false}
     *         otherwise.
     * @see android.provider.DocumentsContract.Document#COLUMN_FLAGS
     * @see android.provider.DocumentsContract.Document#FLAG_SUPPORTS_DELETE
     * @see android.provider.DocumentsContract.Document#FLAG_SUPPORTS_WRITE
     * @see android.provider.DocumentsContract.Document#FLAG_DIR_SUPPORTS_CREATE
     */
    public abstract boolean canWrite();

    /**
     * Deletes this file.
     * <p>
     * Note that this method does <i>not</i> throw {@code IOException} on
     * failure. Callers must check the return value.
     *
     * @return {@code true} if this file was deleted, {@code false} otherwise.
     * @see android.provider.DocumentsContract#deleteDocument(ContentResolver,
     *      Uri)
     */
    public abstract boolean delete();

    /**
     * Returns a boolean indicating whether this file can be found.
     *
     * @return {@code true} if this file exists, {@code false} otherwise.
     */
    public abstract boolean exists();

    /**
     * Returns the file contained in the directory represented by this
     * file.
     *
     * @return the file, or null if it does not exist.
     */
    public abstract UniFile contains(String displayName);

    /**
     * Returns an array of files contained in the directory represented by this
     * file.
     *
     * @return an array of files or {@code null}.
     * @see android.provider.DocumentsContract#buildChildDocumentsUriUsingTree(Uri,
     *      String)
     */
    public abstract UniFile[] listFiles();

    /**
     * Search through {@link #listFiles()} for the first document matching the
     * given display name. Returns {@code null} when no matching document is
     * found.
     */
    public UniFile findFile(String displayName) {
        for (UniFile doc : listFiles()) {
            if (displayName.equals(doc.getName())) {
                return doc;
            }
        }
        return null;
    }

    /**
     * Renames this file to {@code displayName}.
     * <p>
     * Note that this method does <i>not</i> throw {@code IOException} on
     * failure. Callers must check the return value.
     * <p>
     * Some providers may need to create a new document to reflect the rename,
     * potentially with a different MIME type, so {@link #getUri()} and
     * {@link #getType()} may change to reflect the rename.
     * <p>
     * When renaming a directory, children previously enumerated through
     * {@link #listFiles()} may no longer be valid.
     *
     * @param displayName the new display name.
     * @return true on success.
     * @see android.provider.DocumentsContract#renameDocument(ContentResolver,
     *      Uri, String)
     */
    public abstract boolean renameTo(String displayName);

    /**
     * Open a stream on to the content associated with the file, clean it if it exists
     *
     * @return the {@link OutputStream}
     * @throws IOException
     */
    public abstract OutputStream openOutputStream() throws IOException;

    /**
     * Open a stream on to the content associated with the file
     *
     * @param append {@code true} for do not clean it if it exists
     * @return the {@link OutputStream}
     * @throws IOException
     */
    public abstract OutputStream openOutputStream(boolean append) throws IOException;

    /**
     * Open a stream on to the content associated with the file
     *
     * @return the {@link InputStream}
     * @throws IOException
     */
    public abstract InputStream openInputStream() throws IOException;
}
