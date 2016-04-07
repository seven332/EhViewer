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

package com.hippo.dict;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictFilter {
    private static final String TAG = "DictFilter";
    // pattern for language
    private static final String enRegEx = "^[0-9a-zA-Z_\\s]+$";
    private static final String zhRegEx = "^[0-9a-zA-Z_\\s[\u4e00-\u9fa5]+]+$";

    public static class EnFilter implements Filter {

        @Override
        public String[] filter(String[] data) {
            Log.d(TAG, "[enfilter] ------------------");
            Pattern localeRegEx = Pattern.compile(zhRegEx);
            List<String> result = new ArrayList<>();
            for (String s : data) {
                Matcher m = localeRegEx.matcher(s);
                if (m.matches()) {
                    result.add(s);
                    Log.d(TAG, "[enfilter] " + s);
                }
            }
            return result.toArray(new String[result.size()]);
        }
    }

    public static class LocaleFilter implements Filter {

        @Override
        public String[] filter(String[] data) {
            Log.d(TAG, "[localefilter] ------------------");
            Pattern localeRegEx = Pattern.compile(enRegEx);
            List<String> result = new ArrayList<>();
            for (String s : data) {
                Matcher m = localeRegEx.matcher(s);
                if (m.matches()) {
                    result.add(s);
                    Log.d(TAG, "[localefilter] " + s);
                }
            }
            return result.toArray(new String[result.size()]);
        }
    }



    public interface Filter {
        String[] filter(String[] data);
    }
}
