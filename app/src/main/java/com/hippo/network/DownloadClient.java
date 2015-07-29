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

package com.hippo.network;

import android.webkit.MimeTypeMap;

import com.hippo.httpclient.HttpClient;
import com.hippo.httpclient.HttpRequest;
import com.hippo.httpclient.HttpResponse;
import com.hippo.io.UniFileOutputStreamPipe;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.Utilities;
import com.hippo.yorozuya.io.OutputStreamPipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadClient {

    private static long transferData(InputStream in, OutputStream out, OnDownloadListener listener)
            throws Exception {
        final byte data[] = new byte[1024 * 4];
        long receivedSize = 0;

        while (true) {
            int bytesRead = in.read(data);
            if (bytesRead == -1) {
                break;
            }
            out.write(data, 0, bytesRead);
            receivedSize += bytesRead;
            if (listener != null) {
                listener.onDonwlad(receivedSize);
            }
        }

        return receivedSize;
    }

    public static void execute(DownloadRequest request) {
        OnDownloadListener listener = request.mListener;
        HttpClient httpClient = request.mHttpClient;
        HttpRequest httpRequest = request.mHttpRequest;
        if (httpRequest == null) {
            httpRequest = new HttpRequest();
            request.mHttpRequest = httpRequest;
        }
        try {
            httpRequest.setUrl(new URL(request.mUrl));
        } catch (MalformedURLException e) {
            // Listener
            if (listener != null) {
                listener.onFailed(e);
            }
            return;
        }

        UniFile uniFile = null;
        OutputStreamPipe osPipe = null;
        try {
            // Listener
            if (listener != null) {
                listener.onStartDownloading();
            }

            HttpResponse httpResponse = httpClient.execute(httpRequest);

            osPipe = request.mOSPipe;
            if (osPipe == null) {
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(httpResponse.getContentType());
                if (extension == null) {
                    extension = MimeTypeMap.getFileExtensionFromUrl(request.mUrl);
                }
                String name = Utilities.getNameFromUrl(request.mUrl);
                String filename;
                if (listener != null) {
                    filename = listener.onFixname(name, extension, request.mFilename);
                } else {
                    filename = request.mFilename;
                }
                request.mFilename = filename;

                // Use Temp filename
                uniFile = request.mDir.createFile(FileUtils.ensureFilename(filename + ".download"));
                if (uniFile == null) {
                    // Listener
                    if (listener != null) {
                        listener.onFailed(new IOException("Can't create file " + filename));
                    }
                    return;
                }
                osPipe = new UniFileOutputStreamPipe(uniFile);
            }
            osPipe.obtain();

            long contentLength = httpResponse.getContentLength();

            // Listener
            if (listener != null) {
                listener.onConnect(contentLength);
            }

            long receivedSize = transferData(httpResponse.getInputStream(), osPipe.open(), listener);

            if (contentLength > 0 && contentLength != receivedSize) {
                throw new IOException("contentLength is " + contentLength + ", but receivedSize is " + receivedSize);
            }

            // Rename
            if (uniFile != null && request.mFilename != null) {
                uniFile.renameTo(request.mFilename);
            }

            // Listener
            if (listener != null) {
                listener.onSucceed();
            }
        } catch (Exception e) {
            // remove download failed file
            if (uniFile != null) {
                uniFile.delete();
            }

            if (listener != null) {
                listener.onFailed(e);
            }
        } finally {
            httpRequest.disconnect();
            if (osPipe != null) {
                osPipe.close();
                osPipe.release();
            }
        }
    }

    public static class SimpleDownloadListener implements OnDownloadListener {

        @Override
        public void onStartDownloading() {
        }

        @Override
        public String onFixname(String newFilename, String newExtension, String oldFilename) {
            return oldFilename;
        }

        @Override
        public void onConnect(long totalSize) {
        }

        @Override
        public void onDonwlad(long receivedSize) {
        }

        @Override
        public void onFailed(Exception e) {
        }

        @Override
        public void onSucceed() {
        }
    }

    public interface OnDownloadListener {

        void onStartDownloading();

        String onFixname(String fileFirstname, String extension, String oldFilename);

        void onConnect(long totalSize);

        void onDonwlad(long receivedSize);

        void onFailed(Exception e);

        void onSucceed();
    }
}
