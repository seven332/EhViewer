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

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 1/15/2017.
 */

import com.hippo.ehviewer.client.converter.FavouritesConverter;
import com.hippo.ehviewer.client.converter.ForumsConverter;
import com.hippo.ehviewer.client.converter.GalleryDetailConverter;
import com.hippo.ehviewer.client.converter.GalleryListConverter;
import com.hippo.ehviewer.client.converter.GalleryMetadataConverter;
import com.hippo.ehviewer.client.converter.ProfileConverter;
import com.hippo.ehviewer.client.converter.SignInConverter;
import com.hippo.ehviewer.client.converter.VoidConverter;
import com.hippo.ehviewer.client.converter.WhatsHotConverter;
import com.hippo.ehviewer.client.param.GalleryMetadataParam;
import com.hippo.ehviewer.client.result.FavouritesResult;
import com.hippo.ehviewer.client.result.ForumsResult;
import com.hippo.ehviewer.client.result.GalleryDetailResult;
import com.hippo.ehviewer.client.result.GalleryListResult;
import com.hippo.ehviewer.client.result.GalleryMetadataResult;
import com.hippo.ehviewer.client.result.ProfileResult;
import com.hippo.ehviewer.client.result.SignInResult;
import com.hippo.ehviewer.client.result.VoidResult;
import com.hippo.ehviewer.client.result.WhatsHotResult;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@code Converter.Factory} to create {@link EhConverter}.
 */
public final class EhConverterFactory extends Converter.Factory {

  private EhConverterFactory() {}

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    if (SignInResult.class.equals(type)) return SignInConverter.INSTANCE;
    else if (VoidResult.class.equals(type)) return VoidConverter.INSTANCE;
    else if (GalleryListResult.class.equals(type)) return GalleryListConverter.INSTANCE;
    else if (ForumsResult.class.equals(type)) return ForumsConverter.INSTANCE;
    else if (ProfileResult.class.equals(type)) return ProfileConverter.INSTANCE;
    else if (WhatsHotResult.class.equals(type)) return WhatsHotConverter.INSTANCE;
    else if (GalleryMetadataResult.class.equals(type)) return GalleryMetadataConverter.INSTANCE;
    else if (FavouritesResult.class.equals(type)) return FavouritesConverter.INSTANCE;
    else if (GalleryDetailResult.class.equals(type)) return GalleryDetailConverter.INSTANCE;
    else throw new IllegalStateException("Unknown type: " + type);
  }

  @Override
  public Converter<?, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    if (GalleryMetadataParam.class.equals(type)) return new GalleryMetadataParam.Converter();
    else return null;
  }

  /**
   * Create a {@code EhConverterFactory}.
   */
  public static EhConverterFactory create() {
    return new EhConverterFactory();
  }
}
