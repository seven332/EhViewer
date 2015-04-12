package com.hippo.ehviewer.ehclient;

import android.support.annotation.Nullable;
import android.util.Pair;

import com.hippo.ehviewer.util.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TorrentParser {

    /**
     *
     * @return the param or new instance if size changed
     */
    public static Pair<String, String>[] parse(String body, @Nullable Pair<String, String>[] torrents) {
        List<Pair<String, String>> torrentList;
        int arrayLength;
        int size = 0;

        if (torrents == null) {
            arrayLength = 0;
            torrentList = new LinkedList<>();
        } else {
            arrayLength = torrents.length;
            torrentList = null;
        }

        Pattern p = Pattern.compile("<td colspan=\"5\"> &nbsp; <a href=\"([^\"]+)\">([^<>]*)</a></td>");
        Matcher m = p.matcher(body);
        while (m.find()) {
            size++;

            if (size > arrayLength && torrentList == null) {
                torrentList = new LinkedList<>();
                // Copy data
                int copySize = size - 1;
                for (int i = 0; i < copySize; i++) {
                    torrentList.add(torrents[i]);
                }
            }

            // Remove ?p= to make torrent redistributable
            String url = m.group(1);
            if (url == null) {
                size--;
                continue;
            }
            int index = url.indexOf("?p=");
            if (index != -1) {
                url = url.substring(0, index);
            }

            Pair<String, String> newItem = new Pair<>(url, Utils.unescapeXml(m.group(2)));
            if (torrentList != null) {
                torrentList.add(newItem);
            } else {
                torrents[size - 1] = newItem;
            }
        }

        if (torrentList != null) {
            //noinspection unchecked,SuspiciousToArrayCall
            return (Pair<String, String>[]) torrentList.toArray(new Pair[torrentList.size()]);
        } else if (size == arrayLength){
            return torrents;
        } else {
            //noinspection unchecked
            Pair<String, String>[] result = new Pair[size];
            System.arraycopy(torrents, 0, result, 0, size);
            return result;
        }
    }
}
