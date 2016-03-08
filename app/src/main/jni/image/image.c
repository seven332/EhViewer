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

//
// Created by Hippo on 12/27/2015.
//

#include "image.h"
#include "image_jpeg.h"
#include "image_png.h"
#include "image_gif.h"
#include "java_wrapper.h"
#include "../log.h"

static int get_format(JNIEnv* env, InputStream* stream)
{
  unsigned char temp[2];
  int read = read_input_stream(env, stream, temp, 0, 2);

  if (read == 2) {
#ifdef IMAGE_SUPPORT_JPEG
    if (temp[0] == IMAGE_JPEG_MAGIC_NUMBER_0 && temp[1] == IMAGE_JPEG_MAGIC_NUMBER_1) {
      return IMAGE_FORMAT_JPEG;
    }
#endif
#ifdef IMAGE_SUPPORT_PNG
    if (temp[0] == IMAGE_PNG_MAGIC_NUMBER_0 && temp[1] == IMAGE_PNG_MAGIC_NUMBER_1) {
      return IMAGE_FORMAT_PNG;
    }
#endif
#ifdef IMAGE_SUPPORT_GIF
    if (temp[0] == IMAGE_GIF_MAGIC_NUMBER_0 && temp[1] == IMAGE_GIF_MAGIC_NUMBER_1) {
      return IMAGE_FORMAT_GIF;
    }
#endif
  }

  return IMAGE_FORMAT_UNKNOWN;
}

void* decode(JNIEnv* env, InputStream* stream, bool partially, int* format)
{
  unsigned char magic_numbers[2];
  PatchHeadInputStream* patch_head_input_stream;

  *format = get_format(env, stream);

  switch (*format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      magic_numbers[0] = IMAGE_JPEG_MAGIC_NUMBER_0;
      magic_numbers[1] = IMAGE_JPEG_MAGIC_NUMBER_1;
      break;
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      magic_numbers[0] = IMAGE_PNG_MAGIC_NUMBER_0;
      magic_numbers[1] = IMAGE_PNG_MAGIC_NUMBER_1;
      break;
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      magic_numbers[0] = IMAGE_GIF_MAGIC_NUMBER_0;
      magic_numbers[1] = IMAGE_GIF_MAGIC_NUMBER_1;
      break;
#endif
    default:
      LOGE(MSG("Can't detect format %d"), *format);
      destroy_input_stream(get_env(), &stream);
      return NULL;
  }

  patch_head_input_stream = create_patch_head_input_stream(stream, magic_numbers, 2);
  if (patch_head_input_stream == NULL){
    WTF_OM;
    destroy_input_stream(get_env(), &stream);
    return NULL;
  }

  switch (*format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      return JPEG_decode(env, patch_head_input_stream, partially);
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      return PNG_decode(env, patch_head_input_stream, partially);
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      return GIF_decode(env, patch_head_input_stream, partially);
#endif
    default:
      LOGE(MSG("Can't detect format %d"), *format);
      close_patch_head_input_stream(get_env(), patch_head_input_stream);
      destroy_patch_head_input_stream(get_env(), &patch_head_input_stream);
      return NULL;
  }
}

bool complete(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      return JPEG_complete((JPEG*) image);
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      return PNG_complete((PNG*) image);
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      return GIF_complete((GIF*) image);
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
      return false;
  }
}

bool is_completed(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      return JPEG_is_completed((JPEG*) image);
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      return PNG_is_completed((PNG*) image);
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      return GIF_is_completed((GIF*) image);
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
      return false;
  }
}

int get_width(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      return JPEG_get_width((JPEG*) image);
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      return PNG_get_width((PNG*) image);
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      return GIF_get_width((GIF*) image);
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
      return -1;
  }
}

int get_height(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      return JPEG_get_height((JPEG*) image);
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      return PNG_get_height((PNG*) image);
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      return GIF_get_height((GIF*) image);
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
      return -1;
  }
}

void render(void* image, int format, int src_x, int src_y,
    void* dst, int dst_w, int dst_h, int dst_x, int dst_y,
    int width, int height, bool fill_blank, int default_color)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      JPEG_render((JPEG*) image, src_x, src_y,
          dst, dst_w, dst_h, dst_x, dst_y,
          width, height, fill_blank, default_color);
      break;
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      PNG_render((PNG*) image, src_x, src_y,
          dst, dst_w, dst_h, dst_x, dst_y,
          width, height, fill_blank, default_color);
      break;
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      GIF_render((GIF*) image, src_x, src_y,
          dst, dst_w, dst_h, dst_x, dst_y,
          width, height, fill_blank, default_color);
      break;
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
      break;
  }
}

void advance(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      JPEG_advance((JPEG*) image);
      break;
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      PNG_advance((PNG*) image);
      break;
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      GIF_advance((GIF*) image);
      break;
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
  }
}

int get_delay(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      return JPEG_get_delay((JPEG*) image);
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      return PNG_get_delay((PNG*) image);
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      return GIF_get_delay((GIF*) image);
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
      return false;
  }
}

int get_frame_count(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      return JPEG_get_frame_count((JPEG*) image);
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      return PNG_get_frame_count((PNG*) image);
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      return GIF_get_frame_count((GIF*) image);
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
      return false;
  }
}

bool is_opaque(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      return JPEG_is_opaque((JPEG*) image);
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      return PNG_is_opaque((PNG*) image);
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      return GIF_is_opaque((GIF*) image);
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
      return false;
  }
}

void recycle(void* image, int format)
{
  switch (format) {
#ifdef IMAGE_SUPPORT_JPEG
    case IMAGE_FORMAT_JPEG:
      JPEG_recycle((JPEG*) image);
      break;
#endif
#ifdef IMAGE_SUPPORT_PNG
    case IMAGE_FORMAT_PNG:
      PNG_recycle((PNG*) image);
      break;
#endif
#ifdef IMAGE_SUPPORT_GIF
    case IMAGE_FORMAT_GIF:
      GIF_recycle((GIF*) image);
      break;
#endif
    default:
      LOGE(MSG("Can't detect format %d"), format);
  }
}
