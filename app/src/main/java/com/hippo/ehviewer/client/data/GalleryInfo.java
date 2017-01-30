/*
 * Copyright 2017 Hippo Seven
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

package com.hippo.ehviewer.client.data;

/*
 * Created by Hippo on 1/29/2017.
 */

import com.hippo.ehviewer.client.EhUtils;

/**
 * Gallery Information.
 * <p>
 * There three ways to obtain a {@code GalleryInfo}.
 * <ul>
 * <li>Parsing gallery list html</li>
 * <li>Parsing gallery detail html</li>
 * <li>Gallery Metadata API</li>
 * </ul>
 * None of these methods can fill all fields.
 */
public class GalleryInfo {
  /**
   * Gallery ID.
   * <p>
   * {@code int} should be enough for a long long time.
   * But I like {@code long}.
   */
  public long gid;
  /**
   * Gallery token. Most gallery operations need it.
   * <p>
   * NonNull, must be valid.
   * <p>
   * Regex:<pre>{@code
   * [0-9a-f]{10}
   * }</pre>
   * Example:<pre>{@code
   * c219d2cf41
   * }</pre>
   */
  public String token;
  /**
   * Gallery title.
   * <p>
   * May be {@code null} if user enable show jp title.
   * <p>
   * One of {@code title} and {@code titleJpn} must be non-null.
   */
  public String title;
  /**
   * Gallery title.
   * <p>
   * {@code null} if can't get it.
   * <p>
   * One of {@code title} and {@code titleJpn} must be non-null.
   */
  public String titleJpn;
  /**
   * The fingerprint of the first image.
   * <p>
   * Format:
   * {@code [sha1]-[size]-[width]-[height]-[format]}
   * <p>
   * Regex:
   * {@code [0-9a-f]{40}-\d+-\d+-\d+-[0-9a-z]+}
   * <p>
   * Example:
   * {@code 7dd3e4a62807a6938910a14407d9867b18a58a9f-2333088-2831-4015-jpg}
   * <p>
   * {@code null} if can't get it.
   */
  public String cover;
  /**
   * The url of the cover.
   * <p>
   * {@code null} if can't get it.
   */
  public String coverUrl;
  /**
   * Cover width / Cover height.
   * <p>
   * {@link Float#NaN} if can't it.
   */
  public float coverRatio = Float.NaN;
  /**
   * Gallery category.
   * <p>
   * {@link com.hippo.ehviewer.client.EhUtils#UNKNOWN} if can't get it.
   */
  public int category = EhUtils.UNKNOWN;
  /**
   * Posted time stamp.
   * <p>
   * {@code 0} if can't get it.
   */
  public long date;
  /**
   * Who uploads the gallery.
   * <p>
   * {@code null} if can't get it.
   */
  public String uploader;
  /**
   * Gallery Rating.
   * <p>
   * Range: {@code [0.5, 5]}
   * <p>
   * {@link Float#NaN} if can't it, or if no rating temporarily.
   */
  public float rating = Float.NaN;

  // TODO Add more fields.
}
