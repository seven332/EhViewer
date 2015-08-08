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

package com.hippo.ehviewer.gallery;

import android.util.Log;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.AutoExpandArray;
import com.hippo.yorozuya.IOUtils;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SpiderInfo {

    private static final String TAG = SpiderInfo.class.getSimpleName();

    private static final int VERSION = 1;

    public GalleryBase galleryBase;
    public int pages = -1;
    public String previewSize;
    public int previewPages;
    public int previewCountPerPage;
    public AutoExpandArray<String> tokens;

    /**
     * version
     * galleryBase.gid
     * galleryBase.token
     * galleryBase.title
     * galleryBase.titleJpn
     * galleryBase.thumb
     * galleryBase.category
     * galleryBase.posted
     * galleryBase.uploader
     * galleryBase.rating
     * pages
     * previewSize
     * previewPages
     * previewCountPerPage
     * index token
     * index token
     * ...
     *
     * @param os the output stream
     */
    public void write(OutputStream os) throws IOException {
        if (galleryBase == null || pages == -1 || previewSize == null) {
            // No need to write
            return;
        }

        GalleryBase gb = galleryBase;
        OutputStreamWriter osWrite = new OutputStreamWriter(os, "UTF-8");
        osWrite.append(Integer.toString(VERSION)).append('\n')
                .append(Integer.toString(gb.gid)).append('\n')
                .append(gb.token).append('\n')
                .append(stringToHexString(gb.title)).append('\n')
                .append(stringToHexString(gb.titleJpn)).append('\n')
                .append(gb.thumb).append('\n')
                .append(Integer.toString(gb.category)).append('\n')
                .append(gb.posted).append('\n')
                .append(gb.uploader).append('\n')
                .append(Float.toString(gb.rating)).append('\n')
                .append(Integer.toString(pages)).append('\n')
                .append(previewSize).append('\n')
                .append(Integer.toString(previewPages)).append('\n')
                .append(Integer.toString(previewCountPerPage)).append('\n');

        if (tokens != null) {
            int length = Math.min(tokens.maxValidIndex() + 1, pages);
            for (int i = 0; i < length; i++) {
                String token = tokens.get(i);
                if (token != null) {
                    osWrite.append(Integer.toString(i)).append(':')
                            .append(token).append('\n');
                }
            }
        }

        osWrite.flush();
        osWrite.close();
    }


    public static SpiderInfo read(InputStream is) throws Exception {
        BufferedInputStream bis = new BufferedInputStream(is);
        SpiderInfo spiderInfo = new SpiderInfo();

        int version = Integer.parseInt(IOUtils.readAsciiLine(bis));
        if (VERSION != version){
            throw new IllegalStateException("Version should be " + VERSION +
                    ", but it is " + version);
        }

        GalleryBase gb = new GalleryBase();
        gb.gid = Integer.parseInt(IOUtils.readAsciiLine(bis));
        gb.token = IOUtils.readAsciiLine(bis);
        gb.title = hexStringToString(IOUtils.readAsciiLine(bis));
        gb.titleJpn = hexStringToString(IOUtils.readAsciiLine(bis));
        gb.thumb = IOUtils.readAsciiLine(bis);
        gb.category = Integer.parseInt(IOUtils.readAsciiLine(bis));
        gb.posted = IOUtils.readAsciiLine(bis);
        gb.uploader = IOUtils.readAsciiLine(bis);
        gb.rating = Float.parseFloat(IOUtils.readAsciiLine(bis));
        spiderInfo.galleryBase = gb;

        spiderInfo.pages = Integer.parseInt(IOUtils.readAsciiLine(bis));
        spiderInfo.previewSize = IOUtils.readAsciiLine(bis);
        if (!spiderInfo.previewSize.equals(EhConfig.PREVIEW_SIZE_NORMAL) &&
                !spiderInfo.previewSize.equals(EhConfig.PREVIEW_SIZE_LARGE)) {
            throw new IllegalStateException("Unknown preview size " + spiderInfo.previewSize);
        }
        spiderInfo.previewPages = Integer.parseInt(IOUtils.readAsciiLine(bis));
        spiderInfo.previewCountPerPage = Integer.parseInt(IOUtils.readAsciiLine(bis));

        AutoExpandArray<String> tokens = new AutoExpandArray<>(spiderInfo.pages);
        try {
            //noinspection InfiniteLoopStatement
            for (;;) {
                String line = IOUtils.readAsciiLine(is);
                int pos = line.indexOf(":");
                if (pos < 0) {
                    Log.e(TAG, "Can't parse line " + line);
                    continue;
                }
                try {
                    int index = Integer.parseInt(line.substring(0, pos));
                    String token = line.substring(pos + 1);
                    tokens.set(index, token);
                } catch (NumberFormatException e) {
                    // Empty
                    Log.e(TAG, "Can't parse line " + line);
                }
            }
        } catch (EOFException e) {
            // Empty
        }
        spiderInfo.tokens = tokens;

        return spiderInfo;
    }

    public static String stringToHexString(String str) {
        StringBuilder sb = new StringBuilder(320);
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            String s = Integer.toString((int) ch, 16);
            int length2 = 4 - s.length();
            for (int j = 0; j < length2; j++) {
                sb.append('0');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String hexStringToString(String str) {
        AssertUtils.assertEquals("Hex String's length must be a multiple of 4.", str.length() % 4, 0);

        StringBuilder sb = new StringBuilder(80);
        int length = str.length();
        for (int i = 0; i < length; i += 4) {
            sb.append((char) Integer.parseInt(str.substring(i, i + 4), 16));
        }
        return sb.toString();
    }
}
