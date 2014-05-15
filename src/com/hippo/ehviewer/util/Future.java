/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.hippo.ehviewer.util;

// This Future differs from the java.util.concurrent.Future in these aspects:
//
// - Once cancel() is called, isCancelled() always returns true. It is a sticky
//   flag used to communicate to the implementation. The implmentation may
//   ignore that flag. Regardless whether the Future is cancelled, a return
//   value will be provided to get(). The implementation may choose to return
//   null if it finds the Future is cancelled.
//
// - get() does not throw exceptions.
//
public interface Future<T> {
    public void cancel();
    public boolean isCancelled();
    public boolean isDone();
    public T get();
    public void waitDone();
}
