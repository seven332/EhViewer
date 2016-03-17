/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.spider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SpiderInfo {

    private static final String TAG = SpiderInfo.class.getSimpleName();

    static final String TOKEN_FAILED = "failed";

    int startPage = 0;
    long gid = -1;
    String token = null;
    int pages = -1;
    int previewPages = -1;
    int previewPerPage = -1;
    SparseArray<String> pTokenMap = null;


    public static SpiderInfo read(@Nullable UniFile file) {
        if (file == null) {
            return null;
        }

        InputStream is = null;
        try {
            is = file.openInputStream();
            return read(is);
        } catch (IOException e) {
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static int getStartPage(String str) {
        if (null == str) {
            return 0;
        }

        int startPage = 0;
        for (int i = 0, n = str.length(); i < n; i++) {
            startPage *= 16;
            char ch = str.charAt(i);
            if (ch >= '0' && ch <= '9') {
                startPage += ch - '0';
            } else if (ch >= 'a' && ch <= 'f') {
                startPage += ch - 'a' + 10;
            }
        }

        return startPage >= 0 ? startPage : 0;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static SpiderInfo read(@Nullable InputStream is) {
        if (null == is) {
            return null;
        }

        SpiderInfo spiderInfo = null;
        try {
            spiderInfo = new SpiderInfo();
            // Start page
            spiderInfo.startPage = getStartPage(IOUtils.readAsciiLine(is));
            // Gid
            spiderInfo.gid = Long.parseLong(IOUtils.readAsciiLine(is));
            // Token
            spiderInfo.token = IOUtils.readAsciiLine(is);
            // Deprecated, mode, only ex is support now, MUST be 1
            if (1 != Integer.parseInt(IOUtils.readAsciiLine(is))) {
                Log.w(TAG, "Not ex mode");
                return null;
            }
            // Preview pages
            spiderInfo.previewPages = Integer.parseInt(IOUtils.readAsciiLine(is));
            // Preview pre page
            spiderInfo.previewPerPage = Integer.parseInt(IOUtils.readAsciiLine(is));
            // Pages
            spiderInfo.pages = Integer.parseInt(IOUtils.readAsciiLine(is));
            // PToken
            spiderInfo.pTokenMap = new SparseArray<>(spiderInfo.pages);
            while (true) { // EOFException will raise
                String line = IOUtils.readAsciiLine(is);
                int pos = line.indexOf(" ");
                if (pos > 0 || pos < line.length() - 1) {
                    int index = Integer.parseInt(line.substring(0, pos));
                    String pToken = line.substring(pos + 1);
                    spiderInfo.pTokenMap.put(index, pToken);
                } else {
                    Log.e(TAG, "Can't parse index and pToken, index = " + pos);
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Ignore
        }

        if (spiderInfo == null || spiderInfo.gid == -1 || spiderInfo.token == null ||
                spiderInfo.previewPages == -1 || spiderInfo.previewPerPage == -1 ||
                spiderInfo.pages == -1 || spiderInfo.pTokenMap == null) {
            return null;
        } else {
            return spiderInfo;
        }
    }

    public void write(@NonNull OutputStream os) {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(os);
            writer.write(String.format("%08x", startPage >= 0 ? startPage : 0)); // Avoid negative
            writer.write("\n");
            writer.write(Long.toString(gid));
            writer.write("\n");
            writer.write(token);
            writer.write("\n");
            writer.write("1");
            writer.write("\n");
            writer.write(Integer.toString(previewPages));
            writer.write("\n");
            writer.write(Integer.toString(previewPerPage));
            writer.write("\n");
            writer.write(Integer.toString(pages));
            writer.write("\n");
            for (int i = 0; i < pTokenMap.size(); i++) {
                if (TOKEN_FAILED.equals(pTokenMap.valueAt(i))) {
                    continue;
                }
                writer.write(Integer.toString(pTokenMap.keyAt(i)));
                writer.write(" ");
                writer.write(pTokenMap.valueAt(i));
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException e) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(os);
        }
    }
}
