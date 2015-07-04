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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene;

import java.util.HashMap;
import java.util.Map;

public class Announcer {

    private String mAction;

    private Map<String, Object> mMap = new HashMap<>();

    public void setAction(String action) {
        mAction = action;
    }

    public String getAction() {
        return mAction;
    }

    public void putExtra(String key, Object value) {
        mMap.put(key, value);
    }

    public boolean getBooleanExtra(String key, boolean defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else {
            return defaultValue;
        }
    }

    public byte getByteExtra(String key, byte defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof Byte) {
            return (Byte) obj;
        } else {
            return defaultValue;
        }
    }

    public char getCharExtra(String key, char defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof Character) {
            return (Character) obj;
        } else {
            return defaultValue;
        }
    }

    public short getShortExtra(String key, short defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof Short) {
            return (Short) obj;
        } else {
            return defaultValue;
        }
    }

    public int getIntExtra(String key, int defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else {
            return defaultValue;
        }
    }

    public long getLongExtra(String key, long defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof Long) {
            return (Long) obj;
        } else {
            return defaultValue;
        }
    }

    public float getFloatExtra(String key, float defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof Float) {
            return (Float) obj;
        } else {
            return defaultValue;
        }
    }

    public double getDoubleExtra(String key, double defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof Double) {
            return (Double) obj;
        } else {
            return defaultValue;
        }
    }

    public String getStringExtra(String key, String defaultValue) {
        Object obj = mMap.get(key);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return defaultValue;
        }
    }

    public Object getExtra(String key) {
        return mMap.get(key);
    }

    public void removeExtra(String key) {
        mMap.remove(key);
    }
}
