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

public interface GalleryProvider {

    /**
     * There is no worker to get the page now.
     * There is no guarantee that the {@link GalleryProviderListener} will be
     * called, so do some reactions after get it.
     */
    Object RESULT_NONE = new Object();

    /**
     * Worker failed to get the page
     * There is no guarantee that the {@link GalleryProviderListener} will be
     * called, so do some reactions after get it.
     */
    Object RESULT_FAILED = new Object();

    /**
     * {@link GalleryProviderListener} will be calls soon.
     */
    Object RESULT_WAIT = new Object();

    /**
     * Request is out of range
     */
    Object RESULT_OUT_OF_RANGE = new Object();

    /**
     * Request special index page
     *
     * @param index the index to request
     * @return Might be on of {@link #RESULT_NONE}, {@link #RESULT_FAILED},
     * {@link #RESULT_WAIT}, {@link #RESULT_OUT_OF_RANGE}. It could be
     * {@link Float} it means the percent of getting the page
     */
    Object request(int index);

    /**
     * Just like {@link #request(int)}, but it ignores cache or local file
     *
     * @param index the index to request
     * @return just like {@link #request(int)}
     */
    Object forceRequest(int index);

    /**
     * Gallery provider may fails, so we need
     */
    void restart();

    void retry();

    void addGalleryProviderListener(GalleryProviderListener listener);

    void removeGalleryProviderListener(GalleryProviderListener listener);
}
