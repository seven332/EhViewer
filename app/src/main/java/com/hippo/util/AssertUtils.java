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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.util;

public class AssertUtils {

    public static void assertNull(String message, Object object) {
        if (object != null) {
            throw new AssertError(message);
        }
    }

    public static void assertNotNull(String message, Object object) {
        if (object == null) {
            throw new AssertError(message);
        }
    }

    public static void assertEquals(String message, int expected, int actual) {
        if (expected != actual) {
            throw new AssertError(message);
        }
    }

    public static void assertInstanceof(String message, Object obj, Class clazz) {
        if (!clazz.isInstance(obj)) {
            throw new AssertError(message);
        }
    }

    public static void assertNullEx(String message, Object object)
            throws AssertException {
        if (object != null) {
            throw new AssertException(message);
        }
    }

    public static void assertNotNullEx(String message, Object object)
            throws AssertException {
        if (object == null) {
            throw new AssertException(message);
        }
    }

    public static void assertEqualsEx(String message, int expected, int actual)
            throws AssertException {
        if (expected != actual) {
            throw new AssertException(message);
        }
    }

    public static void assertInstanceofEx(String message, Object obj, Class clazz)
            throws AssertException {
        if (!clazz.isInstance(obj)) {
            throw new AssertException(message);
        }
    }
}
